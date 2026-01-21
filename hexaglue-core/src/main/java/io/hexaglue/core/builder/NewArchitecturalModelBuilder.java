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

package io.hexaglue.core.builder;

import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the new architectural model from classification results.
 *
 * <p>This builder orchestrates all the builders from Phase 3 to transform
 * TypeNodes and ClassificationResults into a complete ArchitecturalModel
 * with TypeRegistry, ClassificationReport, and specialized indexes.</p>
 *
 * <h2>Pipeline</h2>
 * <pre>{@code
 * ApplicationGraph + ClassificationResults
 *         │
 *         ▼
 * ┌─────────────────────────────┐
 * │  NewArchitecturalModelBuilder │
 * ├─────────────────────────────┤
 * │  - TypeStructureBuilder     │
 * │  - AggregateRootBuilder     │
 * │  - EntityBuilder            │
 * │  - DrivenPortBuilder        │
 * │  - etc.                     │
 * └─────────────────────────────┘
 *         │
 *         ▼
 * Result
 * ├── TypeRegistry
 * ├── ClassificationReport
 * ├── DomainIndex
 * └── PortIndex
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * NewArchitecturalModelBuilder builder = new NewArchitecturalModelBuilder();
 * Result result = builder.build(graphQuery, classificationResults);
 *
 * // Access the components
 * TypeRegistry registry = result.typeRegistry();
 * ClassificationReport report = result.classificationReport();
 * }</pre>
 *
 * @since 4.1.0
 */
public final class NewArchitecturalModelBuilder {

    private final TypeStructureBuilder structureBuilder;
    private final ClassificationTraceConverter traceConverter;
    private final FieldRoleDetector fieldRoleDetector;
    private final MethodRoleDetector methodRoleDetector;
    private final UnclassifiedCategoryDetector categoryDetector;
    private final AggregateRootBuilder aggregateRootBuilder;
    private final EntityBuilder entityBuilder;
    private final ValueObjectBuilder valueObjectBuilder;
    private final IdentifierBuilder identifierBuilder;
    private final DomainEventBuilder domainEventBuilder;
    private final DomainServiceBuilder domainServiceBuilder;
    private final DrivingPortBuilder drivingPortBuilder;
    private final DrivenPortBuilder drivenPortBuilder;
    private final ApplicationTypeBuilder applicationTypeBuilder;
    private final UnclassifiedTypeBuilder unclassifiedTypeBuilder;
    private final ClassificationReportBuilder reportBuilder;

    /**
     * Creates a new NewArchitecturalModelBuilder with default component builders.
     */
    public NewArchitecturalModelBuilder() {
        this.fieldRoleDetector = new FieldRoleDetector();
        this.methodRoleDetector = new MethodRoleDetector();
        this.structureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        this.traceConverter = new ClassificationTraceConverter();
        this.categoryDetector = new UnclassifiedCategoryDetector();

        this.aggregateRootBuilder = new AggregateRootBuilder(structureBuilder, traceConverter, fieldRoleDetector);
        this.entityBuilder = new EntityBuilder(structureBuilder, traceConverter, fieldRoleDetector);
        this.valueObjectBuilder = new ValueObjectBuilder(structureBuilder, traceConverter);
        this.identifierBuilder = new IdentifierBuilder(structureBuilder, traceConverter);
        this.domainEventBuilder = new DomainEventBuilder(structureBuilder, traceConverter);
        this.domainServiceBuilder = new DomainServiceBuilder(structureBuilder, traceConverter);
        this.drivingPortBuilder = new DrivingPortBuilder(structureBuilder, traceConverter);
        this.drivenPortBuilder = new DrivenPortBuilder(structureBuilder, traceConverter);
        this.applicationTypeBuilder = new ApplicationTypeBuilder(structureBuilder, traceConverter);
        this.unclassifiedTypeBuilder = new UnclassifiedTypeBuilder(structureBuilder, traceConverter, categoryDetector);
        this.reportBuilder = new ClassificationReportBuilder();
    }

