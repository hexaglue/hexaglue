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
 * Java modifiers for types, methods, and fields.
 *
 * @since 4.0.0
 */
public enum Modifier {

    // Access modifiers
    PUBLIC,
    PROTECTED,
    PRIVATE,

    // Non-access modifiers
    STATIC,
    FINAL,
    ABSTRACT,
    NATIVE,
    SYNCHRONIZED,
    TRANSIENT,
    VOLATILE,
    STRICTFP,

    // Java 17+ modifiers
    SEALED,
    NON_SEALED,

    // Method modifiers
    DEFAULT
}
