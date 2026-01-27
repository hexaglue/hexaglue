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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * A relationship between two architectural components.
 *
 * @param from source component name
 * @param to target component name
 * @param type relationship type (e.g., "orchestrates", "persists-via", "references", "uses", "implemented-by")
 * @param isCycle whether this relationship is part of a cycle
 * @since 5.0.0
 */
public record Relationship(
        String from,
        String to,
        String type,
        boolean isCycle) {

    /**
     * Creates a relationship with validation.
     */
    public Relationship {
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");
        Objects.requireNonNull(type, "type is required");
    }

    /**
     * Creates a relationship that is not part of a cycle.
     *
     * @param from source
     * @param to target
     * @param type relationship type
     * @return the relationship
     */
    public static Relationship of(String from, String to, String type) {
        return new Relationship(from, to, type, false);
    }

    /**
     * Creates a relationship that is part of a cycle.
     *
     * @param from source
     * @param to target
     * @param type relationship type
     * @return the relationship
     */
    public static Relationship cycle(String from, String to, String type) {
        return new Relationship(from, to, type, true);
    }
}
