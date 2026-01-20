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

import io.hexaglue.arch.adapters.DrivenAdapter;
import io.hexaglue.arch.adapters.DrivingAdapter;
import io.hexaglue.arch.domain.Aggregate;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.arch.domain.DomainEvent;
import io.hexaglue.arch.domain.DomainService;
import io.hexaglue.arch.domain.Identifier;
import io.hexaglue.arch.domain.ValueObject;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.arch.model.report.PrioritizedRemediation;
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.query.DefaultModelQuery;
import io.hexaglue.arch.query.ModelQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The complete architectural model of an analyzed application.
 *
 * <p>This is the main entry point for accessing the architectural analysis results.
 * It contains the element registry (all classified types), relationship store
 * (indexed relationships), and metadata about the analysis.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Built by ArchitecturalModelBuilder
 * ArchitecturalModel model = builder.build();
 *
 * // Access elements
 * model.aggregates().forEach(agg -> ...);
 * model.drivenPorts().filter(p -> p.isRepository()).forEach(repo -> ...);
 *
 * // Resolve references
 * Optional<Aggregate> agg = model.resolve(aggregateRef);
 *
 * // Query relationships (O(1))
 * Optional<DrivenPort> repo = model.repositoryFor(aggregate.id());
 *
 * // New v4.1.0 API
 * model.typeRegistry().ifPresent(reg -> reg.all().forEach(...));
 * model.classificationReport().ifPresent(report -> report.remediations().forEach(...));
 * }</pre>
 *
 * @param project project context information
 * @param registry the element registry containing all classified types
 * @param relationships the relationship store for O(1) lookups
 * @param analysisMetadata metadata about the analysis process
 * @param typeRegistryV2 the new type registry (v4.1.0), may be null for legacy usage
 * @param classificationReportV2 the classification report (v4.1.0), may be null for legacy usage
 * @param domainIndexV2 the domain index (v4.1.0), may be null for legacy usage
 * @param portIndexV2 the port index (v4.1.0), may be null for legacy usage
 * @since 4.0.0
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

    // === Domain Element Access (Legacy - Deprecated) ===

    /**
     * Returns all aggregates in the model.
     *
     * @return stream of aggregates
     * @deprecated Use {@link #domainIndex()} with {@link DomainIndex#aggregateRoots()} instead.
     *     The new API provides enriched types with computed metadata (identity field, entities, etc.).
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.aggregates().forEach(agg -> ...);
     *
     *     // After (v4.1.0)
     *     model.domainIndex().ifPresent(domain ->
     *         domain.aggregateRoots().forEach(agg -> {
     *             Field identity = agg.identityField();
     *             List<TypeRef> entities = agg.entities();
     *         }));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<Aggregate> aggregates() {
        return registry.all(Aggregate.class);
    }

    /**
     * Returns all domain entities in the model.
     *
     * @return stream of domain entities
     * @deprecated Use {@link #domainIndex()} with {@link DomainIndex#aggregateRoots()} and
     *     {@link DomainIndex#entities()} instead. The new API separates aggregate roots from
     *     child entities and provides enriched structure access.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.domainEntities().filter(DomainEntity::isAggregateRoot).forEach(...);
     *
     *     // After (v4.1.0)
     *     model.domainIndex().ifPresent(domain -> {
     *         domain.aggregateRoots().forEach(agg -> ...);  // Aggregate roots
     *         domain.entities().forEach(entity -> ...);     // Child entities
     *     });
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DomainEntity> domainEntities() {
        return registry.all(DomainEntity.class);
    }

    /**
     * Returns all value objects in the model.
     *
     * @return stream of value objects
     * @deprecated Use {@link #domainIndex()} with {@link DomainIndex#valueObjects()} instead.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.valueObjects().forEach(vo -> ...);
     *
     *     // After (v4.1.0)
     *     model.domainIndex().ifPresent(domain ->
     *         domain.valueObjects().forEach(vo -> vo.structure().fields()...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<ValueObject> valueObjects() {
        return registry.all(ValueObject.class);
    }

    /**
     * Returns all identifiers in the model.
     *
     * @return stream of identifiers
     * @deprecated Use {@link #domainIndex()} with {@link DomainIndex#identifiers()} instead.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.identifiers().forEach(id -> ...);
     *
     *     // After (v4.1.0)
     *     model.domainIndex().ifPresent(domain ->
     *         domain.identifiers().forEach(id -> id.wrappedType()...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<Identifier> identifiers() {
        return registry.all(Identifier.class);
    }

    /**
     * Returns all domain events in the model.
     *
     * @return stream of domain events
     * @deprecated Use {@link #domainIndex()} with {@link DomainIndex#domainEvents()} instead.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.domainEvents().forEach(event -> ...);
     *
     *     // After (v4.1.0)
     *     model.domainIndex().ifPresent(domain ->
     *         domain.domainEvents().forEach(event -> event.structure()...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DomainEvent> domainEvents() {
        return registry.all(DomainEvent.class);
    }

    /**
     * Returns all domain services in the model.
     *
     * @return stream of domain services
     * @deprecated Use {@link #domainIndex()} with {@link DomainIndex#domainServices()} instead.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.domainServices().forEach(svc -> ...);
     *
     *     // After (v4.1.0)
     *     model.domainIndex().ifPresent(domain ->
     *         domain.domainServices().forEach(svc -> svc.structure()...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DomainService> domainServices() {
        return registry.all(DomainService.class);
    }

    // === Port Access (Legacy - Deprecated) ===

    /**
     * Returns all driving ports in the model.
     *
     * @return stream of driving ports
     * @deprecated Use {@link #portIndex()} with {@link PortIndex#drivingPorts()} instead.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.drivingPorts().forEach(port -> ...);
     *
     *     // After (v4.1.0)
     *     model.portIndex().ifPresent(ports ->
     *         ports.drivingPorts().forEach(port -> port.structure()...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DrivingPort> drivingPorts() {
        return registry.all(DrivingPort.class);
    }

    /**
     * Returns all driven ports in the model.
     *
     * @return stream of driven ports
     * @deprecated Use {@link #portIndex()} with {@link PortIndex#drivenPorts()} instead.
     *     The new API provides {@link io.hexaglue.arch.model.DrivenPort} with enriched
     *     metadata including {@code DrivenPortType} and {@code managedAggregate()}.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.drivenPorts().filter(p -> p.isRepository()).forEach(...);
     *
     *     // After (v4.1.0)
     *     model.portIndex().ifPresent(ports -> {
     *         ports.repositories().forEach(repo -> repo.managedAggregate()...);
     *         ports.gateways().forEach(gw -> ...);
     *     });
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DrivenPort> drivenPorts() {
        return registry.all(DrivenPort.class);
    }

    /**
     * Returns all application services in the model.
     *
     * @return stream of application services
     * @deprecated Use {@link #domainIndex()} with appropriate query methods instead.
     *     Application services will be accessible via the type registry.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.applicationServices().forEach(svc -> ...);
     *
     *     // After (v4.1.0)
     *     model.typeRegistry().ifPresent(reg ->
     *         reg.all(ApplicationService.class).forEach(svc -> ...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<ApplicationService> applicationServices() {
        return registry.all(ApplicationService.class);
    }

    // === Adapter Access (Legacy - Deprecated) ===

    /**
     * Returns all driving adapters in the model.
     *
     * <p>Note: Adapters are generated by plugins, not classified by the core engine.
     * They are outside the classification perimeter.</p>
     *
     * @return stream of driving adapters
     * @deprecated Adapters are generated by plugins and should not be accessed via the model.
     *     Use the type registry for generic access if needed.
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DrivingAdapter> drivingAdapters() {
        return registry.all(DrivingAdapter.class);
    }

    /**
     * Returns all driven adapters in the model.
     *
     * <p>Note: Adapters are generated by plugins, not classified by the core engine.
     * They are outside the classification perimeter.</p>
     *
     * @return stream of driven adapters
     * @deprecated Adapters are generated by plugins and should not be accessed via the model.
     *     Use the type registry for generic access if needed.
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<DrivenAdapter> drivenAdapters() {
        return registry.all(DrivenAdapter.class);
    }

    /**
     * Returns all unclassified types in the model.
     *
     * @return stream of unclassified types
     * @deprecated Use {@link #classificationReport()} with {@link ClassificationReport#actionRequired()}
     *     for types needing attention, or {@link #typeRegistry()} with
     *     {@link TypeRegistry#all(Class)} for all unclassified types.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     model.unclassifiedTypes().forEach(u -> ...);
     *
     *     // After (v4.1.0) - Types requiring action
     *     model.classificationReport().ifPresent(report ->
     *         report.actionRequired().forEach(u -> {
     *             UnclassifiedCategory category = u.category();
     *             List<RemediationHint> hints = u.classification().remediationHints();
     *         }));
     *
     *     // After (v4.1.0) - All unclassified types
     *     model.typeRegistry().ifPresent(reg ->
     *         reg.all(io.hexaglue.arch.model.UnclassifiedType.class).forEach(...));
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public Stream<UnclassifiedType> unclassifiedTypes() {
        return registry.all(UnclassifiedType.class);
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
     * @param aggregateId the aggregate identifier
     * @return the repository, if found
     */
    public Optional<DrivenPort> repositoryFor(ElementId aggregateId) {
        return relationships.repositoryFor(aggregateId).flatMap(id -> registry.get(id, DrivenPort.class));
    }

    /**
     * Returns whether an aggregate has a repository.
     *
     * @param aggregateId the aggregate identifier
     * @return true if a repository exists
     */
    public boolean hasRepository(ElementId aggregateId) {
        return relationships.repositoryFor(aggregateId).isPresent();
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

    /**
     * Returns the number of unclassified types.
     *
     * @return the unclassified count
     * @deprecated Use {@link #classificationReport()} with {@link ClassificationReport#stats()}
     *     for accurate statistics.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     long count = model.unclassifiedCount();
     *
     *     // After (v4.1.0)
     *     model.classificationReport().ifPresent(report -> {
     *         int total = report.stats().unclassifiedCount();
     *         double rate = report.stats().classificationRate();
     *     });
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public long unclassifiedCount() {
        return unclassifiedTypes().count();
    }

    /**
     * Returns whether the model has any unclassified types.
     *
     * @return true if unclassified types exist
     * @deprecated Use {@link #hasClassificationIssues()} instead for a more accurate assessment
     *     that includes conflicts and ambiguous classifications.
     *     <pre>{@code
     *     // Before (v4.0.0)
     *     if (model.hasUnclassified()) { ... }
     *
     *     // After (v4.1.0)
     *     if (model.hasClassificationIssues()) {
     *         model.classificationReport().ifPresent(report ->
     *             report.actionRequired().forEach(...));
     *     }
     *     }</pre>
     * @since 4.0.0
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public boolean hasUnclassified() {
        return unclassifiedTypes().findAny().isPresent();
    }

    // === Query API ===

    /**
     * Returns a fluent query interface for navigating the model.
     *
     * <p>The query API provides immutable, reusable queries with specialized
     * methods for each element type.</p>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * model.query()
     *      .aggregates()
     *      .withRepository()
     *      .forEach(agg -> process(agg));
     * }</pre>
     *
     * @return a new model query
     */
    public ModelQuery query() {
        return new DefaultModelQuery(registry, relationships);
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
