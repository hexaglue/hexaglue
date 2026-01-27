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

import io.hexaglue.arch.model.audit.CouplingMetrics;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.*;

/**
 * Calculator for package-level coupling metrics based on Robert C. Martin's principles.
 *
 * <p>This calculator computes coupling metrics for packages to assess their
 * stability and adherence to the Main Sequence (balance between abstractness
 * and stability).
 *
 * <p>Metrics calculated:
 * <ul>
 *   <li><b>Ca (Afferent Coupling)</b>: Incoming dependencies</li>
 *   <li><b>Ce (Efferent Coupling)</b>: Outgoing dependencies</li>
 *   <li><b>I (Instability)</b>: Ce / (Ca + Ce)</li>
 *   <li><b>A (Abstractness)</b>: Abstract types / Total types</li>
 *   <li><b>D (Distance from Main Sequence)</b>: |A + I - 1|</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class CouplingMetricsCalculator {

    /**
     * Calculates coupling metrics for all packages in the graph.
     *
     * @param graph the application graph
     * @return map of package name to coupling metrics
     */
    public Map<String, CouplingMetrics> calculateForAllPackages(ApplicationGraph graph) {
        if (graph == null) {
            return Map.of();
        }

        // Group types by package
        Map<String, List<TypeNode>> packageToTypes = groupTypesByPackage(graph);

        Map<String, CouplingMetrics> result = new HashMap<>();

        for (Map.Entry<String, List<TypeNode>> entry : packageToTypes.entrySet()) {
            String packageName = entry.getKey();
            List<TypeNode> typesInPackage = entry.getValue();

            CouplingMetrics metrics = calculateForPackage(packageName, typesInPackage, graph);
            result.put(packageName, metrics);
        }

        return result;
    }

    /**
     * Calculates coupling metrics for a specific package.
     *
     * @param packageName    the package name
     * @param typesInPackage the types in this package
     * @param graph          the application graph
     * @return coupling metrics for the package
     */
    public CouplingMetrics calculateForPackage(
            String packageName, List<TypeNode> typesInPackage, ApplicationGraph graph) {
        int ca = calculateAfferentCoupling(typesInPackage, graph);
        int ce = calculateEfferentCoupling(typesInPackage, graph);
        double abstractness = calculateAbstractness(typesInPackage);

        return new CouplingMetrics(packageName, ca, ce, abstractness);
    }

    /**
     * Groups types by their package name.
     */
    private Map<String, List<TypeNode>> groupTypesByPackage(ApplicationGraph graph) {
        Map<String, List<TypeNode>> result = new HashMap<>();

        for (TypeNode typeNode : graph.typeNodes()) {
            String packageName = typeNode.packageName();
            result.computeIfAbsent(packageName, k -> new ArrayList<>()).add(typeNode);
        }

        return result;
    }

    /**
     * Calculates afferent coupling (Ca) - incoming dependencies.
     *
     * <p>Ca is the number of types outside this package that depend on types
     * inside this package.
     */
    private int calculateAfferentCoupling(List<TypeNode> typesInPackage, ApplicationGraph graph) {
        Set<String> dependentTypes = new HashSet<>();
        Set<String> packageTypeIds =
                typesInPackage.stream().map(type -> type.id().value()).collect(java.util.stream.Collectors.toSet());

        // Find all edges pointing TO any type in this package FROM types outside this package
        for (Edge edge : graph.edges()) {
            String toId = edge.to().value();
            String fromId = edge.from().value();

            // If edge points to a type in this package from a type outside this package
            if (packageTypeIds.contains(toId) && !packageTypeIds.contains(fromId)) {
                dependentTypes.add(fromId);
            }
        }

        return dependentTypes.size();
    }

    /**
     * Calculates efferent coupling (Ce) - outgoing dependencies.
     *
     * <p>Ce is the number of types outside this package that types inside
     * this package depend on.
     */
    private int calculateEfferentCoupling(List<TypeNode> typesInPackage, ApplicationGraph graph) {
        Set<String> dependencyTypes = new HashSet<>();
        Set<String> packageTypeIds =
                typesInPackage.stream().map(type -> type.id().value()).collect(java.util.stream.Collectors.toSet());

        // Find all edges going FROM any type in this package TO types outside this package
        for (Edge edge : graph.edges()) {
            String fromId = edge.from().value();
            String toId = edge.to().value();

            // If edge goes from a type in this package to a type outside this package
            if (packageTypeIds.contains(fromId) && !packageTypeIds.contains(toId)) {
                dependencyTypes.add(toId);
            }
        }

        return dependencyTypes.size();
    }

    /**
     * Calculates abstractness (A) - ratio of abstract types to total types.
     *
     * <p>A type is considered abstract if it's an interface or an abstract class.
     */
    private double calculateAbstractness(List<TypeNode> typesInPackage) {
        if (typesInPackage.isEmpty()) {
            return 0.0;
        }

        long abstractCount = typesInPackage.stream().filter(this::isAbstract).count();

        return (double) abstractCount / typesInPackage.size();
    }

    /**
     * Determines if a type is abstract (interface or abstract class).
     */
    private boolean isAbstract(TypeNode type) {
        return type.isInterface() || type.isAbstract();
    }

    /**
     * Interprets the coupling metrics and returns a zone classification.
     *
     * @param metrics the coupling metrics
     * @return zone classification
     */
    public ZoneClassification classifyZone(CouplingMetrics metrics) {
        double instability = metrics.instability();
        double abstractness = metrics.abstractness();

        // Zone of Pain: Stable + Concrete (I < 0.3, A < 0.3)
        if (instability < 0.3 && abstractness < 0.3) {
            return ZoneClassification.ZONE_OF_PAIN;
        }

        // Zone of Uselessness: Unstable + Abstract (I > 0.7, A > 0.7)
        if (instability > 0.7 && abstractness > 0.7) {
            return ZoneClassification.ZONE_OF_USELESSNESS;
        }

        // On Main Sequence: |A + I - 1| < 0.1
        double distance = metrics.distanceFromMainSequence();
        if (distance < 0.1) {
            return ZoneClassification.MAIN_SEQUENCE;
        }

        // Off Main Sequence: |A + I - 1| >= 0.1
        if (distance < 0.3) {
            return ZoneClassification.NEAR_MAIN_SEQUENCE;
        }

        return ZoneClassification.OFF_MAIN_SEQUENCE;
    }

    /**
     * Zone classification for package coupling metrics.
     */
    public enum ZoneClassification {
        /** On the main sequence (ideal) */
        MAIN_SEQUENCE,

        /** Near the main sequence (good) */
        NEAR_MAIN_SEQUENCE,

        /** Off the main sequence (needs attention) */
        OFF_MAIN_SEQUENCE,

        /** Zone of Pain (stable but concrete - hard to change) */
        ZONE_OF_PAIN,

        /** Zone of Uselessness (unstable and abstract - not used) */
        ZONE_OF_USELESSNESS
    }
}
