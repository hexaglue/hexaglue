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
 * Cardinality of a property.
 */
public enum Cardinality {

    /**
     * Single required value (e.g., {@code String name}).
     */
    SINGLE,

    /**
     * Optional value (e.g., {@code Optional<String> middleName}).
     */
    OPTIONAL,

    /**
     * Collection of values (e.g., {@code List<LineItem> items}).
     */
    COLLECTION
}
