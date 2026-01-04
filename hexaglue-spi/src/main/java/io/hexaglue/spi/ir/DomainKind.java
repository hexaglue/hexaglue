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

package io.hexaglue.spi.ir;

/**
 * Classification of domain types according to DDD tactical patterns.
 */
public enum DomainKind {

    /**
     * An aggregate root - the entry point to an aggregate.
     * Has identity and manages its invariants.
     */
    AGGREGATE_ROOT,

    /**
     * An entity within an aggregate (not the root).
     * Has identity but is accessed through its aggregate root.
     */
    ENTITY,

    /**
     * A value object - immutable, no identity, defined by its attributes.
     */
    VALUE_OBJECT,

    /**
     * An identifier type - wraps a primitive identity value.
     */
    IDENTIFIER,

    /**
     * A domain event - immutable record of something that happened.
     */
    DOMAIN_EVENT,

    /**
     * A domain service - stateless operation that doesn't belong to an entity.
     */
    DOMAIN_SERVICE
}
