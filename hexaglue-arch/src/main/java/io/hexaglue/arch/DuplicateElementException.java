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
 * Exception thrown when attempting to add a duplicate element to a registry.
 *
 * @since 4.0.0
 */
public class DuplicateElementException extends RuntimeException {

    private final ElementId id;

    /**
     * Creates a new exception for a duplicate element.
     *
     * @param id the identifier of the duplicate element
     */
    public DuplicateElementException(ElementId id) {
        super("Duplicate element: " + id);
        this.id = id;
    }

    /**
     * Returns the identifier of the duplicate element.
     *
     * @return the duplicate element identifier
     */
    public ElementId getId() {
        return id;
    }
}
