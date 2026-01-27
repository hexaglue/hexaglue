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

package io.hexaglue.arch.model.ir;

/**
 * Strategy for identity generation.
 *
 * <p>Combines conceptual strategies (NATURAL, SURROGATE) with JPA-specific
 * generation strategies for more precise code generation.
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.ir
 */
public enum IdentityStrategy {

    // === Conceptual strategies ===

    /**
     * Natural key - the identity has business meaning (e.g., ISBN, SSN).
     * Typically assigned by the application.
     */
    NATURAL,

    /**
     * Surrogate key - the identity is generated and has no business meaning.
     * This is a generic surrogate; use specific strategies when known.
     */
    SURROGATE,

    // === JPA generation strategies ===

    /**
     * JPA AUTO strategy - the persistence provider picks the strategy.
     * Maps to {@code @GeneratedValue(strategy = GenerationType.AUTO)}.
     */
    AUTO,

    /**
     * JPA IDENTITY strategy - uses database identity column.
     * Maps to {@code @GeneratedValue(strategy = GenerationType.IDENTITY)}.
     * Common with MySQL, SQL Server.
     */
    IDENTITY,

    /**
     * JPA SEQUENCE strategy - uses database sequence.
     * Maps to {@code @GeneratedValue(strategy = GenerationType.SEQUENCE)}.
     * Common with PostgreSQL, Oracle.
     */
    SEQUENCE,

    /**
     * JPA TABLE strategy - uses a separate table for ID generation.
     * Maps to {@code @GeneratedValue(strategy = GenerationType.TABLE)}.
     */
    TABLE,

    /**
     * UUID strategy - application generates UUID.
     * Maps to {@code @GeneratedValue(generator = "uuid")} or application-level generation.
     */
    UUID,

    /**
     * Assigned strategy - application provides the ID value.
     * No {@code @GeneratedValue} annotation needed.
     */
    ASSIGNED,

    /**
     * Composite strategy - multi-property identity (e.g., composite keys).
     * Maps to {@code @EmbeddedId} or {@code @IdClass} in JPA.
     * Used when the identity is composed of multiple fields.
     */
    COMPOSITE,

    /**
     * Unknown strategy - could not be determined.
     */
    UNKNOWN;

    /**
     * Returns true if this strategy requires {@code @GeneratedValue} annotation.
     */
    public boolean requiresGeneratedValue() {
        return switch (this) {
            case AUTO, IDENTITY, SEQUENCE, TABLE, UUID -> true;
            case NATURAL, SURROGATE, ASSIGNED, COMPOSITE, UNKNOWN -> false;
        };
    }

    /**
     * Returns true if this is a JPA-specific generation strategy.
     */
    public boolean isJpaStrategy() {
        return switch (this) {
            case AUTO, IDENTITY, SEQUENCE, TABLE -> true;
            default -> false;
        };
    }

    /**
     * Returns true if the ID is generated (not assigned by application).
     */
    public boolean isGenerated() {
        return switch (this) {
            case AUTO, IDENTITY, SEQUENCE, TABLE, UUID, SURROGATE -> true;
            case NATURAL, ASSIGNED, COMPOSITE, UNKNOWN -> false;
        };
    }

    /**
     * Maps to the JPA GenerationType name, or null if not applicable.
     */
    public String toJpaGenerationType() {
        return switch (this) {
            case AUTO -> "AUTO";
            case IDENTITY -> "IDENTITY";
            case SEQUENCE -> "SEQUENCE";
            case TABLE -> "TABLE";
            case UUID -> "UUID"; // JPA 3.1+
            default -> null;
        };
    }
}
