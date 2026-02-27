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

package io.hexaglue.plugin.rest.model;

/**
 * How a DTO field was derived from the domain model.
 *
 * @since 3.1.0
 */
public enum ProjectionKind {
    /** Field copied as-is (String, int, enum). */
    DIRECT,
    /** Identity unwrap: CustomerId to Long via .value(). */
    IDENTITY_UNWRAP,
    /** Value object flatten: Money to balanceAmount + balanceCurrency. */
    VALUE_OBJECT_FLATTEN,
    /** Cross-aggregate reference to Long (unwrap ID). */
    AGGREGATE_REFERENCE,
    /** Collection mapping: List of X to List of XResponse. */
    COLLECTION,
    /** Complex type mapped to a nested DTO. */
    NESTED_DTO
}
