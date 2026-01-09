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

package io.hexaglue.core.classification.anomaly;

import io.hexaglue.core.classification.deterministic.Classification;
import io.hexaglue.core.graph.algorithm.Cycle;
import io.hexaglue.core.graph.algorithm.CycleDetectionConfig;
import io.hexaglue.core.graph.algorithm.TarjanCycleDetector;
import io.hexaglue.core.graph.composition.CompositionEdge;
import io.hexaglue.core.graph.composition.CompositionGraph;
import io.hexaglue.spi.ir.DomainKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detector for architectural anomalies in domain models.
 *
 * <p>The anomaly detector analyzes the composition graph and classifications to find:
 * <ul>
 *   <li>Direct references between aggregate roots (design smell)</li>
 *   <li>Composition cycles (structural issue)</li>
 *   <li>Shared entities across aggregates (boundary violation)</li>
 *   <li>Aggregate roots without repositories (incomplete implementation)</li>
 *   <li>Value objects with identity fields (classification issue)</li>
 * </ul>
 *
 * <p>Detected anomalies are reported with severity levels to help prioritize fixes.
 *
 * @since 3.0.0
 */
public final class AnomalyDetector {

    /**
     * Detects all anomalies in the given graph and classifications.
     *
     * @param graph           the composition graph
     * @param classifications the domain classifications
     * @return list of detected anomalies
     */
    public List<Anomaly> detect(CompositionGraph graph, Map<String, Classification> classifications) {
        Objects.requireNonNull(graph, "graph required");
        Objects.requireNonNull(classifications, "classifications required");

        List<Anomaly> anomalies = new ArrayList<>();

        // Detect each anomaly type
        anomalies.addAll(detectDirectAggregateReferences(graph, classifications));
        anomalies.addAll(detectCompositionCycles(graph));
        anomalies.addAll(detectSharedEntities(graph, classifications));
        anomalies.addAll(detectAggregatesWithoutRepositories(graph, classifications));
        anomalies.addAll(detectValueObjectsWithIdentity(graph, classifications));

        return anomalies;
    }

    /**
     * Detects direct references between aggregate roots.
     *
     * @param graph           the composition graph
     * @param classifications the classifications
     * @return list of anomalies
     */
    private List<Anomaly> detectDirectAggregateReferences(
            CompositionGraph graph, Map<String, Classification> classifications) {

        List<Anomaly> anomalies = new ArrayList<>();
        Set<String> aggregateRoots = findAggregateRoots(classifications);

        for (CompositionEdge edge : graph.getDirectReferenceEdges()) {
            String source = edge.source();
            String target = edge.target();

            // Check if both are aggregate roots
            if (aggregateRoots.contains(source) && aggregateRoots.contains(target)) {
                String message = String.format(
                        "Aggregate root '%s' directly references aggregate root '%s' via field '%s'. "
                                + "Use ID reference instead for proper aggregate isolation.",
                        simplifyTypeName(source), simplifyTypeName(target), edge.fieldName());

                anomalies.add(
                        Anomaly.warning(AnomalyType.DIRECT_AGGREGATE_REFERENCE, source, message, List.of(target)));
            }
        }

        return anomalies;
    }

    /**
     * Detects cycles in the composition graph.
     *
     * @param graph the composition graph
     * @return list of anomalies
     */
    private List<Anomaly> detectCompositionCycles(CompositionGraph graph) {
        List<Anomaly> anomalies = new ArrayList<>();

        // Only detect cycles in composition edges (not references)
        List<CompositionEdge> compositionEdges =
                graph.getEdges().stream().filter(CompositionEdge::isComposition).toList();

        // Build set of nodes involved in composition
        Set<String> compositionNodes = new HashSet<>();
        compositionEdges.forEach(e -> {
            compositionNodes.add(e.source());
            compositionNodes.add(e.target());
        });

        // Use Tarjan's algorithm to detect cycles
        TarjanCycleDetector<String, CompositionEdge> detector =
                new TarjanCycleDetector<>(CompositionEdge::source, CompositionEdge::target);

        List<Cycle<CompositionEdge>> cycles =
                detector.detectCycles(compositionNodes, compositionEdges, CycleDetectionConfig.defaults());

        // Create anomaly for each cycle
        for (Cycle<CompositionEdge> cycle : cycles) {
            List<CompositionEdge> edges = cycle.edges();
            if (edges.isEmpty()) {
                continue;
            }

            CompositionEdge firstEdge = edges.get(0);
            String affectedType = firstEdge.source();

            // Extract all types in cycle
            Set<String> typesInCycle = edges.stream()
                    .flatMap(e -> List.of(e.source(), e.target()).stream())
                    .collect(Collectors.toSet());

            String cycleDescription = edges.stream()
                    .map(e -> String.format("%s.%s", simplifyTypeName(e.source()), e.fieldName()))
                    .collect(Collectors.joining(" -> "));

            String message = String.format(
                    "Composition cycle detected: %s. "
                            + "Cycles can cause serialization issues and indicate modeling problems.",
                    cycleDescription);

            anomalies.add(
                    Anomaly.error(AnomalyType.COMPOSITION_CYCLE, affectedType, message, new ArrayList<>(typesInCycle)));
        }

        return anomalies;
    }

