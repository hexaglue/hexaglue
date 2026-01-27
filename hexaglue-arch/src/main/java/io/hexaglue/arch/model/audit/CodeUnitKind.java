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

package io.hexaglue.arch.model.audit;

/**
 * The kind of code unit being audited.
 *
 * <p>This enum represents the Java construct type of a code unit.
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public enum CodeUnitKind {

    /** A regular Java class. */
    CLASS,

    /** A Java interface. */
    INTERFACE,

    /** A Java enum. */
    ENUM,

    /** A Java record. */
    RECORD,

    /** A Java annotation type. */
    ANNOTATION
}
