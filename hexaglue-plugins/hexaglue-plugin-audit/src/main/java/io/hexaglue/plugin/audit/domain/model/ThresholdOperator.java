/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

/**
 * Comparison operator for metric thresholds.
 *
 * @since 1.0.0
 */
public enum ThresholdOperator {
    /**
     * Value exceeds threshold if it is greater than the maximum.
     */
    GREATER_THAN,

    /**
     * Value exceeds threshold if it is less than the minimum.
     */
    LESS_THAN,

    /**
     * Value exceeds threshold if it is outside the [min, max] range.
     */
    BETWEEN
}
