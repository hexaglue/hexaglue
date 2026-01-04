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

package io.hexaglue.core.classification.domain;

/**
 * Domain-Driven Design classification kinds.
 *
 * <p>Based on DDD tactical patterns and jMolecules annotations.
 */
public enum DomainKind {

    /**
     * Aggregate root - the entry point to an aggregate.
     *
     * <p>An aggregate root is an entity that controls access to a cluster
     * of related objects. External objects can only reference the root.
     */
    AGGREGATE_ROOT,

    /**
     * Entity - an object with identity that persists over time.
     *
     * <p>Entities have a unique identity and their equality is based
     * on that identity, not their attributes.
     */
    ENTITY,

    /**
     * Value object - an immutable object without identity.
     *
     * <p>Value objects are equal when all their attributes are equal.
     * They should be immutable and side-effect free.
     */
    VALUE_OBJECT,

    /**
     * Identifier - a value object that represents an entity's identity.
     *
     * <p>Examples: CustomerId, OrderId, ISBN
     */
    IDENTIFIER,

    /**
     * Domain event - represents something that happened in the domain.
     *
     * <p>Events are immutable and named in past tense.
     * Examples: OrderPlaced, CustomerRegistered
     */
    DOMAIN_EVENT,

    /**
     * Domain service - stateless operations that don't belong to entities.
     *
     * <p>Domain services contain domain logic that doesn't naturally
     * fit within an entity or value object.
     */
    DOMAIN_SERVICE
}