    /**
     * Detects entities that are composed by multiple aggregates.
     *
     * @param graph           the composition graph
     * @param classifications the classifications
     * @return list of anomalies
     */
    private List<Anomaly> detectSharedEntities(CompositionGraph graph, Map<String, Classification> classifications) {

        List<Anomaly> anomalies = new ArrayList<>();

        // Build map of entity -> composing types
        Map<String, Set<String>> entityToComposers = new HashMap<>();
        for (CompositionEdge edge : graph.getEdges()) {
            if (!edge.isComposition()) {
                continue;
            }

            Classification targetClassification = classifications.get(edge.target());
            if (targetClassification != null && targetClassification.kind() == DomainKind.ENTITY) {
                entityToComposers
                        .computeIfAbsent(edge.target(), k -> new HashSet<>())
                        .add(edge.source());
            }
        }

        // Find entities with multiple composers
        for (Map.Entry<String, Set<String>> entry : entityToComposers.entrySet()) {
            String entity = entry.getKey();
            Set<String> composers = entry.getValue();

            if (composers.size() > 1) {
                String message = String.format(
                        "Entity '%s' is composed by multiple aggregates: %s. "
                                + "An entity should belong to exactly one aggregate.",
                        simplifyTypeName(entity),
                        composers.stream().map(this::simplifyTypeName).collect(Collectors.joining(", ")));

                anomalies.add(Anomaly.error(AnomalyType.SHARED_ENTITY, entity, message, new ArrayList<>(composers)));
            }
        }

        return anomalies;
    }

    /**
     * Detects aggregate roots without corresponding repositories.
     *
     * @param graph           the composition graph
     * @param classifications the classifications
     * @return list of anomalies
     */
    private List<Anomaly> detectAggregatesWithoutRepositories(
            CompositionGraph graph, Map<String, Classification> classifications) {

        List<Anomaly> anomalies = new ArrayList<>();

        // Find all aggregate roots classified by non-repository strategy
        for (Map.Entry<String, Classification> entry : classifications.entrySet()) {
            String typeName = entry.getKey();
            Classification classification = entry.getValue();

            if (classification.kind() == DomainKind.AGGREGATE_ROOT
                    && classification.strategy() != io.hexaglue.spi.classification.ClassificationStrategy.REPOSITORY) {

                String message = String.format(
                        "Aggregate root '%s' has no corresponding repository. "
                                + "Consider creating a repository or reviewing the classification.",
                        simplifyTypeName(typeName));

                anomalies.add(Anomaly.warning(AnomalyType.AGGREGATE_WITHOUT_REPOSITORY, typeName, message, List.of()));
            }
        }

        return anomalies;
    }

    /**
     * Detects value objects that have identity fields.
     *
     * @param graph           the composition graph
     * @param classifications the classifications
     * @return list of anomalies
     */
    private List<Anomaly> detectValueObjectsWithIdentity(
            CompositionGraph graph, Map<String, Classification> classifications) {

        List<Anomaly> anomalies = new ArrayList<>();

        for (Map.Entry<String, Classification> entry : classifications.entrySet()) {
            String typeName = entry.getKey();
            Classification classification = entry.getValue();

            // Check if classified as value object but node has identity
            if (classification.kind() == DomainKind.VALUE_OBJECT) {
                graph.getNode(typeName).ifPresent(node -> {
                    if (node.hasIdentity() && !node.isIdWrapper()) {
                        String message = String.format(
                                "Value object '%s' has an identity field. "
                                        + "Value objects should not have identity - consider reclassifying as ENTITY.",
                                simplifyTypeName(typeName));

                        anomalies.add(
                                Anomaly.warning(AnomalyType.VALUE_OBJECT_WITH_IDENTITY, typeName, message, List.of()));
                    }
                });
            }
        }

        return anomalies;
    }

    /**
     * Finds all aggregate roots in the classifications.
     *
     * @param classifications the classifications
     * @return set of aggregate root qualified names
     */
    private Set<String> findAggregateRoots(Map<String, Classification> classifications) {
        return classifications.entrySet().stream()
                .filter(e -> e.getValue().kind() == DomainKind.AGGREGATE_ROOT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Simplifies a qualified type name for display.
     *
     * @param qualifiedName the fully qualified name
     * @return the simple name
     */
    private String simplifyTypeName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
