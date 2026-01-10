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

package io.hexaglue.core.classification.deterministic;

import io.hexaglue.core.classification.anomaly.Anomaly;
import io.hexaglue.core.classification.anomaly.AnomalyDetector;
import io.hexaglue.core.classification.discriminator.IdWrapperDiscriminator;
import io.hexaglue.core.classification.discriminator.RecordValueObjectDiscriminator;
import io.hexaglue.core.classification.discriminator.RepositoryDiscriminator;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.JavaType;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.composition.CompositionGraph;
import io.hexaglue.core.graph.composition.CompositionGraphBuilder;
import io.hexaglue.core.graph.composition.CompositionNode;
import io.hexaglue.spi.classification.ClassificationEvidence;
import io.hexaglue.spi.ir.DomainKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic classifier for domain types based on structural analysis.
 *
 * <p>This classifier implements a multi-phase classification algorithm:
 * <ol>
 *   <li><b>Phase 0:</b> Explicit Annotations - Detect @AggregateRoot, @Entity, @ValueObject</li>
 *   <li><b>Phase 1:</b> Repository Detection - Find aggregate roots via repositories</li>
 *   <li><b>Phase 2:</b> Record Value Objects - Classify records as value objects</li>
 *   <li><b>Phase 3:</b> ID Wrappers - Classify ID wrapper types</li>
 *   <li><b>Phase 4:</b> Composition Graph - Build relationship graph</li>
 *   <li><b>Phase 5:</b> Graph-Based Classification - Infer types from composition</li>
 *   <li><b>Phase 6:</b> Mark Unclassified - Mark remaining types as unclassified</li>
 *   <li><b>Phase 7:</b> Anomaly Detection - Detect architectural issues</li>
 * </ol>
 *
 * <p>The algorithm is deterministic - given the same input, it always produces
 * the same output in the same order.
 *
 * @since 3.0.0
 */
public final class DeterministicClassifier {

    private final IdWrapperDiscriminator idWrapperDiscriminator;
    private final RecordValueObjectDiscriminator recordValueObjectDiscriminator;
    private final RepositoryDiscriminator repositoryDiscriminator;
    private final CompositionGraphBuilder compositionGraphBuilder;
    private final AnomalyDetector anomalyDetector;

    /**
     * Creates a new deterministic classifier with default components.
     */
    public DeterministicClassifier() {
        this.idWrapperDiscriminator = new IdWrapperDiscriminator();
        this.recordValueObjectDiscriminator = new RecordValueObjectDiscriminator(idWrapperDiscriminator);
        this.repositoryDiscriminator = new RepositoryDiscriminator();
        this.compositionGraphBuilder = new CompositionGraphBuilder();
        this.anomalyDetector = new AnomalyDetector();
    }

    /**
     * Classifies all domain types in the given semantic model.
     *
     * <p>This method executes all classification phases and returns a complete
     * classification result with detected anomalies.
     *
     * @param model the semantic model containing domain types
     * @param graph the application graph (for additional context)
     * @return the classification result
     */
    public ClassificationResult classify(JavaSemanticModel model, ApplicationGraph graph) {
        Objects.requireNonNull(model, "model required");
        Objects.requireNonNull(graph, "graph required");

        Map<String, Classification> result = new HashMap<>();
        List<Anomaly> anomalies = new ArrayList<>();

        // PHASE 0: Explicit Annotations (not implemented in this version)
        // classifyByAnnotations(model, result);

        // PHASE 1: Repository Detection
        classifyByRepositories(model, graph, result);

        // PHASE 2: Record Value Objects
        classifyRecordValueObjects(model, result);

        // PHASE 3: ID Wrappers
        classifyIdWrappers(model, result);

        // PHASE 4: Build Composition Graph
        CompositionGraph compGraph = compositionGraphBuilder.build(model);

        // PHASE 5: Graph-Based Classification
        classifyByCompositionGraph(compGraph, result);

        // PHASE 6: Mark Unclassified
        markUnclassified(model, result);

        // PHASE 7: Anomaly Detection
        anomalies.addAll(anomalyDetector.detect(compGraph, result));

        return new ClassificationResult(result, anomalies);
    }

