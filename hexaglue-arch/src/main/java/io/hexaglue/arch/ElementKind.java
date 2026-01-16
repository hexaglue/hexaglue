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

/**
 * Types of architectural elements in a DDD/Hexagonal Architecture application.
 *
 * <h2>Design Note (v2.1)</h2>
 * <p>This enum is used for TYPING elements, but is NOT part of the {@link ElementId}.
 * The kind is obtained via {@code element.kind()} or
 * {@code element.classificationTrace().classifiedAs()}.</p>
 *
 * <h2>Categories</h2>
 * <ul>
 *   <li><strong>Domain</strong>: Core business logic elements (Aggregates, Entities, Value Objects, etc.)</li>
 *   <li><strong>Ports</strong>: Interfaces defining the application boundary</li>
 *   <li><strong>Adapters</strong>: Implementations connecting to infrastructure</li>
 * </ul>
 *
 * @since 4.0.0
 */
public enum ElementKind {

    // ===== Domain =====

    /**
     * An aggregate - a cluster of domain objects treated as a single unit.
     */
    AGGREGATE,

    /**
     * An aggregate root - the entry point to an aggregate that ensures consistency.
     */
    AGGREGATE_ROOT,

    /**
     * A domain entity - has identity and lifecycle.
     */
    ENTITY,

    /**
     * A value object - immutable, identified by its attributes.
     */
    VALUE_OBJECT,

    /**
     * An identifier - a value object that represents an entity's identity.
     */
    IDENTIFIER,

    /**
     * A domain event - represents something that happened in the domain.
     */
    DOMAIN_EVENT,

    /**
     * An externalized event - a domain event marked for external publication.
     *
     * <p>Externalized events are domain events that should be published
     * outside the bounded context, typically through a message broker.
     * Detected via @Externalized annotation (jMolecules).</p>
     *
     * @since 4.1.0
     */
    EXTERNALIZED_EVENT,

    /**
     * A domain service - stateless operation that doesn't naturally fit in an entity or value object.
     */
    DOMAIN_SERVICE,

    // ===== Ports =====

    /**
     * A driving port (primary/inbound) - interface exposed to the outside world.
     */
    DRIVING_PORT,

    /**
     * A driven port (secondary/outbound) - interface for external dependencies.
     */
    DRIVEN_PORT,

    /**
     * An application service - orchestrates use cases using domain and ports.
     *
     * <p>Application services have dependencies on DRIVEN ports (infrastructure
     * interfaces) and expose operations that can be called by DRIVING ports.
     */
    APPLICATION_SERVICE,

    /**
     * Inbound-only actor - implements driving port(s) but has no driven port dependencies.
     *
     * <p>Typically a query handler or simple command handler that only exposes
     * operations without requiring infrastructure calls.
     *
     * @since 4.0.0
     */
    INBOUND_ONLY,

    /**
     * Outbound-only actor - depends on driven port(s) but implements no driving port.
     *
     * <p>Typically a background processor, event handler, or scheduled task
     * that calls infrastructure but is not directly exposed as a use case.
     *
     * @since 4.0.0
     */
    OUTBOUND_ONLY,

    /**
     * Saga - a long-running process that coordinates multiple driven ports.
     *
     * <p>A saga is an outbound-only actor that depends on 2+ driven ports
     * and maintains state fields for tracking progress. Sagas implement
     * the Process Manager pattern from DDD.
     *
     * @since 4.0.0
     */
    SAGA,

    // ===== Adapters =====

    /**
     * A driving adapter (primary/inbound) - implements driving ports (e.g., REST controllers).
     */
    DRIVING_ADAPTER,

    /**
     * A driven adapter (secondary/outbound) - implements driven ports (e.g., JPA repositories).
     */
    DRIVEN_ADAPTER,

    // ===== Fallback =====

    /**
     * An unclassified type - could not be classified with sufficient confidence.
     */
    UNCLASSIFIED;

    /**
     * Returns {@code true} if this is a port type (DRIVING_PORT or DRIVEN_PORT).
     *
     * <p>Note: Application services and their subtypes are NOT considered ports,
     * even though they interact with ports. Use {@link #isApplicationService()}
     * to check for application-layer services.</p>
     *
     * @return true if this element is a port
     */
    public boolean isPort() {
        return this == DRIVING_PORT || this == DRIVEN_PORT;
    }

    /**
     * Returns {@code true} if this is an application service type.
     *
     * <p>Application service types include: APPLICATION_SERVICE, INBOUND_ONLY,
     * OUTBOUND_ONLY, and SAGA. These are use case orchestrators that coordinate
     * domain logic and infrastructure through ports.</p>
     *
     * @return true if this element is an application service
     * @since 4.0.0
     */
    public boolean isApplicationService() {
        return this == APPLICATION_SERVICE || this == INBOUND_ONLY || this == OUTBOUND_ONLY || this == SAGA;
    }

    /**
     * Returns {@code true} if this is an adapter type (DRIVING_ADAPTER or DRIVEN_ADAPTER).
     *
     * @return true if this element is an adapter
     */
    public boolean isAdapter() {
        return this == DRIVING_ADAPTER || this == DRIVEN_ADAPTER;
    }

    /**
     * Returns {@code true} if this is a domain type.
     *
     * <p>Domain types include: AGGREGATE, AGGREGATE_ROOT, ENTITY, VALUE_OBJECT,
     * IDENTIFIER, DOMAIN_EVENT, DOMAIN_SERVICE.</p>
     *
     * @return true if this element is a domain element
     */
    public boolean isDomain() {
        return !isPort() && !isAdapter() && !isApplicationService() && this != UNCLASSIFIED;
    }
}
