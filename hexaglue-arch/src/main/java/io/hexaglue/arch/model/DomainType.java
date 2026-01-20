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

package io.hexaglue.arch.model;

/**
 * Sealed interface for domain types in the architectural model.
 *
 * <p>Domain types represent the core business concepts in a DDD application.
 * They are the building blocks of the domain model and include:</p>
 * <ul>
 *   <li>{@link AggregateRoot} - Entry point to an aggregate, ensures consistency</li>
 *   <li>{@link Entity} - Has identity and lifecycle</li>
 *   <li>{@link ValueObject} - Immutable, identified by its attributes</li>
 *   <li>{@link Identifier} - Value object representing an entity's identity</li>
 *   <li>{@link DomainEvent} - Represents something that happened in the domain</li>
 *   <li>{@link DomainService} - Stateless domain operation</li>
 * </ul>
 *
 * <h2>Pattern Matching</h2>
 * <p>Because this is a sealed interface, you can use exhaustive pattern matching:</p>
 * <pre>{@code
 * if (domainType instanceof AggregateRoot agg) {
 *     // handle aggregate root
 * } else if (domainType instanceof Entity entity) {
 *     // handle entity
 * }
 * }</pre>
 *
 * @since 4.1.0
 */
public sealed interface DomainType extends ArchType
        permits AggregateRoot, Entity, ValueObject, Identifier, DomainEvent, DomainService, DomainType.Marker {

    /**
     * Temporary marker interface for testing and migration.
     *
     * <p>This allows creating test implementations of DomainType.
     * The concrete records (AggregateRoot, Entity, etc.) are the primary implementations.</p>
     *
     * @since 4.1.0
     * @deprecated Use concrete record types instead
     */
    @Deprecated(forRemoval = true)
    non-sealed interface Marker extends DomainType {}
}
