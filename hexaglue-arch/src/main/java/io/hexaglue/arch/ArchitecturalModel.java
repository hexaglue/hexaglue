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

package io.hexaglue.arch;

import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.arch.model.report.PrioritizedRemediation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The complete architectural model of an analyzed application.
 *
 * <p>This is the main entry point for accessing the architectural analysis results.
 * It contains the type registry (all classified types), relationship store
 * (indexed relationships), and metadata about the analysis.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Built by ArchitecturalModelBuilder
 * ArchitecturalModel model = builder.build();
 *
 * // Access domain types via DomainIndex
 * model.domainIndex().ifPresent(domain -> {
 *     domain.aggregateRoots().forEach(agg -> ...);
 *     domain.entities().forEach(entity -> ...);
 *     domain.valueObjects().forEach(vo -> ...);
 * });
 *
 * // Access ports via PortIndex
 * model.portIndex().ifPresent(ports -> {
 *     ports.drivingPorts().forEach(port -> ...);
 *     ports.repositories().forEach(repo -> ...);
 *     ports.gateways().forEach(gw -> ...);
 * });
 *
 * // Query relationships (O(1))
 * Optional<DrivenPort> repo = model.repositoryFor(aggregate.id().toElementId());
 *
 * // Access classification report
 * model.classificationReport().ifPresent(report -> {
 *     report.remediations().forEach(r -> ...);
 *     report.actionRequired().forEach(u -> ...);
 * });
 * }</pre>
 *
 * @param project project context information
 * @param registry the element registry (legacy, for backward compatibility)
 * @param relationships the relationship store for O(1) lookups
 * @param analysisMetadata metadata about the analysis process
 * @param typeRegistryV2 the type registry containing all ArchType instances
 * @param classificationReportV2 the classification report with stats and remediations
 * @param domainIndexV2 the domain index for domain type access
 * @param portIndexV2 the port index for port type access
 * @since 4.0.0
 * @since 5.0.0 removed deprecated methods using legacy types
 */
