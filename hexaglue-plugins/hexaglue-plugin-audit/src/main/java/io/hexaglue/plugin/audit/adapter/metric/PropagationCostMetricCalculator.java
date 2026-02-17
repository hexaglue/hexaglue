/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Calculates propagation cost as defined by MacCormack, Rusnak, and Baldwin (2006).
 *
 * <p>Propagation cost measures the proportion of the system that is affected (directly
 * or indirectly) by a change to any single element. It is computed as the ratio of
 * dependencies in the transitive closure to the total possible dependencies (N²).
 *
 * <p><strong>Metric:</strong> architecture.propagation.cost<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if &gt; 35%<br>
 * <strong>Interpretation:</strong> Lower is better. A propagation cost of 43% means
 * any random change affects, on average, 43% of the system. Well-modular hexagonal
 * architectures should have low propagation cost because domain changes do not
 * propagate to adapters (they pass through ports) and vice versa.
 *
 * @see <a href="https://www.hbs.edu/ris/Publication%20Files/05-016.pdf">MacCormack et al. (2006)</a>
 * @since 5.1.0
 */
public class PropagationCostMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.propagation.cost";
    private static final double WARNING_THRESHOLD = 35.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates propagation cost using the dependency graph from ArchitectureQuery.
     *
     * <p>Formula: {@code PC = transitive_dependency_count / N² * 100}
     * where each type counts itself as a dependency (reflexive).
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.1.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        if (architectureQuery == null) {
            return Metric.of(METRIC_NAME, 0.0, "%", "Propagation cost (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        if (dependencies.isEmpty()) {
            return Metric.of(METRIC_NAME, 0.0, "%", "Propagation cost (no dependencies found)");
        }

        // Collect all types (sources and targets)
        Set<String> allTypes = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTypes::addAll);

        int n = allTypes.size();
        if (n <= 1) {
            return Metric.of(METRIC_NAME, 0.0, "%", "Propagation cost (single type, no propagation possible)");
        }

        // Count total transitive dependencies (including self)
        long totalTransitiveDeps = 0;
        for (String type : allTypes) {
            totalTransitiveDeps += computeReachableCount(type, dependencies);
        }

        double propagationCost = (double) totalTransitiveDeps / ((long) n * n) * 100.0;

        return Metric.of(
                METRIC_NAME,
                propagationCost,
                "%",
                String.format(
                        "Propagation cost: %.1f%% (%d transitive deps across %d types)",
                        propagationCost, totalTransitiveDeps, n),
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }

    /**
     * Computes the number of types reachable from the given type (including itself)
     * via BFS on the dependency graph.
     */
    private int computeReachableCount(String type, Map<String, Set<String>> dependencies) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(type);
        visited.add(type);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> deps = dependencies.getOrDefault(current, Set.of());
            for (String dep : deps) {
                if (visited.add(dep)) {
                    queue.add(dep);
                }
            }
        }

        return visited.size();
    }
}
