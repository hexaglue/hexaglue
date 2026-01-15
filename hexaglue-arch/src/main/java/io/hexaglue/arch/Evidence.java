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

import io.hexaglue.syntax.SourceLocation;
import java.util.Objects;
import java.util.Optional;

/**
 * Evidence supporting a classification decision.
 *
 * <p>Provides detailed information about what was found during analysis
 * that supports a particular classification.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * Evidence evidence = new Evidence(
 *     EvidenceType.ANNOTATION,
 *     "@AggregateRoot annotation found",
 *     Optional.of(SourceLocation.at(Path.of("Order.java"), 10, 1))
 * );
 * }</pre>
 *
 * @param type the type of evidence
 * @param description human-readable description of what was found
 * @param location optional source location where the evidence was found
 * @since 4.0.0
 */
public record Evidence(EvidenceType type, String description, Optional<SourceLocation> location) {

    /**
     * Creates a new Evidence instance.
     *
     * @param type the type of evidence, must not be null
     * @param description the description, must not be null or blank
     * @param location the optional source location
     * @throws NullPointerException if type, description, or location is null
     * @throws IllegalArgumentException if description is blank
     */
    public Evidence {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(location, "location must not be null (use Optional.empty())");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    /**
     * Creates evidence without a source location.
     *
     * @param type the type of evidence
     * @param description the description
     * @return a new Evidence instance
     */
    public static Evidence of(EvidenceType type, String description) {
        return new Evidence(type, description, Optional.empty());
    }

    /**
     * Creates evidence with a source location.
     *
     * @param type the type of evidence
     * @param description the description
     * @param location the source location
     * @return a new Evidence instance
     */
    public static Evidence at(EvidenceType type, String description, SourceLocation location) {
        return new Evidence(type, description, Optional.of(location));
    }
}
