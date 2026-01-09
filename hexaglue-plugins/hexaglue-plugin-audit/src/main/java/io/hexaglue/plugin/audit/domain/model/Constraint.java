/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Architectural constraint to be validated.
 *
 * <p>A constraint represents a rule that the architecture must satisfy.
 * Each constraint has an identifier, name, description, default severity,
 * and optional tags for categorization.
 *
 * <p>This is an immutable value object.
 *
 * @param id              the constraint identifier (must not be null)
 * @param name            the human-readable constraint name (must not be null)
 * @param description     the detailed description (empty string if not provided)
 * @param defaultSeverity the default severity level (must not be null)
 * @param tags            optional tags for categorization (defensive copy made)
 * @since 1.0.0
 */
public record Constraint(
        ConstraintId id,
        String name,
        String description,
        Severity defaultSeverity,
        List<String> tags) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public Constraint {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(name, "name required");
        Objects.requireNonNull(description, "description required");
        Objects.requireNonNull(defaultSeverity, "defaultSeverity required");
        tags = tags != null ? List.copyOf(tags) : List.of();
    }

    /**
     * Factory method for creating DDD constraints.
     *
     * @param id       the constraint ID (e.g., "ddd:aggregate-repository")
     * @param name     the constraint name
     * @param severity the default severity
     * @return a new Constraint with "ddd" tag
     */
    public static Constraint ddd(String id, String name, Severity severity) {
        return new Constraint(ConstraintId.of(id), name, "", severity, List.of("ddd"));
    }

    /**
     * Factory method for creating DDD constraints with description.
     *
     * @param id          the constraint ID
     * @param name        the constraint name
     * @param description the detailed description
     * @param severity    the default severity
     * @return a new Constraint with "ddd" tag
     */
    public static Constraint ddd(String id, String name, String description, Severity severity) {
        return new Constraint(ConstraintId.of(id), name, description, severity, List.of("ddd"));
    }

    /**
     * Factory method for creating hexagonal architecture constraints.
     *
     * @param id       the constraint ID (e.g., "hexagonal:dependency-direction")
     * @param name     the constraint name
     * @param severity the default severity
     * @return a new Constraint with "hexagonal" tag
     */
    public static Constraint hexagonal(String id, String name, Severity severity) {
        return new Constraint(ConstraintId.of(id), name, "", severity, List.of("hexagonal"));
    }

    /**
     * Factory method for creating hexagonal architecture constraints with description.
     *
     * @param id          the constraint ID
     * @param name        the constraint name
     * @param description the detailed description
     * @param severity    the default severity
     * @return a new Constraint with "hexagonal" tag
     */
    public static Constraint hexagonal(String id, String name, String description, Severity severity) {
        return new Constraint(ConstraintId.of(id), name, description, severity, List.of("hexagonal"));
    }

    /**
     * Factory method for creating custom constraints.
     *
     * @param id       the constraint ID
     * @param name     the constraint name
     * @param severity the default severity
     * @return a new Constraint with "custom" tag
     */
    public static Constraint custom(String id, String name, Severity severity) {
        return new Constraint(ConstraintId.of(id), name, "", severity, List.of("custom"));
    }
}
