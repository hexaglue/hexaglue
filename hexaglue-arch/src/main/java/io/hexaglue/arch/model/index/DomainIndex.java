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

package io.hexaglue.arch.model.index;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Index for domain types providing convenient access methods.
 *
 * <p>The DomainIndex provides typed access to domain types (aggregate roots,
 * entities, value objects, identifiers, domain events, and domain services)
 * and supports querying relationships between them.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DomainIndex domain = model.domainIndex();
 *
 * // Iterate over aggregate roots
 * domain.aggregateRoots().forEach(agg -> {
 *     System.out.println("Aggregate: " + agg.simpleName());
 *
 *     // Get entities within aggregate
 *     domain.entitiesOf(agg).forEach(entity ->
 *         System.out.println("  Entity: " + entity.simpleName()));
 *
 *     // Get value objects within aggregate
 *     domain.valueObjectsOf(agg).forEach(vo ->
 *         System.out.println("  Value Object: " + vo.simpleName()));
 * });
 *
 * // Get specific aggregate
 * domain.aggregateRoot(TypeId.of("com.example.Order"))
 *     .ifPresent(order -> processOrder(order));
 * }</pre>
 *
 * @since 4.1.0
 */
public final class DomainIndex {

    private final TypeRegistry registry;

    private DomainIndex(TypeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Creates a DomainIndex from a TypeRegistry.
     *
     * @param registry the type registry
     * @return a new DomainIndex
     * @throws NullPointerException if registry is null
     */
    public static DomainIndex from(TypeRegistry registry) {
        return new DomainIndex(registry);
    }

    /**
     * Returns a stream of all aggregate roots.
     *
     * @return stream of aggregate roots
     */
    public Stream<AggregateRoot> aggregateRoots() {
        return registry.all(AggregateRoot.class);
    }

    /**
     * Returns a stream of all entities.
     *
     * @return stream of entities
     */
    public Stream<Entity> entities() {
        return registry.all(Entity.class);
    }

    /**
     * Returns a stream of all value objects.
     *
     * @return stream of value objects
     */
    public Stream<ValueObject> valueObjects() {
        return registry.all(ValueObject.class);
    }

    /**
     * Returns a stream of all identifiers.
     *
     * @return stream of identifiers
     */
    public Stream<Identifier> identifiers() {
        return registry.all(Identifier.class);
    }

    /**
     * Returns a stream of all domain events.
     *
     * @return stream of domain events
     */
    public Stream<DomainEvent> domainEvents() {
        return registry.all(DomainEvent.class);
    }

    /**
     * Returns a stream of all domain services.
     *
     * @return stream of domain services
     */
    public Stream<DomainService> domainServices() {
        return registry.all(DomainService.class);
    }

    /**
     * Returns the aggregate root with the given id.
     *
     * @param id the type id
     * @return an optional containing the aggregate root, or empty if not found or wrong type
     */
    public Optional<AggregateRoot> aggregateRoot(TypeId id) {
        return registry.get(id, AggregateRoot.class);
    }

    /**
     * Returns the entities that are part of the given aggregate root.
     *
     * <p>Looks up the entity references stored in the aggregate root's
     * {@link AggregateRoot#entities()} list.</p>
     *
     * @param aggregate the aggregate root
     * @return list of entities within the aggregate
     */
    public List<Entity> entitiesOf(AggregateRoot aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        return aggregate.entities().stream()
                .map(this::lookupEntity)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Returns the value objects that are part of the given aggregate root.
     *
     * <p>Looks up the value object references stored in the aggregate root's
     * {@link AggregateRoot#valueObjects()} list.</p>
     *
     * @param aggregate the aggregate root
     * @return list of value objects within the aggregate
     */
    public List<ValueObject> valueObjectsOf(AggregateRoot aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        return aggregate.valueObjects().stream()
                .map(this::lookupValueObject)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Entity> lookupEntity(TypeRef ref) {
        return registry.get(TypeId.of(ref.qualifiedName()), Entity.class);
    }

    private Optional<ValueObject> lookupValueObject(TypeRef ref) {
        return registry.get(TypeId.of(ref.qualifiedName()), ValueObject.class);
    }
}
