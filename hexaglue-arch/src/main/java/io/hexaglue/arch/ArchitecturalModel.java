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
import io.hexaglue.arch.model.index.CompositionIndex;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.arch.model.report.PrioritizedRemediation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
 * @param relationships the relationship store for O(1) lookups
 * @param analysisMetadata metadata about the analysis process
 * @param typeRegistryInternal the type registry containing all ArchType instances (internal field)
 * @param classificationReportInternal the classification report with stats and remediations (internal field)
 * @param domainIndexInternal the domain index for domain type access (internal field)
 * @param portIndexInternal the port index for port type access (internal field)
 * @param compositionIndexInternal the composition index for cross-package relationships (internal field)
 * @param moduleIndexInternal the module index for multi-module type-to-module mapping (internal field)
 * @since 4.0.0
 * @since 5.0.0 removed ElementRegistry, ElementRef, and related legacy types
 * @since 5.0.0 added compositionIndex for cross-package composition detection
 * @since 5.0.0 added moduleIndex for multi-module support
 */
public record ArchitecturalModel(
        ProjectContext project,
        RelationshipStore relationships,
        AnalysisMetadata analysisMetadata,
        TypeRegistry typeRegistryInternal,
        ClassificationReport classificationReportInternal,
        DomainIndex domainIndexInternal,
        PortIndex portIndexInternal,
        CompositionIndex compositionIndexInternal,
        ModuleIndex moduleIndexInternal) {

    /**
     * Creates a new ArchitecturalModel instance.
     *
     * <p>Core fields (project, relationships, analysisMetadata) are required.
     * Index fields (typeRegistry, classificationReport, domainIndex, portIndex)
     * may be null for backward compatibility during migration.</p>
     *
     * @param project the project context, must not be null
     * @param relationships the relationship store, must not be null
     * @param analysisMetadata the analysis metadata, must not be null
     * @param typeRegistryInternal the type registry, may be null
     * @param classificationReportInternal the classification report, may be null
     * @param domainIndexInternal the domain index, may be null
     * @param portIndexInternal the port index, may be null
     * @param compositionIndexInternal the composition index, may be null
     * @param moduleIndexInternal the module index, may be null (absent in mono-module)
     * @throws NullPointerException if any required field is null
     */
    public ArchitecturalModel {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(relationships, "relationships must not be null");
        Objects.requireNonNull(analysisMetadata, "analysisMetadata must not be null");
        // typeRegistryInternal, classificationReportInternal, domainIndexInternal, portIndexInternal,
        // compositionIndexInternal, moduleIndexInternal may be null for backward compatibility
    }

    // === New v4.1.0 API ===

    /**
     * Returns the new type registry if available.
     *
     * @return an optional containing the type registry, or empty if not available
     * @since 4.1.0
     */
    public Optional<TypeRegistry> typeRegistry() {
        return Optional.ofNullable(typeRegistryInternal);
    }

    /**
     * Returns the classification report if available.
     *
     * @return an optional containing the classification report, or empty if not available
     * @since 4.1.0
     */
    public Optional<ClassificationReport> classificationReport() {
        return Optional.ofNullable(classificationReportInternal);
    }

    /**
     * Returns the domain index if available.
     *
     * @return an optional containing the domain index, or empty if not available
     * @since 4.1.0
     */
    public Optional<DomainIndex> domainIndex() {
        return Optional.ofNullable(domainIndexInternal);
    }

    /**
     * Returns the port index if available.
     *
     * @return an optional containing the port index, or empty if not available
     * @since 4.1.0
     */
    public Optional<PortIndex> portIndex() {
        return Optional.ofNullable(portIndexInternal);
    }

    /**
     * Returns the composition index if available.
     *
     * <p>The composition index provides convenient queries for cross-package
     * compositional relationships (OWNS, CONTAINS, REFERENCES) enabling
     * accurate domain model diagrams.</p>
     *
     * @return an optional containing the composition index, or empty if not available
     * @since 5.0.0
     */
    public Optional<CompositionIndex> compositionIndex() {
        return Optional.ofNullable(compositionIndexInternal);
    }

    /**
     * Returns the module index if available.
     *
     * <p>The module index maps types to their containing module and provides
     * queries for module-level navigation. It is only present in multi-module
     * projects.</p>
     *
     * @return an optional containing the module index, or empty if not available
     * @since 5.0.0
     */
    public Optional<ModuleIndex> moduleIndex() {
        return Optional.ofNullable(moduleIndexInternal);
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
        return classificationReportInternal != null && classificationReportInternal.hasIssues();
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
        if (classificationReportInternal == null) {
            return List.of();
        }
        return classificationReportInternal.remediations().stream()
                .sorted()
                .limit(limit)
                .toList();
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
        if (portIndexInternal != null) {
            return portIndexInternal.repositoryFor(TypeId.fromElementId(aggregateId));
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
     * @since 5.0.0 updated to use typeRegistry
     */
    public int size() {
        return typeRegistryInternal != null ? typeRegistryInternal.size() : 0;
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
     *
     * @since 5.0.0 removed ElementRegistry support, use typeRegistry() instead
     * @since 5.0.0 added compositionIndex support
     * @since 5.0.0 added moduleIndex support
     */
    public static final class Builder {
        private final ProjectContext project;
        private final RelationshipStore.Builder relationshipsBuilder;

        // Index fields
        private TypeRegistry typeRegistry;
        private ClassificationReport classificationReport;
        private DomainIndex domainIndex;
        private PortIndex portIndex;
        private CompositionIndex compositionIndex;
        private ModuleIndex moduleIndex;

        private Builder(ProjectContext project) {
            this.project = Objects.requireNonNull(project, "project must not be null");
            this.relationshipsBuilder = RelationshipStore.builder();
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
            this.typeRegistry = typeRegistry;
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
            this.classificationReport = report;
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
            this.domainIndex = domainIndex;
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
            this.portIndex = portIndex;
            return this;
        }

        /**
         * Sets the composition index (v5.0.0).
         *
         * @param compositionIndex the composition index
         * @return this builder
         * @since 5.0.0
         */
        public Builder compositionIndex(CompositionIndex compositionIndex) {
            this.compositionIndex = compositionIndex;
            return this;
        }

        /**
         * Sets the module index (v5.0.0).
         *
         * @param moduleIndex the module index
         * @return this builder
         * @since 5.0.0
         */
        public Builder moduleIndex(ModuleIndex moduleIndex) {
            this.moduleIndex = moduleIndex;
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
                    relationshipsBuilder.build(),
                    metadata,
                    this.typeRegistry,
                    this.classificationReport,
                    this.domainIndex,
                    this.portIndex,
                    this.compositionIndex,
                    this.moduleIndex);
        }

        /**
         * Builds the model with auto-generated metadata.
         *
         * @return the built ArchitecturalModel
         * @since 5.0.0 updated to use typeRegistry for size calculation
         */
        public ArchitecturalModel build() {
            int size = this.typeRegistry != null ? this.typeRegistry.size() : 0;
            return new ArchitecturalModel(
                    project,
                    relationshipsBuilder.build(),
                    AnalysisMetadata.forTesting(size),
                    this.typeRegistry,
                    this.classificationReport,
                    this.domainIndex,
                    this.portIndex,
                    this.compositionIndex,
                    this.moduleIndex);
        }
    }
}
