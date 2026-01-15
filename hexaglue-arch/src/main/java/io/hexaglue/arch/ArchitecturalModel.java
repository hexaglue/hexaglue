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
import io.hexaglue.arch.ports.ApplicationService;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.query.DefaultModelQuery;
import io.hexaglue.arch.query.ModelQuery;
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
 * }</pre>
 *
 * @param project project context information
 * @param registry the element registry containing all classified types
 * @param relationships the relationship store for O(1) lookups
 * @param analysisMetadata metadata about the analysis process
 * @since 4.0.0
 */
public record ArchitecturalModel(
        ProjectContext project,
        ElementRegistry registry,
        RelationshipStore relationships,
        AnalysisMetadata analysisMetadata) {

    /**
     * Creates a new ArchitecturalModel instance.
     *
     * @param project the project context, must not be null
     * @param registry the element registry, must not be null
     * @param relationships the relationship store, must not be null
     * @param analysisMetadata the analysis metadata, must not be null
     * @throws NullPointerException if any field is null
     */
    public ArchitecturalModel {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(relationships, "relationships must not be null");
        Objects.requireNonNull(analysisMetadata, "analysisMetadata must not be null");
    }

    // === Domain Element Access ===

    /**
     * Returns all aggregates in the model.
     *
     * @return stream of aggregates
     */
    public Stream<Aggregate> aggregates() {
        return registry.all(Aggregate.class);
    }

    /**
     * Returns all domain entities in the model.
     *
     * @return stream of domain entities
     */
    public Stream<DomainEntity> domainEntities() {
        return registry.all(DomainEntity.class);
    }

    /**
     * Returns all value objects in the model.
     *
     * @return stream of value objects
     */
    public Stream<ValueObject> valueObjects() {
        return registry.all(ValueObject.class);
    }

    /**
     * Returns all identifiers in the model.
     *
     * @return stream of identifiers
     */
    public Stream<Identifier> identifiers() {
        return registry.all(Identifier.class);
    }

    /**
     * Returns all domain events in the model.
     *
     * @return stream of domain events
     */
    public Stream<DomainEvent> domainEvents() {
        return registry.all(DomainEvent.class);
    }

    /**
     * Returns all domain services in the model.
     *
     * @return stream of domain services
     */
    public Stream<DomainService> domainServices() {
        return registry.all(DomainService.class);
    }

    // === Port Access ===

    /**
     * Returns all driving ports in the model.
     *
     * @return stream of driving ports
     */
    public Stream<DrivingPort> drivingPorts() {
        return registry.all(DrivingPort.class);
    }

    /**
     * Returns all driven ports in the model.
     *
     * @return stream of driven ports
     */
    public Stream<DrivenPort> drivenPorts() {
        return registry.all(DrivenPort.class);
    }

    /**
     * Returns all application services in the model.
     *
     * @return stream of application services
     */
    public Stream<ApplicationService> applicationServices() {
        return registry.all(ApplicationService.class);
    }

    // === Adapter Access ===

    /**
     * Returns all driving adapters in the model.
     *
     * @return stream of driving adapters
     */
    public Stream<DrivingAdapter> drivingAdapters() {
        return registry.all(DrivingAdapter.class);
    }

    /**
     * Returns all driven adapters in the model.
     *
     * @return stream of driven adapters
     */
    public Stream<DrivenAdapter> drivenAdapters() {
        return registry.all(DrivenAdapter.class);
    }

    /**
     * Returns all unclassified types in the model.
     *
     * @return stream of unclassified types
     */
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
     */
    public long unclassifiedCount() {
        return unclassifiedTypes().count();
    }

    /**
     * Returns whether the model has any unclassified types.
     *
     * @return true if unclassified types exist
     */
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
         * Builds the model with the specified metadata.
         *
         * @param metadata the analysis metadata
         * @return the built ArchitecturalModel
         */
        public ArchitecturalModel build(AnalysisMetadata metadata) {
            return new ArchitecturalModel(project, registryBuilder.build(), relationshipsBuilder.build(), metadata);
        }

        /**
         * Builds the model with auto-generated metadata.
         *
         * @return the built ArchitecturalModel
         */
        public ArchitecturalModel build() {
            ElementRegistry registry = registryBuilder.build();
            return new ArchitecturalModel(
                    project, registry, relationshipsBuilder.build(), AnalysisMetadata.forTesting(registry.size()));
        }
    }
}
