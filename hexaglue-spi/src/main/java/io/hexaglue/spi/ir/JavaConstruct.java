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

package io.hexaglue.spi.ir;

/**
 * The Java construct used to define a type.
 *
 * <p>This is distinct from {@link DomainKind} - a VALUE_OBJECT can be
 * implemented as a CLASS or a RECORD.
 */
public enum JavaConstruct {

    /**
     * A regular Java class.
     */
    CLASS,

    /**
     * A Java record (immutable data carrier).
     */
    RECORD,

    /**
     * A Java enum.
     */
    ENUM,

    /**
     * A Java interface.
     */
    INTERFACE
}
