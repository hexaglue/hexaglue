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

package io.hexaglue.core.audit.metrics;

import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.HashSet;
import java.util.Set;

/**
 * Calculator for Lakos architectural metrics.
 *
 * <p>This calculator computes metrics defined by John Lakos for assessing
 * large-scale architectural quality. These metrics help identify architectural
 * debt and guide refactoring efforts.
 *
 * <p><b>Note:</b> This is a simplified implementation that operates at the
 * type level. A complete implementation would operate at the package level
 * and include more sophisticated cycle handling.
 *
 * @since 3.0.0
 */
public final class LakosMetricsCalculator {

    /**
     * Calculates Lakos metrics for the given application graph.
     *
     * @param graph the application graph
     * @return Lakos metrics
     */
    public LakosMetrics calculate(ApplicationGraph graph) {
        if (graph == null) {
            return new LakosMetrics(0, 0, 0.0, 0.0, 0.0);
        }

        Set<TypeNode> types = collectTypes(graph);
        int componentCount = types.size();

        if (componentCount == 0) {
            return new LakosMetrics(0, 0, 0.0, 0.0, 0.0);
        }

        // Calculate CCD (Cumulative Component Dependency)
        int ccd = calculateCCD(types, graph);

        // Calculate ACD (Average Component Dependency)
        double acd = calculateACD(componentCount, ccd);

        // Calculate NCCD (Normalized CCD)
        double nccd = calculateNCCD(componentCount, ccd);

        // Calculate RACD (Relative ACD)
        double racd = calculateRACD(componentCount, acd);

        return new LakosMetrics(componentCount, ccd, acd, nccd, racd);
    }

    /**
     * Collects all type nodes from the graph.
     */
    private Set<TypeNode> collectTypes(ApplicationGraph graph) {
        return new HashSet<>(graph.typeNodes());
    }

    /**
     * Calculates CCD (Cumulative Component Dependency).
     *
     * <p>CCD is the sum of the number of transitive dependencies for each component.
     * This provides a measure of overall system coupling.
     */
    private int calculateCCD(Set<TypeNode> types, ApplicationGraph graph) {
        int totalDependencies = 0;

        for (TypeNode type : types) {
            totalDependencies += calculateDependsOn(type, graph);
        }

        return totalDependencies;
    }

    /**
     * Calculates the number of transitive dependencies for a given type.
     *
     * <p>This is a recursive calculation that follows all outgoing edges.
     */
    private int calculateDependsOn(TypeNode type, ApplicationGraph graph) {
        Set<NodeId> visited = new HashSet<>();
        collectTransitiveDependencies(type.id(), graph, visited);
        // Subtract 1 to exclude the type itself
        return Math.max(0, visited.size() - 1);
    }

    /**
     * Recursively collects all transitive dependencies.
     */
    private void collectTransitiveDependencies(NodeId nodeId, ApplicationGraph graph, Set<NodeId> visited) {
        if (!visited.add(nodeId)) {
            return; // Already visited (cycle or duplicate)
        }

        // Find all outgoing edges from this node
        for (Edge edge : graph.edgesFrom(nodeId)) {
            collectTransitiveDependencies(edge.to(), graph, visited);
        }
    }

    /**
     * Calculates ACD (Average Component Dependency).
     *
     * <p>ACD = CCD / number of components
     */
    private double calculateACD(int componentCount, int ccd) {
        return componentCount > 0 ? (double) ccd / componentCount : 0.0;
    }

    /**
     * Calculates NCCD (Normalized CCD).
     *
     * <p>NCCD compares the actual CCD to the CCD of a balanced binary tree
     * with the same number of nodes. A balanced tree represents an ideal
     * dependency structure.
     *
     * <p>For a balanced binary tree with n nodes:
     * CCD_ideal ≈ n * log₂(n)
     *
     * <p>NCCD = CCD_actual / CCD_ideal
     */
    private double calculateNCCD(int componentCount, int ccd) {
        if (componentCount <= 1) {
            return 0.0;
        }

        // Ideal CCD for a balanced binary tree
        double idealCCD = componentCount * (Math.log(componentCount) / Math.log(2));

        return idealCCD > 0 ? ccd / idealCCD : 0.0;
    }

    /**
     * Calculates RACD (Relative ACD).
     *
     * <p>RACD compares the actual ACD to the theoretical minimum ACD.
     * The minimum occurs when dependencies form a balanced tree.
     *
     * <p>Minimum ACD ≈ log₂(n)
     *
     * <p>RACD = ACD_actual / ACD_min
     */
    private double calculateRACD(int componentCount, double acd) {
        if (componentCount <= 1) {
            return 0.0;
        }

        // Theoretical minimum ACD (balanced tree)
        double minACD = Math.log(componentCount) / Math.log(2);

        return minACD > 0 ? acd / minACD : 0.0;
    }
}
