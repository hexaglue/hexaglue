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
 * Evidence based on type structure.
 *
 * <p>Structural evidence captures violations related to the structure of types,
 * such as:
 * <ul>
 *   <li>Missing required fields (e.g., no identity field in entity)</li>
 *   <li>Wrong field types or visibility</li>
 *   <li>Missing annotations</li>
 *   <li>Incorrect class hierarchy</li>
 * </ul>
 *
 * @param description   the evidence description
 * @param involvedTypes the types involved in this evidence
 * @since 1.0.0
 */
public record StructuralEvidence(String description, List<String> involvedTypes) implements Evidence {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public StructuralEvidence {
        Objects.requireNonNull(description, "description required");
        involvedTypes = involvedTypes != null ? List.copyOf(involvedTypes) : List.of();
    }

    /**
     * Factory method for creating structural evidence.
     *
     * @param description   the description
     * @param involvedTypes the involved types
     * @return a new StructuralEvidence instance
     */
    public static StructuralEvidence of(String description, List<String> involvedTypes) {
        return new StructuralEvidence(description, involvedTypes);
    }

    /**
     * Factory method for creating structural evidence with a single involved type.
     *
     * @param description  the description
     * @param involvedType the involved type
     * @return a new StructuralEvidence instance
     */
    public static StructuralEvidence of(String description, String involvedType) {
        return new StructuralEvidence(description, List.of(involvedType));
    }
}
