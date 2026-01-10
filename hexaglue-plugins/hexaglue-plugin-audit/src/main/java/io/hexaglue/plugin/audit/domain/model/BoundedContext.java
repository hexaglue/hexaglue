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

package io.hexaglue.plugin.audit.domain.model;

import io.hexaglue.spi.ir.DomainType;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Bounded Context in Domain-Driven Design.
 *
 * <p>A Bounded Context is a central pattern in DDD that defines explicit boundaries
 * within which a particular domain model is defined and applicable. Each bounded
 * context has its own ubiquitous language and contains cohesive domain types that
 * work together to solve a specific problem space.
 *
 * <p>This immutable value object captures a detected bounded context and its
 * constituent domain types, categorized by their tactical DDD patterns:
 * <ul>
 *   <li>Aggregate Roots - Consistency boundaries and entry points to aggregates</li>
 *   <li>Entities - Objects with identity within the bounded context</li>
 *   <li>Value Objects - Immutable objects defined by their attributes</li>
 *   <li>Domain Events - Records of significant occurrences in the domain</li>
 *   <li>Domain Services - Stateless operations that don't naturally belong to entities</li>
 * </ul>
 *
 * <p><strong>Detection strategy:</strong><br>
 * Bounded contexts are typically detected by analyzing package structure. A package
 * containing at least one aggregate root is considered to represent a bounded context.
 * All domain types within that package and its sub-packages belong to that context.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * BoundedContext orderContext = new BoundedContext(
 *     "order",
 *     List.of(orderAggregate),
 *     List.of(orderLineEntity),
 *     List.of(moneyValueObject),
 *     List.of(orderPlacedEvent),
 *     List.of(pricingService)
 * );
 *
 * System.out.println("Context: " + orderContext.name());
 * System.out.println("Aggregates: " + orderContext.aggregateRoots().size());
 * System.out.println("Total types: " + orderContext.totalTypes());
 * }</pre>
 *
 * @param name           the name of the bounded context (typically derived from package name)
 * @param aggregateRoots the aggregate roots in this context
 * @param entities       the entities (non-root) in this context
 * @param valueObjects   the value objects in this context
 * @param domainEvents   the domain events in this context
 * @param domainServices the domain services in this context
 * @since 1.0.0
 */
public record BoundedContext(
        String name,
        List<DomainType> aggregateRoots,
        List<DomainType> entities,
        List<DomainType> valueObjects,
        List<DomainType> domainEvents,
        List<DomainType> domainServices) {

    /**
     * Compact constructor with validation and defensive copies.
     *
     * @throws NullPointerException if any parameter is null
     */
    public BoundedContext {
        Objects.requireNonNull(name, "name required");
        aggregateRoots = aggregateRoots != null ? List.copyOf(aggregateRoots) : List.of();
        entities = entities != null ? List.copyOf(entities) : List.of();
        valueObjects = valueObjects != null ? List.copyOf(valueObjects) : List.of();
        domainEvents = domainEvents != null ? List.copyOf(domainEvents) : List.of();
        domainServices = domainServices != null ? List.copyOf(domainServices) : List.of();
    }

    /**
     * Creates a bounded context with the given name and aggregate roots.
     *
     * <p>This is a convenience factory method for creating a bounded context
     * with only aggregate roots, which is common when initially detecting contexts.
     *
     * @param name           the context name
     * @param aggregateRoots the aggregate roots
     * @return a new BoundedContext with only aggregate roots populated
     */
    public static BoundedContext withAggregates(String name, List<DomainType> aggregateRoots) {
        return new BoundedContext(name, aggregateRoots, List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Creates an empty bounded context with the given name.
     *
     * @param name the context name
     * @return a new BoundedContext with no domain types
     */
    public static BoundedContext empty(String name) {
        return new BoundedContext(name, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Returns the total number of domain types in this bounded context.
     *
     * @return the sum of all domain type lists
     */
    public int totalTypes() {
        return aggregateRoots.size()
                + entities.size()
                + valueObjects.size()
                + domainEvents.size()
                + domainServices.size();
    }

    /**
     * Returns true if this bounded context contains no domain types.
     *
     * @return true if all domain type lists are empty
     */
    public boolean isEmpty() {
        return totalTypes() == 0;
    }

    /**
     * Returns true if this bounded context has at least one aggregate root.
     *
     * @return true if aggregateRoots is not empty
     */
    public boolean hasAggregates() {
        return !aggregateRoots.isEmpty();
    }

    /**
     * Returns all domain types in this bounded context as a single list.
     *
     * <p>The returned list contains all aggregate roots, entities, value objects,
     * domain events, and domain services in that order.
     *
     * @return a new list containing all domain types
     */
    public List<DomainType> allTypes() {
        return List.of(aggregateRoots, entities, valueObjects, domainEvents, domainServices).stream()
                .flatMap(List::stream)
                .toList();
    }
}