    /**
     * Phase 1: Classifies types by analyzing repository interfaces.
     *
     * @param model  the semantic model
     * @param graph  the application graph
     * @param result the result map to populate
     */
    private void classifyByRepositories(
            JavaSemanticModel model, ApplicationGraph graph, Map<String, Classification> result) {

        // Note: This is a simplified version that works with JavaType
        // A full implementation would need to convert JavaType to CtInterface
        // For now, we'll skip this phase in the JavaSemanticModel-based implementation
        // This will be properly implemented when integrated with the Spoon-based frontend

        // Placeholder for repository detection
        // In real implementation, we'd iterate over interfaces and use RepositoryDiscriminator
    }

    /**
     * Phase 2: Classifies record types as value objects.
     *
     * @param model  the semantic model
     * @param result the result map to populate
     */
    private void classifyRecordValueObjects(JavaSemanticModel model, Map<String, Classification> result) {

        for (JavaType type : model.types()) {
            // Skip if already classified
            if (result.containsKey(type.qualifiedName())) {
                continue;
            }

            // Check if it's a record
            if (!type.isRecord()) {
                continue;
            }

            // Note: We can't use RecordValueObjectDiscriminator directly because it needs CtRecord
            // This is a simplified version
            boolean hasIdentity = type.hasIdField();
            boolean isIdWrapper =
                    type.simpleName().endsWith("Id") || type.simpleName().endsWith("ID");

            if (!hasIdentity && !isIdWrapper) {
                // Record without identity -> VALUE_OBJECT
                String reasoning = String.format(
                        "Java record '%s' without identity is classified as VALUE_OBJECT", type.simpleName());

                List<ClassificationEvidence> evidences = List.of(
                        ClassificationEvidence.positive(
                                "IS_RECORD", 50, "Type is a Java record, which are immutable by design"),
                        ClassificationEvidence.positive("NO_IDENTITY", 30, "Record has no 'id' component"));

                result.put(type.qualifiedName(), Classification.fromRecord(type.qualifiedName(), reasoning, evidences));
            }
        }
    }

    /**
     * Phase 3: Classifies ID wrapper types.
     *
     * @param model  the semantic model
     * @param result the result map to populate
     */
    private void classifyIdWrappers(JavaSemanticModel model, Map<String, Classification> result) {

        for (JavaType type : model.types()) {
            // Skip if already classified
            if (result.containsKey(type.qualifiedName())) {
                continue;
            }

            // Simple ID wrapper detection
            String simpleName = type.simpleName();
            boolean hasIdNaming = simpleName.endsWith("Id") || simpleName.endsWith("ID");

            if (hasIdNaming) {
                // Check if it's actually an ID wrapper (single field of primitive type)
                // This is simplified - full implementation would use IdWrapperDiscriminator
                if (type.fields().size() == 1) {
                    String reasoning =
                            String.format("Type '%s' is an ID wrapper (single field with Id naming)", simpleName);

                    List<ClassificationEvidence> evidences = List.of(
                            ClassificationEvidence.positive("ID_NAMING", 40, "Type name ends with 'Id' or 'ID'"),
                            ClassificationEvidence.positive("SINGLE_FIELD", 40, "Type has exactly one field"));

                    result.put(
                            type.qualifiedName(),
                            new Classification(
                                    type.qualifiedName(),
                                    DomainKind.IDENTIFIER,
                                    io.hexaglue.spi.classification.CertaintyLevel.CERTAIN_BY_STRUCTURE,
                                    io.hexaglue.spi.classification.ClassificationStrategy.RECORD,
                                    reasoning,
                                    evidences));
                }
            }
        }
    }

