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

package io.hexaglue.syntax;

/**
 * The form (structure) of a type declaration.
 *
 * @since 4.0.0
 */
public enum TypeForm {

    /**
     * A class (non-record).
     */
    CLASS,

    /**
     * An interface.
     */
    INTERFACE,

    /**
     * A record (Java 16+).
     */
    RECORD,

    /**
     * An enum.
     */
    ENUM,

    /**
     * An annotation type.
     */
    ANNOTATION
}
