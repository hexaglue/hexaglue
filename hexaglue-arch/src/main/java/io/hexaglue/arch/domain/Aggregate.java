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

package io.hexaglue.arch.domain;

import io.hexaglue.arch.ArchElement;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ElementRegistry;
import java.util.List;
import java.util.Objects;

/**
 * An Aggregate in Domain-Driven Design.
 *
 * <p>An aggregate defines a consistency boundary - a cluster of domain objects
 * that are treated as a unit for data changes. The aggregate root is the only
 * entry point to the aggregate.</p>
 *
 * <p>This record contains REFERENCES to its components (not the objects themselves)
 * to avoid data duplication. Use the resolve methods to access the actual objects.</p>
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>Aggregate Root - The entry point entity ({@link DomainEntity} with kind AGGREGATE_ROOT)</li>
 *   <li>Internal Entities - Entities within the boundary</li>
 *   <li>Value Objects - Immutable values used by the aggregate</li>
 *   <li>Domain Events - Events published by the aggregate</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Order aggregate contains:
 * // - Order (aggregate root)
 * // - OrderLine (internal entity)
 * // - Money, Address (value objects)
 * // - OrderPlaced (domain event)
 *
 * Aggregate agg = new Aggregate(
 *     ElementId.of("com.example.OrderAggregate"),
 *     ElementRef.of(orderId, DomainEntity.class),
 *     List.of(orderLineRef),
 *     List.of(moneyRef, addressRef),
 *     List.of(orderPlacedRef),
 *     List.of(customerRef),
 *     classificationTrace
 * );
 * }</pre>
 *
 * @param id the unique identifier for this aggregate
 * @param root reference to the aggregate root entity
 * @param internalEntities references to internal entities
 * @param valueObjects references to value objects used
 * @param publishedEvents references to events published by this aggregate
 * @param externalReferences references to other aggregates (by ID type)
 * @param classificationTrace the trace explaining the classification
 * @since 4.0.0
 */
public record Aggregate(
        ElementId id,
        ElementRef<DomainEntity> root,
        List<ElementRef<DomainEntity>> internalEntities,
        List<ElementRef<ValueObject>> valueObjects,
        List<ElementRef<DomainEvent>> publishedEvents,
        List<AggregateReference> externalReferences,
        ClassificationTrace classificationTrace)
        implements ArchElement.Marker {

    /**
     * Creates a new Aggregate instance.
     *
     * @param id the identifier, must not be null
     * @param root reference to the root entity, must not be null
     * @param internalEntities internal entity references, must not be null
     * @param valueObjects value object references, must not be null
     * @param publishedEvents published event references, must not be null
     * @param externalReferences external aggregate references, must not be null
     * @param classificationTrace the trace, must not be null
     * @throws NullPointerException if any required field is null
     */
    public Aggregate {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(internalEntities, "internalEntities must not be null");
        Objects.requireNonNull(valueObjects, "valueObjects must not be null");
        Objects.requireNonNull(publishedEvents, "publishedEvents must not be null");
        Objects.requireNonNull(externalReferences, "externalReferences must not be null");
        Objects.requireNonNull(classificationTrace, "classificationTrace must not be null");
        internalEntities = List.copyOf(internalEntities);
        valueObjects = List.copyOf(valueObjects);
        publishedEvents = List.copyOf(publishedEvents);
        externalReferences = List.copyOf(externalReferences);
    }

    @Override
    public ElementKind kind() {
        return ElementKind.AGGREGATE;
    }

    /**
     * Resolves the aggregate root entity.
     *
     * @param registry the element registry
     * @return the resolved aggregate root
     * @throws io.hexaglue.arch.UnresolvedReferenceException if resolution fails
     */
    public DomainEntity resolveRoot(ElementRegistry registry) {
        return root.resolveOrThrow(registry);
    }

    /**
     * Resolves all internal entities.
     *
     * @param registry the element registry
     * @return list of resolved entities
     * @throws io.hexaglue.arch.UnresolvedReferenceException if any resolution fails
     */
    public List<DomainEntity> resolveEntities(ElementRegistry registry) {
        return internalEntities.stream()
                .map(ref -> ref.resolveOrThrow(registry))
                .toList();
    }

    /**
     * Resolves all value objects.
     *
     * @param registry the element registry
     * @return list of resolved value objects
     * @throws io.hexaglue.arch.UnresolvedReferenceException if any resolution fails
     */
    public List<ValueObject> resolveValueObjects(ElementRegistry registry) {
        return valueObjects.stream().map(ref -> ref.resolveOrThrow(registry)).toList();
    }

    /**
     * Resolves all published events.
     *
     * @param registry the element registry
     * @return list of resolved domain events
     * @throws io.hexaglue.arch.UnresolvedReferenceException if any resolution fails
     */
    public List<DomainEvent> resolveEvents(ElementRegistry registry) {
        return publishedEvents.stream().map(ref -> ref.resolveOrThrow(registry)).toList();
    }

    /**
     * Returns whether this aggregate publishes any events.
     *
     * @return true if events are published
     */
    public boolean publishesEvents() {
        return !publishedEvents.isEmpty();
    }

    /**
     * Returns whether this aggregate references other aggregates.
     *
     * @return true if external references exist
     */
    public boolean hasExternalReferences() {
        return !externalReferences.isEmpty();
    }

    /**
     * Creates a simple Aggregate for testing.
     *
     * @param qualifiedName the fully qualified name
     * @param rootRef reference to the root entity
     * @param trace the classification trace
     * @return a new Aggregate
     */
    public static Aggregate of(String qualifiedName, ElementRef<DomainEntity> rootRef, ClassificationTrace trace) {
        return new Aggregate(ElementId.of(qualifiedName), rootRef, List.of(), List.of(), List.of(), List.of(), trace);
    }
}
