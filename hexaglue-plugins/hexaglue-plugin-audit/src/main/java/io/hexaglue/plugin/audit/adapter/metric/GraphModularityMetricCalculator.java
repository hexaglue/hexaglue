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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the Newman-Girvan graph modularity Q for directed graphs.
 *
 * <p>Modularity Q measures how well the dependency graph is partitioned into
 * packages. It compares the actual number of intra-package edges to the expected
 * number in a random graph with the same degree distribution.
 *
 * <p>For directed graphs:
 * <pre>
 * Q = (1/m) × Σ [A_ij - (k_out_i × k_in_j) / m]  for (i,j) in same package
 * </pre>
 * where m is total edges, k_out_i is i's fan-out, and k_in_j is j's fan-in.
 *
 * <p><strong>Metric:</strong> architecture.modularity.q<br>
 * <strong>Unit:</strong> Q<br>
 * <strong>Threshold:</strong> Warning if Q &lt; 0.3<br>
 * <strong>Interpretation:</strong> Higher is better. Q &gt; 0.3 indicates meaningful
 * community structure (well-defined packages). Q near 0 means packages are no better
 * than random. Negative Q means packages actively group dissimilar types.
 *
 * @see <a href="https://doi.org/10.1103/PhysRevE.69.026113">Newman (2004)</a>
 * @since 5.1.0
 */
public class GraphModularityMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.modularity.q";
    private static final double WARNING_THRESHOLD = 0.3;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates graph modularity Q for the type dependency graph.
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
            return Metric.of(METRIC_NAME, 0.0, "Q", "Graph modularity (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        if (dependencies.isEmpty()) {
            return Metric.of(METRIC_NAME, 0.0, "Q", "Graph modularity (no dependencies found)");
        }

        // Collect all types
        Set<String> allTypes = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTypes::addAll);

        // Count total edges (m)
        long totalEdges = 0;
        for (Set<String> targets : dependencies.values()) {
            totalEdges += targets.size();
        }

        if (totalEdges == 0) {
            return Metric.of(METRIC_NAME, 0.0, "Q", "Graph modularity (no edges in graph)");
        }

        // Compute fan-out (k_out) and fan-in (k_in) for each node
        Map<String, Integer> fanOut = new HashMap<>();
        Map<String, Integer> fanIn = new HashMap<>();
        for (String type : allTypes) {
            fanOut.put(type, 0);
            fanIn.put(type, 0);
        }
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String source = entry.getKey();
            Set<String> targets = entry.getValue();
            fanOut.put(source, targets.size());
            for (String target : targets) {
                fanIn.merge(target, 1, Integer::sum);
            }
        }

        // Assign packages
        Map<String, String> typeToPackage = new HashMap<>();
        for (String type : allTypes) {
            typeToPackage.put(type, extractPackage(type));
        }

        // Calculate Q
        double m = totalEdges;
        double qSum = 0.0;

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String source = entry.getKey();
            String sourcePackage = typeToPackage.get(source);
            int kOutI = fanOut.get(source);

            for (String target : entry.getValue()) {
                String targetPackage = typeToPackage.get(target);
                if (sourcePackage.equals(targetPackage)) {
                    int kInJ = fanIn.get(target);
                    // A_ij = 1 (edge exists), contribution = 1 - (k_out_i * k_in_j) / m
                    qSum += 1.0 - ((double) kOutI * kInJ) / m;
                }
            }
        }

        double q = qSum / m;

        return Metric.of(
                METRIC_NAME,
                q,
                "Q",
                String.format("Graph modularity: Q=%.3f (%d edges, %d types)", q, totalEdges, allTypes.size()),
                MetricThreshold.lessThan(WARNING_THRESHOLD));
    }

    /**
     * Extracts the package name from a fully qualified type name.
     */
    private String extractPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
