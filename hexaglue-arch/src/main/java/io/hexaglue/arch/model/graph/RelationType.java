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

package io.hexaglue.arch.model.graph;

/**
 * Types of relationships between architectural elements in the domain model.
 *
 * <p>These relationship types capture the semantic connections between types
 * in a hexagonal architecture, enabling navigation, analysis, and visualization
 * of the architectural structure.</p>
 *
 * <h2>Relationship Categories</h2>
 * <ul>
 *   <li><strong>Compositional</strong>: {@link #CONTAINS}, {@link #OWNS}</li>
 *   <li><strong>Reference</strong>: {@link #REFERENCES}, {@link #DEPENDS_ON}</li>
 *   <li><strong>Inheritance</strong>: {@link #EXTENDS}, {@link #IMPLEMENTS}</li>
 *   <li><strong>Port/Adapter</strong>: {@link #EXPOSES}, {@link #ADAPTS}</li>
 *   <li><strong>Domain Events</strong>: {@link #EMITS}, {@link #HANDLES}</li>
 *   <li><strong>Persistence</strong>: {@link #PERSISTS}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Check relationship type
 * Relationship rel = graph.from(aggregateId).findFirst().get();
 * if (rel.type().isCompositional()) {
 *     // Handle contained/owned entities
 * }
 *
 * // Filter by type
 * graph.from(aggregateId)
 *     .filter(r -> r.type() == RelationType.EMITS)
 *     .forEach(r -> System.out.println("Emits: " + r.target()));
 * }</pre>
 *
 * @since 5.0.0
 */
public enum RelationType {

    /**
     * Aggregate contains an Entity or ValueObject within its boundary.
     *
     * <p>This represents the DDD aggregate composition relationship where
     * entities and value objects exist within the aggregate's consistency boundary.</p>
     *
     * <p>Example: {@code Order} CONTAINS {@code OrderLine}</p>
     */
    CONTAINS,

    /**
     * Type owns another type (stronger than CONTAINS, implies exclusive ownership).
     *
     * <p>The owned type cannot exist independently of the owner and has no
     * identity outside the owning context.</p>
     *
     * <p>Example: {@code Order} OWNS {@code OrderId}</p>
     */
    OWNS,

    /**
     * Type holds a reference to another aggregate or type.
     *
     * <p>This is a soft reference that doesn't imply containment. Typically
     * references between aggregates are by ID rather than by object reference.</p>
     *
     * <p>Example: {@code Order} REFERENCES {@code Customer} (via customerId)</p>
     */
    REFERENCES,

    /**
     * Type depends on another type (general dependency).
     *
     * <p>This represents a usage dependency where one type uses another
     * in method parameters, return types, or field types.</p>
     *
     * <p>Example: {@code OrderService} DEPENDS_ON {@code PricingService}</p>
     */
    DEPENDS_ON,

    /**
     * Class extends another class.
     *
     * <p>Standard Java class inheritance relationship.</p>
     *
     * <p>Example: {@code PremiumOrder} EXTENDS {@code Order}</p>
     */
    EXTENDS,

    /**
     * Type implements an interface.
     *
     * <p>Standard Java interface implementation relationship.</p>
     *
     * <p>Example: {@code OrderServiceImpl} IMPLEMENTS {@code OrderService}</p>
     */
    IMPLEMENTS,

    /**
     * Driving port exposes use cases to external actors.
     *
     * <p>This represents the relationship between a driving port and the
     * application service that implements it.</p>
     *
     * <p>Example: {@code OrderUseCase} EXPOSES {@code CreateOrderCommand}</p>
     */
    EXPOSES,

    /**
     * Adapter implements a port (connects infrastructure to domain).
     *
     * <p>This represents the relationship between an adapter (infrastructure)
     * and the port (interface) it implements.</p>
     *
     * <p>Example: {@code JpaOrderRepository} ADAPTS {@code OrderRepository}</p>
     */
    ADAPTS,

    /**
     * Repository persists an aggregate.
     *
     * <p>This represents the relationship between a repository port and
     * the aggregate root it manages.</p>
     *
     * <p>Example: {@code OrderRepository} PERSISTS {@code Order}</p>
     */
    PERSISTS,

    /**
     * Aggregate emits a domain event.
     *
     * <p>This represents the relationship between an aggregate and the
     * domain events it can publish.</p>
     *
     * <p>Example: {@code Order} EMITS {@code OrderCreatedEvent}</p>
     */
    EMITS,

    /**
     * Event handler handles a domain event.
     *
     * <p>This represents the relationship between an event handler and
     * the domain event it processes.</p>
     *
     * <p>Example: {@code OrderEventHandler} HANDLES {@code OrderCreatedEvent}</p>
     */
    HANDLES;

    /**
     * Returns whether this relationship type represents composition.
     *
     * <p>Compositional relationships imply that the target exists within
     * the context of the source.</p>
     *
     * @return true if CONTAINS or OWNS
     */
    public boolean isCompositional() {
        return this == CONTAINS || this == OWNS;
    }

    /**
     * Returns whether this relationship type represents a reference.
     *
     * <p>Reference relationships are navigational links without ownership.</p>
     *
     * @return true if REFERENCES or DEPENDS_ON
     */
    public boolean isReference() {
        return this == REFERENCES || this == DEPENDS_ON;
    }

    /**
     * Returns whether this relationship type represents inheritance.
     *
     * @return true if EXTENDS or IMPLEMENTS
     */
    public boolean isInheritance() {
        return this == EXTENDS || this == IMPLEMENTS;
    }

    /**
     * Returns whether this relationship type is related to hexagonal ports.
     *
     * @return true if EXPOSES, ADAPTS, or PERSISTS
     */
    public boolean isPortRelated() {
        return this == EXPOSES || this == ADAPTS || this == PERSISTS;
    }

    /**
     * Returns whether this relationship type is related to domain events.
     *
     * @return true if EMITS or HANDLES
     */
    public boolean isEventRelated() {
        return this == EMITS || this == HANDLES;
    }

    /**
     * Returns whether this relationship implies the source owns/controls the target.
     *
     * @return true if the source has ownership semantics over the target
     */
    public boolean impliesOwnership() {
        return this == CONTAINS || this == OWNS || this == PERSISTS;
    }
}