    /**
     * Builds the architectural model from the given graph query and classification results.
     *
     * @param graphQuery the graph query interface for accessing type nodes
     * @param results the classification results
     * @return the built result containing registry, report, and indexes
     * @throws NullPointerException if any argument is null
     */
    public Result build(GraphQuery graphQuery, ClassificationResults results) {
        Objects.requireNonNull(graphQuery, "graphQuery must not be null");
        Objects.requireNonNull(results, "results must not be null");

        // 1. Create builder context
        BuilderContext context = BuilderContext.of(graphQuery, results);

        // 2. Build all ArchTypes from TypeNodes
        List<ArchType> archTypes = new ArrayList<>();
        List<UnclassifiedType> unclassified = new ArrayList<>();

        graphQuery.types().forEach(typeNode -> {
            Optional<ClassificationResult> classificationOpt = getClassification(typeNode, results);

            // Get or create classification result
            ClassificationResult classification =
                    classificationOpt.orElseGet(() -> ClassificationResult.unclassifiedDomain(typeNode.id(), null));

            ArchType archType = buildArchType(typeNode, classification, context);
            archTypes.add(archType);

            if (archType instanceof UnclassifiedType u) {
                unclassified.add(u);
            }
        });

        // 3. Build TypeRegistry
        TypeRegistry registry = TypeRegistry.builder().addAll(archTypes).build();

        // 4. Build ClassificationReport
        ClassificationReport report = reportBuilder.build(archTypes, unclassified, results);

        // 5. Build indexes
        DomainIndex domainIndex = DomainIndex.from(registry);
        PortIndex portIndex = PortIndex.from(registry);

        return new Result(registry, report, domainIndex, portIndex, Instant.now());
    }

    private Optional<ClassificationResult> getClassification(TypeNode typeNode, ClassificationResults results) {
        NodeId nodeId = typeNode.id();
        return results.get(nodeId);
    }

    private ArchType buildArchType(TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        String kind = classification.kind();

        if (kind == null || "UNCLASSIFIED".equals(kind)) {
            return unclassifiedTypeBuilder.build(typeNode, classification, context);
        }

        return switch (kind) {
            case "AGGREGATE_ROOT" -> buildAggregateRoot(typeNode, classification, context);
            case "ENTITY" -> entityBuilder.build(typeNode, classification, context);
            case "VALUE_OBJECT" -> valueObjectBuilder.build(typeNode, classification, context);
            case "IDENTIFIER" -> identifierBuilder.build(typeNode, classification, context);
            case "DOMAIN_EVENT", "EXTERNALIZED_EVENT" -> domainEventBuilder.build(typeNode, classification, context);
            case "DOMAIN_SERVICE" -> domainServiceBuilder.build(typeNode, classification, context);
            case "DRIVING_PORT" -> drivingPortBuilder.build(typeNode, classification, context);
            case "DRIVEN_PORT", "REPOSITORY", "GATEWAY" -> drivenPortBuilder.build(typeNode, classification, context);
            case "APPLICATION_SERVICE", "INBOUND_ONLY", "OUTBOUND_ONLY", "SAGA" ->
                applicationTypeBuilder.build(typeNode, classification, context);
            default -> unclassifiedTypeBuilder.build(typeNode, classification, context);
        };
    }

    private ArchType buildAggregateRoot(
            TypeNode typeNode, ClassificationResult classification, BuilderContext context) {
        try {
            return aggregateRootBuilder.build(typeNode, classification, context);
        } catch (IllegalStateException e) {
            // If aggregate root cannot be built (e.g., missing identity field),
            // fall back to unclassified with a note
            return unclassifiedTypeBuilder.build(typeNode, classification, context);
        }
    }

    /**
     * Result of building the architectural model.
     *
     * <p>Contains all the components that make up the new architectural model:
     * the type registry, classification report, and specialized indexes.</p>
     *
     * @param typeRegistry the registry containing all ArchTypes
     * @param classificationReport the report with stats, conflicts, and remediations
     * @param domainIndex the index for domain types
     * @param portIndex the index for port types
     * @param generatedAt the timestamp when the model was generated
     * @since 4.1.0
     */
    public record Result(
            TypeRegistry typeRegistry,
            ClassificationReport classificationReport,
            DomainIndex domainIndex,
            PortIndex portIndex,
            Instant generatedAt) {

        public Result {
            Objects.requireNonNull(typeRegistry, "typeRegistry must not be null");
            Objects.requireNonNull(classificationReport, "classificationReport must not be null");
            Objects.requireNonNull(domainIndex, "domainIndex must not be null");
            Objects.requireNonNull(portIndex, "portIndex must not be null");
            Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        }

        /**
         * Returns the total number of types in the model.
         *
         * @return the type count
         */
        public int size() {
            return typeRegistry.size();
        }

        /**
         * Returns whether there are any classification issues.
         *
         * @return true if there are unclassified types or conflicts
         */
        public boolean hasIssues() {
            return classificationReport.hasIssues();
        }
    }
}
