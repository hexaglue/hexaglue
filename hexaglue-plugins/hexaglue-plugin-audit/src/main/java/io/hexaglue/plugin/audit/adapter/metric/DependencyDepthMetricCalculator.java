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
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Calculates the longest dependency chain (depth) in the dependency graph.
 *
 * <p>Dependency depth represents how many levels of transitive dependencies exist.
 * In a well-structured hexagonal architecture, this should be 3-5 levels
 * (adapter &rarr; application service &rarr; domain &rarr; value object). A depth
 * significantly exceeding this suggests unnecessary intermediary layers or confused layering.
 *
 * <p>Cycles are handled by contracting strongly connected components (SCCs) into
 * single nodes before computing the longest path in the resulting DAG.
 *
 * <p><strong>Metric:</strong> architecture.dependency.depth<br>
 * <strong>Unit:</strong> levels<br>
 * <strong>Threshold:</strong> Warning if &gt; 7<br>
 * <strong>Interpretation:</strong> Lower is generally better. 3-5 is typical for
 * hexagonal architecture. Values above 7 suggest excessive layering.
 *
 * @since 5.1.0
 */
public class DependencyDepthMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.dependency.depth";
    private static final double WARNING_THRESHOLD = 7.0;

    private final CycleDetector cycleDetector;

    /** Creates a calculator with a default {@link CycleDetector}. */
    public DependencyDepthMetricCalculator() {
        this.cycleDetector = new CycleDetector();
    }

    /** Creates a calculator with the given {@link CycleDetector} (for testing). */
    DependencyDepthMetricCalculator(CycleDetector cycleDetector) {
        this.cycleDetector = cycleDetector;
    }

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the dependency depth using the dependency graph from ArchitectureQuery.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Build the dependency graph from {@code allTypeDependencies()}</li>
     *   <li>Detect SCCs (cycles) and contract them into super-nodes</li>
     *   <li>Compute the longest path in the resulting DAG via BFS from source nodes</li>
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
            return Metric.of(METRIC_NAME, 0.0, "levels", "Dependency depth (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        if (dependencies.isEmpty()) {
            return Metric.of(METRIC_NAME, 0.0, "levels", "Dependency depth (no dependencies found)");
        }

        // Collect all types
        Set<String> allTypes = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTypes::addAll);

        if (allTypes.size() <= 1) {
            return Metric.of(METRIC_NAME, 0.0, "levels", "Dependency depth (single type)");
        }

        // Contract SCCs into super-nodes and compute longest path on the DAG
        int depth = computeLongestPath(allTypes, dependencies);

        return Metric.of(
                METRIC_NAME,
                depth,
                "levels",
                String.format("Dependency depth: %d levels across %d types", depth, allTypes.size()),
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }

    /**
     * Computes the longest path in the dependency graph after SCC contraction.
     *
     * <p>Delegates SCC detection to {@link CycleDetector}, then computes BFS-based
     * longest path on the condensed DAG.
     */
    private int computeLongestPath(Set<String> allTypes, Map<String, Set<String>> dependencies) {
        // Step 1: Assign each type to an SCC (represented by a canonical member)
        Map<String, String> sccMap = cycleDetector.computeSccMapping(allTypes, dependencies);

        // Step 2: Build condensed DAG (edges between SCCs)
        Map<String, Set<String>> condensedDag = new HashMap<>();
        for (String type : allTypes) {
            String sourceScc = sccMap.get(type);
            condensedDag.putIfAbsent(sourceScc, new HashSet<>());
            for (String dep : dependencies.getOrDefault(type, Set.of())) {
                String targetScc = sccMap.get(dep);
                if (targetScc != null && !sourceScc.equals(targetScc)) {
                    condensedDag
                            .computeIfAbsent(sourceScc, k -> new HashSet<>())
                            .add(targetScc);
                }
            }
        }

        // Step 3: Find all SCC nodes
        Set<String> allSccNodes = new HashSet<>(condensedDag.keySet());
        condensedDag.values().forEach(allSccNodes::addAll);

        // Step 4: Compute in-degrees for source detection
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : allSccNodes) {
            inDegree.put(node, 0);
        }
        for (Set<String> targets : condensedDag.values()) {
            for (String target : targets) {
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        // Step 5: BFS from source nodes (in-degree = 0) computing longest path
        Map<String, Integer> dist = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
                dist.put(entry.getKey(), 0);
            }
        }

        int maxDepth = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDist = dist.getOrDefault(current, 0);
            for (String neighbor : condensedDag.getOrDefault(current, Set.of())) {
                int newDist = currentDist + 1;
                if (newDist > dist.getOrDefault(neighbor, 0)) {
                    dist.put(neighbor, newDist);
                    maxDepth = Math.max(maxDepth, newDist);
                }
                // Add to queue if all predecessors processed (topological relaxation)
                int remaining = inDegree.merge(neighbor, -1, Integer::sum);
                if (remaining == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return maxDepth;
    }
}