public record ArchitecturalModel(
        ProjectContext project,
        ElementRegistry registry,
        RelationshipStore relationships,
        AnalysisMetadata analysisMetadata,
        TypeRegistry typeRegistryV2,
        ClassificationReport classificationReportV2,
        DomainIndex domainIndexV2,
        PortIndex portIndexV2) {

    /**
     * Creates a new ArchitecturalModel instance.
     *
     * <p>Legacy fields (project, registry, relationships, analysisMetadata) are required.
     * New v4.1.0 fields (typeRegistryV2, classificationReportV2, domainIndexV2, portIndexV2)
     * may be null for backward compatibility.</p>
     *
     * @param project the project context, must not be null
     * @param registry the element registry, must not be null
     * @param relationships the relationship store, must not be null
     * @param analysisMetadata the analysis metadata, must not be null
     * @param typeRegistryV2 the type registry (v4.1.0), may be null
     * @param classificationReportV2 the classification report (v4.1.0), may be null
     * @param domainIndexV2 the domain index (v4.1.0), may be null
     * @param portIndexV2 the port index (v4.1.0), may be null
     * @throws NullPointerException if any required field is null
     */
    public ArchitecturalModel {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(relationships, "relationships must not be null");
        Objects.requireNonNull(analysisMetadata, "analysisMetadata must not be null");
        // typeRegistryV2, classificationReportV2, domainIndexV2, portIndexV2 may be null
    }

    // === New v4.1.0 API ===

    /**
     * Returns the new type registry if available.
     *
     * @return an optional containing the type registry, or empty if not available
     * @since 4.1.0
     */
    public Optional<TypeRegistry> typeRegistry() {
        return Optional.ofNullable(typeRegistryV2);
    }

    /**
     * Returns the classification report if available.
     *
     * @return an optional containing the classification report, or empty if not available
     * @since 4.1.0
     */
    public Optional<ClassificationReport> classificationReport() {
        return Optional.ofNullable(classificationReportV2);
    }

    /**
     * Returns the domain index if available.
     *
     * @return an optional containing the domain index, or empty if not available
     * @since 4.1.0
     */
    public Optional<DomainIndex> domainIndex() {
        return Optional.ofNullable(domainIndexV2);
    }

    /**
     * Returns the port index if available.
     *
     * @return an optional containing the port index, or empty if not available
     * @since 4.1.0
     */
    public Optional<PortIndex> portIndex() {
        return Optional.ofNullable(portIndexV2);
    }

    /**
     * Returns whether there are classification issues.
     *
     * <p>If no classification report is available, returns false.</p>
     *
     * @return true if there are unclassified types or conflicts
     * @since 4.1.0
     */
    public boolean hasClassificationIssues() {
        return classificationReportV2 != null && classificationReportV2.hasIssues();
    }

    /**
     * Returns the top prioritized remediations.
     *
     * <p>If no classification report is available, returns an empty list.</p>
     *
     * @param limit the maximum number of remediations to return
     * @return the top remediations, sorted by priority
     * @since 4.1.0
     */
    public List<PrioritizedRemediation> topRemediations(int limit) {
        if (classificationReportV2 == null) {
            return List.of();
        }
        return classificationReportV2.remediations().stream()
                .sorted()
                .limit(limit)
                .toList();
    }

    // === Reference Resolution ===

    /**
     * Resolves an element reference.
     *
     * @param ref the reference to resolve
     * @param <T> the expected element type
     * @return the resolved element, if found and type matches
     */
    public <T extends ArchElement> Optional<T> resolve(ElementRef<T> ref) {
        return ref.resolveOpt(registry);
    }

    /**
     * Resolves an element reference or throws.
     *
     * @param ref the reference to resolve
     * @param <T> the expected element type
     * @return the resolved element
     * @throws UnresolvedReferenceException if resolution fails
     */
    public <T extends ArchElement> T get(ElementRef<T> ref) {
        return ref.resolveOrThrow(registry);
    }

    // === Relationship Queries (O(1)) ===

    /**
     * Finds the repository that manages an aggregate (O(1) lookup).
     *
     * <p>Uses the port index if available, falls back to relationship store lookup.</p>
     *
     * @param aggregateId the aggregate identifier
     * @return the repository, if found
     * @since 5.0.0 updated to return model.DrivenPort
     */
    public Optional<DrivenPort> repositoryFor(ElementId aggregateId) {
        if (portIndexV2 != null) {
            return portIndexV2.repositoryFor(TypeId.fromElementId(aggregateId));
        }
        return Optional.empty();
    }

    /**
     * Returns whether an aggregate has a repository.
     *
     * @param aggregateId the aggregate identifier
     * @return true if a repository exists
     */
    public boolean hasRepository(ElementId aggregateId) {
        return repositoryFor(aggregateId).isPresent();
    }

    // === Statistics ===

    /**
     * Returns the total number of elements in the model.
     *
     * @return the element count
     */
    public int size() {
        return registry.size();
    }

    // === Builder ===

    /**
     * Creates a builder for constructing an ArchitecturalModel.
     *
     * @param project the project context
     * @return a new builder
     */
    public static Builder builder(ProjectContext project) {
        return new Builder(project);
    }

    /**
     * Builder for ArchitecturalModel.
     */
    public static final class Builder {
        private final ProjectContext project;
        private final ElementRegistry.Builder registryBuilder;
        private final RelationshipStore.Builder relationshipsBuilder;

        // New v4.1.0 fields
        private TypeRegistry typeRegistryV2;
        private ClassificationReport classificationReportV2;
        private DomainIndex domainIndexV2;
        private PortIndex portIndexV2;

        private Builder(ProjectContext project) {
            this.project = Objects.requireNonNull(project, "project must not be null");
            this.registryBuilder = ElementRegistry.builder();
            this.relationshipsBuilder = RelationshipStore.builder();
        }

        /**
         * Adds an element to the model.
         *
         * @param element the element to add
         * @return this builder
         */
        public Builder add(ArchElement element) {
            registryBuilder.add(element);
            return this;
        }

        /**
         * Adds a relationship to the model.
         *
         * @param from the source element
         * @param type the relationship type
         * @param to the target element
         * @return this builder
         */
        public Builder addRelation(ElementId from, RelationType type, ElementId to) {
            relationshipsBuilder.addRelation(from, type, to);
            return this;
        }

        /**
         * Adds a MANAGES relationship (repository → aggregate).
         *
         * @param repository the repository
         * @param aggregate the aggregate
         * @return this builder
         */
        public Builder addManages(ElementId repository, ElementId aggregate) {
            relationshipsBuilder.addManages(repository, aggregate);
            return this;
        }

        /**
         * Adds an IMPLEMENTS relationship (type → interface).
         *
         * @param type the implementing type
         * @param iface the interface
         * @return this builder
         */
        public Builder addImplements(ElementId type, ElementId iface) {
            relationshipsBuilder.addImplements(type, iface);
            return this;
        }

        /**
         * Sets the type registry (v4.1.0).
         *
         * @param typeRegistry the type registry
         * @return this builder
         * @since 4.1.0
         */
        public Builder typeRegistry(TypeRegistry typeRegistry) {
            this.typeRegistryV2 = typeRegistry;
            return this;
        }

        /**
         * Sets the classification report (v4.1.0).
         *
         * @param report the classification report
         * @return this builder
         * @since 4.1.0
         */
        public Builder classificationReport(ClassificationReport report) {
            this.classificationReportV2 = report;
            return this;
        }

        /**
         * Sets the domain index (v4.1.0).
         *
         * @param domainIndex the domain index
         * @return this builder
         * @since 4.1.0
         */
        public Builder domainIndex(DomainIndex domainIndex) {
            this.domainIndexV2 = domainIndex;
            return this;
        }

        /**
         * Sets the port index (v4.1.0).
         *
         * @param portIndex the port index
         * @return this builder
         * @since 4.1.0
         */
        public Builder portIndex(PortIndex portIndex) {
            this.portIndexV2 = portIndex;
            return this;
        }

        /**
         * Builds the model with the specified metadata.
         *
         * @param metadata the analysis metadata
         * @return the built ArchitecturalModel
         */
        public ArchitecturalModel build(AnalysisMetadata metadata) {
            return new ArchitecturalModel(
                    project,
                    registryBuilder.build(),
                    relationshipsBuilder.build(),
                    metadata,
                    typeRegistryV2,
                    classificationReportV2,
                    domainIndexV2,
                    portIndexV2);
        }

        /**
         * Builds the model with auto-generated metadata.
         *
         * @return the built ArchitecturalModel
         */
        public ArchitecturalModel build() {
            ElementRegistry registry = registryBuilder.build();
            return new ArchitecturalModel(
                    project,
                    registry,
                    relationshipsBuilder.build(),
                    AnalysisMetadata.forTesting(registry.size()),
                    typeRegistryV2,
                    classificationReportV2,
                    domainIndexV2,
                    portIndexV2);
        }
    }
}
