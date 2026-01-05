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
     * fit within an entity or value object. They have NO dependencies
     * on ports (infrastructure interfaces).
     */
    DOMAIN_SERVICE,

    /**
     * Application service - orchestrates use cases by coordinating
     * domain logic and infrastructure through ports.
     *
     * <p>Application services have dependencies on DRIVEN ports (infrastructure
     * interfaces) and expose operations that can be called by DRIVING ports.
     * They orchestrate but contain no domain logic themselves.
     *
     * <p>Key distinction from DOMAIN_SERVICE: has port dependencies.
     */
    APPLICATION_SERVICE,

    /**
     * Inbound-only actor - implements driving port(s) but has no driven port dependencies.
     *
     * <p>Typically a query handler or simple command handler that only exposes
     * operations without requiring infrastructure calls.
     *
     * <p>Classification: implements(Driving), no Driven dependencies.
     */
    INBOUND_ONLY,

    /**
     * Outbound-only actor - depends on driven port(s) but implements no driving port.
     *
     * <p>Typically a background processor, event handler, or scheduled task
     * that calls infrastructure but is not directly exposed as a use case.
     *
     * <p>Classification: depends(Driven), no Driving implementation.
     */
    OUTBOUND_ONLY,

    /**
     * Saga - a long-running process that coordinates multiple driven ports.
     *
     * <p>A saga is an outbound-only actor that depends on 2+ driven ports
     * and maintains state fields for tracking progress.
     *
     * <p>Classification: OUTBOUND_ONLY + ≥2 driven dependencies + state fields.
     */
    SAGA
}
