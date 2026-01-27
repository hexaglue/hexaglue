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

package io.hexaglue.arch.model.ir;

/**
 * Nullability of a property.
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.ir
 */
public enum Nullability {

    /**
     * The property is never null.
     */
    NON_NULL,

    /**
     * The property can be null.
     */
    NULLABLE,

    /**
     * Nullability could not be determined.
     */
    UNKNOWN
}