    /**
     * Phase 5: Classifies types based on composition graph analysis.
     *
     * @param graph  the composition graph
     * @param result the result map to populate
     */
    private void classifyByCompositionGraph(CompositionGraph graph, Map<String, Classification> result) {

        // Identify composition roots (potential aggregate roots)
        for (String rootType : graph.getCompositionRoots()) {
            // Skip if already classified
            if (result.containsKey(rootType)) {
                continue;
            }

            Optional<CompositionNode> nodeOpt = graph.getNode(rootType);
            if (nodeOpt.isEmpty()) {
                continue;
            }

            CompositionNode node = nodeOpt.get();

            // If it has identity and is a composition root, likely an aggregate root
            if (node.hasIdentity() && !node.isIdWrapper()) {
                String reasoning = String.format(
                        "Type '%s' is a composition root with identity, inferred as AGGREGATE_ROOT", node.simpleName());

                List<ClassificationEvidence> evidences = List.of(
                        ClassificationEvidence.positive(
                                "COMPOSITION_ROOT", 40, "Type is not composed by any other type"),
                        ClassificationEvidence.positive("HAS_IDENTITY", 30, "Type has an identity field"));

                result.put(
                        rootType,
                        Classification.fromComposition(rootType, DomainKind.AGGREGATE_ROOT, reasoning, evidences));
            }
        }

        // Classify composed types
        for (Map.Entry<String, CompositionNode> entry : graph.getNodes().entrySet()) {
            String typeName = entry.getKey();
            CompositionNode node = entry.getValue();

            // Skip if already classified
            if (result.containsKey(typeName)) {
                continue;
            }

            // Check if this type is composed by others
            if (!graph.getComposingTypes(typeName).isEmpty()) {
                // It's composed by other types

                if (node.hasIdentity() && !node.isIdWrapper()) {
                    // Has identity and is composed -> ENTITY
                    String reasoning = String.format(
                            "Type '%s' has identity and is composed by other types, classified as ENTITY",
                            node.simpleName());

                    List<ClassificationEvidence> evidences = List.of(
                            ClassificationEvidence.positive("IS_COMPOSED", 40, "Type is composed by other types"),
                            ClassificationEvidence.positive("HAS_IDENTITY", 30, "Type has an identity field"));

                    result.put(
                            typeName,
                            Classification.fromComposition(typeName, DomainKind.ENTITY, reasoning, evidences));

                } else if (!node.hasIdentity()) {
                    // No identity and is composed -> VALUE_OBJECT
                    String reasoning = String.format(
                            "Type '%s' has no identity and is composed by other types, classified as VALUE_OBJECT",
                            node.simpleName());

                    List<ClassificationEvidence> evidences = List.of(
                            ClassificationEvidence.positive("IS_COMPOSED", 40, "Type is composed by other types"),
                            ClassificationEvidence.positive("NO_IDENTITY", 30, "Type has no identity field"));

                    result.put(
                            typeName,
                            Classification.fromComposition(typeName, DomainKind.VALUE_OBJECT, reasoning, evidences));
                }
            }
        }
    }

    /**
     * Phase 6: Marks remaining types as unclassified.
     *
     * @param model  the semantic model
     * @param result the result map to populate
     */
    private void markUnclassified(JavaSemanticModel model, Map<String, Classification> result) {

        for (JavaType type : model.types()) {
            String qualifiedName = type.qualifiedName();

            // Skip if already classified
            if (result.containsKey(qualifiedName)) {
                continue;
            }

            String reasoning =
                    String.format("Type '%s' could not be classified by any deterministic rule", type.simpleName());

            result.put(qualifiedName, Classification.unclassified(qualifiedName, reasoning));
        }
    }

    /**
     * Returns a summary of the classification components.
     *
     * @return human-readable summary
     */
    public String getComponentsSummary() {
        return String.format(
                "DeterministicClassifier Components:\n" + "  - IdWrapperDiscriminator: %s\n"
                        + "  - RecordValueObjectDiscriminator: %s\n"
                        + "  - RepositoryDiscriminator: %s\n"
                        + "  - CompositionGraphBuilder: %s\n"
                        + "  - AnomalyDetector: %s",
                idWrapperDiscriminator.getClass().getSimpleName(),
                recordValueObjectDiscriminator.getClass().getSimpleName(),
                repositoryDiscriminator.getClass().getSimpleName(),
                compositionGraphBuilder.getClass().getSimpleName(),
                anomalyDetector.getClass().getSimpleName());
    }
}
