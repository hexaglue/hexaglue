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
 * Cardinality of a relationship between elements.
 *
 * <p>Used to express how many instances can participate in a relationship,
 * such as aggregate references or entity associations.</p>
 *
 * @since 4.0.0
 */
public enum Cardinality {

    /**
     * Exactly one instance (required).
     */
    ONE,

    /**
     * Zero or one instance (optional).
     */
    ZERO_OR_ONE,

    /**
     * Zero or more instances (collection, optional).
     */
    ZERO_OR_MANY,

    /**
     * One or more instances (collection, required at least one).
     */
    ONE_OR_MANY;

    /**
     * Returns whether this cardinality is required (at least one instance).
     *
     * @return true if at least one instance is required
     */
    public boolean isRequired() {
        return this == ONE || this == ONE_OR_MANY;
    }

    /**
     * Returns whether this cardinality allows multiple instances.
     *
     * @return true if multiple instances are allowed
     */
    public boolean isMany() {
        return this == ZERO_OR_MANY || this == ONE_OR_MANY;
    }
}
