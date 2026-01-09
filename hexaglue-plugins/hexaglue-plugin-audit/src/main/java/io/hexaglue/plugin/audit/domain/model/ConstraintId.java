/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import java.util.Objects;

/**
 * Type-safe identifier for architectural constraints.
 *
 * <p>Constraint IDs follow a hierarchical naming convention:
 * <pre>
 * {category}:{constraint-name}
 *
 * Examples:
 * - ddd:aggregate-repository
 * - ddd:value-object-immutable
 * - hexagonal:dependency-direction
 * </pre>
 *
 * <p>This is an immutable value object with validation.
 *
 * @param value the constraint identifier (must not be blank)
 * @since 1.0.0
 */
public record ConstraintId(String value) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException     if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public ConstraintId {
        Objects.requireNonNull(value, "constraint id required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("constraint id cannot be blank");
        }
    }

    /**
     * Factory method for creating constraint IDs.
     *
     * @param value the constraint identifier
     * @return a new ConstraintId instance
     * @throws NullPointerException     if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public static ConstraintId of(String value) {
        return new ConstraintId(value);
    }

    /**
     * Returns the constraint category (the part before the colon).
     *
     * <p>For example, for "ddd:aggregate-repository", returns "ddd".
     *
     * @return the category, or the full value if no colon is present
     */
    public String category() {
        int colonIndex = value.indexOf(':');
        return colonIndex >= 0 ? value.substring(0, colonIndex) : value;
    }

    /**
     * Returns the constraint name (the part after the colon).
     *
     * <p>For example, for "ddd:aggregate-repository", returns "aggregate-repository".
     *
     * @return the name, or the full value if no colon is present
     */
    public String name() {
        int colonIndex = value.indexOf(':');
        return colonIndex >= 0 ? value.substring(colonIndex + 1) : value;
    }
}
