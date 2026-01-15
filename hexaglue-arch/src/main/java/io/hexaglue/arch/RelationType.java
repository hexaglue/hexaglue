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

/**
 * Types of relationships between architectural elements.
 *
 * <p>Used by {@link RelationshipStore} to categorize and index relationships
 * for O(1) lookups.</p>
 *
 * @since 4.0.0
 */
public enum RelationType {

    /**
     * Type implements interface (class → interface).
     */
    IMPLEMENTS,

    /**
     * Type extends supertype (class → superclass).
     */
    EXTENDS,

    /**
     * Repository/Service manages type (repository → aggregate).
     */
    MANAGES,

    /**
     * Type uses (depends on) another type.
     */
    USES,

    /**
     * Aggregate contains entity or value object.
     */
    CONTAINS,

    /**
     * Port is implemented by adapter (driven port → driven adapter).
     */
    IMPLEMENTED_BY,

    /**
     * Type publishes event (aggregate → domain event).
     */
    PUBLISHES
}
