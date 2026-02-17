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
import io.hexaglue.plugin.audit.adapter.validator.util.CycleDetector;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates relative cyclicity as defined by Sonargraph.
 *
 * <p>Relative cyclicity measures the percentage of elements involved in dependency
 * cycles, weighted quadratically by SCC (Strongly Connected Component) size. This
 * means a single SCC of 10 types (weight 100) is considered 25x worse than an SCC
 * of 2 types (weight 4), reflecting the exponential difficulty of breaking large cycles.
 *
 * <p>Uses Tarjan's algorithm via {@link CycleDetector#findStronglyConnectedComponents}
 * to compute true SCCs, which correctly merges overlapping cycles into single components.
 *
 * <p><strong>Formula:</strong> {@code Σ(scc_size²) / N² × 100}<br>
 * <strong>Metric:</strong> architecture.cyclicity.relative<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if &gt; 5%<br>
 * <strong>Interpretation:</strong> Lower is better, 0% is ideal. Values above 15%
 * indicate critical structural problems.
 *
 * @since 5.1.0
 */
public class RelativeCyclicityMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.cyclicity.relative";
    private static final double WARNING_THRESHOLD = 5.0;

    private final CycleDetector cycleDetector;

    /** Creates a calculator with a default {@link CycleDetector}. */
    public RelativeCyclicityMetricCalculator() {
        this.cycleDetector = new CycleDetector();
    }

    /** Creates a calculator with the given {@link CycleDetector} (for testing). */
    RelativeCyclicityMetricCalculator(CycleDetector cycleDetector) {
        this.cycleDetector = cycleDetector;
    }

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates relative cyclicity using SCCs from the dependency graph.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Build the full type set from {@code allTypeDependencies()}</li>
     *   <li>Find SCCs of size &ge; 2 via Tarjan's algorithm</li>
     *   <li>Compute {@code Σ(scc_size²) / N² × 100}</li>
     * </ol>
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
            return Metric.of(METRIC_NAME, 0.0, "%", "Relative cyclicity (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        if (dependencies.isEmpty()) {
            return Metric.of(METRIC_NAME, 0.0, "%", "Relative cyclicity (no dependencies found)");
        }

        // Collect all types (sources and targets)
        Set<String> allTypes = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTypes::addAll);

        long n = allTypes.size();
        if (n <= 1) {
            return Metric.of(METRIC_NAME, 0.0, "%", "Relative cyclicity (insufficient types for analysis)");
        }

        // Find SCCs of size >= 2 using Tarjan's algorithm
        List<Set<String>> sccs = cycleDetector.findStronglyConnectedComponents(allTypes, dependencies);

        if (sccs.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    0.0,
                    "%",
                    "Relative cyclicity: no dependency cycles detected",
                    MetricThreshold.greaterThan(WARNING_THRESHOLD));
        }

        // Compute Σ(scc_size²) and count total cyclic types
        long sumSquaredSizes = 0;
        long totalCyclicTypes = 0;
        for (Set<String> scc : sccs) {
            int size = scc.size();
            sumSquaredSizes += (long) size * size;
            totalCyclicTypes += size;
        }

        double relativeCyclicity = (double) sumSquaredSizes / (n * n) * 100.0;

        return Metric.of(
                METRIC_NAME,
                relativeCyclicity,
                "%",
                String.format(
                        "Relative cyclicity: %d SCC(s) involving %d types out of %d total",
                        sccs.size(), totalCyclicTypes, n),
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }
}
