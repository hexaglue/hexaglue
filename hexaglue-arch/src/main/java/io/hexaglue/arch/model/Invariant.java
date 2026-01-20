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

import java.util.Objects;

/**
 * Represents a business invariant on an aggregate root.
 *
 * <p>An invariant is a business rule that must always be true for an aggregate
 * to be in a valid state. Invariants are identified during classification and
 * stored in the {@link AggregateRoot} type.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Invariant inv = Invariant.of(
 *     "orderMustHaveItems",
 *     "An order must have at least one item"
 * );
 * }</pre>
 *
 * @param name the invariant name (typically a camelCase identifier)
 * @param description a human-readable description of the invariant
 * @since 4.1.0
 */
public record Invariant(String name, String description) {

    /**
     * Creates a new Invariant.
     *
     * @param name the invariant name, must not be null or blank
     * @param description the invariant description, must not be null or blank
     * @throws NullPointerException if name or description is null
     * @throws IllegalArgumentException if name or description is blank
     */
    public Invariant {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    /**
     * Creates an Invariant with the given name and description.
     *
     * @param name the invariant name (typically a camelCase identifier)
     * @param description a human-readable description of the invariant
     * @return a new Invariant
     * @throws NullPointerException if name or description is null
     * @throws IllegalArgumentException if name or description is blank
     */
    public static Invariant of(String name, String description) {
        return new Invariant(name, description);
    }
}
