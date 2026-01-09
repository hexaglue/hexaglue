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
 * Evidence based on method behavior.
 *
 * <p>Behavioral evidence captures violations related to method implementations
 * and behavior, such as:
 * <ul>
 *   <li>Setter methods in value objects (indicates mutability)</li>
 *   <li>Missing or incorrect business logic</li>
 *   <li>Side effects in methods that should be pure</li>
 *   <li>High complexity methods</li>
 * </ul>
 *
 * @param description   the evidence description
 * @param involvedTypes the types involved in this evidence
 * @param methodName    the method name where the issue occurs (optional)
 * @since 1.0.0
 */
public record BehavioralEvidence(String description, List<String> involvedTypes, String methodName)
        implements Evidence {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public BehavioralEvidence {
        Objects.requireNonNull(description, "description required");
        involvedTypes = involvedTypes != null ? List.copyOf(involvedTypes) : List.of();
    }

    /**
     * Factory method for creating behavioral evidence without method name.
     *
     * @param description   the description
     * @param involvedTypes the involved types
     * @return a new BehavioralEvidence instance
     */
    public static BehavioralEvidence of(String description, List<String> involvedTypes) {
        return new BehavioralEvidence(description, involvedTypes, null);
    }

    /**
     * Factory method for creating behavioral evidence with method name.
     *
     * @param description  the description
     * @param involvedType the involved type
     * @param methodName   the method name
     * @return a new BehavioralEvidence instance
     */
    public static BehavioralEvidence of(String description, String involvedType, String methodName) {
        return new BehavioralEvidence(description, List.of(involvedType), methodName);
    }
}
