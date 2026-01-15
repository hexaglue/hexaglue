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
 * Query method modifiers for Spring Data derived queries.
 *
 * <p>These modifiers affect how the query is executed or how results are processed.
 *
 * @since 3.0.0
 */
public enum QueryModifier {

    /**
     * Return distinct results (e.g., {@code findDistinctBy...}).
     */
    DISTINCT,

    /**
     * Case-insensitive comparison for specific property (e.g., {@code ...IgnoreCase}).
     */
    IGNORE_CASE,

    /**
     * Case-insensitive comparison for all properties (e.g., {@code ...AllIgnoreCase}).
     */
    ALL_IGNORE_CASE,

    /**
     * Order results by property ascending (e.g., {@code ...OrderByXxxAsc}).
     */
    ORDER_BY_ASC,

    /**
     * Order results by property descending (e.g., {@code ...OrderByXxxDesc}).
     */
    ORDER_BY_DESC
}
