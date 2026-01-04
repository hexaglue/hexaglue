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

package io.hexaglue.core.frontend;

/**
 * The syntactic form of a Java type declaration.
 */
public enum JavaForm {

    /**
     * A regular class declaration.
     */
    CLASS,

    /**
     * A record declaration (Java 16+).
     */
    RECORD,

    /**
     * An interface declaration.
     */
    INTERFACE,

    /**
     * An enum declaration.
     */
    ENUM,

    /**
     * An annotation type declaration.
     */
    ANNOTATION
}
