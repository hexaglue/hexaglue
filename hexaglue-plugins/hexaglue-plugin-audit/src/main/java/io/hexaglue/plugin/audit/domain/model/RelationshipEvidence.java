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

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Evidence based on relationships between types.
 *
 * <p>Relationship evidence captures violations related to dependencies
 * and relationships between types, such as:
 * <ul>
 *   <li>Circular dependencies between aggregates</li>
 *   <li>Wrong dependency direction (domain depending on infrastructure)</li>
 *   <li>Missing required relationships (aggregate without repository)</li>
 *   <li>Cross-layer violations</li>
 * </ul>
 *
 * @param description    the evidence description
 * @param involvedTypes  the types involved in this evidence
 * @param relationships  the specific relationships involved (e.g., "Order -> Customer -> Order")
 * @since 1.0.0
 */
public record RelationshipEvidence(String description, List<String> involvedTypes, List<String> relationships)
        implements Evidence {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public RelationshipEvidence {
        Objects.requireNonNull(description, "description required");
        involvedTypes = involvedTypes != null ? List.copyOf(involvedTypes) : List.of();
        relationships = relationships != null ? List.copyOf(relationships) : List.of();
    }

    /**
     * Factory method for creating relationship evidence.
     *
     * @param description    the description
     * @param involvedTypes  the involved types
     * @param relationships  the specific relationships
     * @return a new RelationshipEvidence instance
     */
    public static RelationshipEvidence of(String description, List<String> involvedTypes, List<String> relationships) {
        return new RelationshipEvidence(description, involvedTypes, relationships);
    }

    /**
     * Factory method for creating relationship evidence without explicit relationship details.
     *
     * @param description   the description
     * @param involvedTypes the involved types
     * @return a new RelationshipEvidence instance
     */
    public static RelationshipEvidence of(String description, List<String> involvedTypes) {
        return new RelationshipEvidence(description, involvedTypes, List.of());
    }
}
