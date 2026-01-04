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
 * Confidence level of a classification decision.
 *
 * <p>Higher confidence means the classification is more reliable.
 * Plugins may use this to adjust their behavior (e.g., skip generation
 * for low-confidence classifications).
 */
public enum ConfidenceLevel {

    /**
     * Classification based on explicit annotation.
     * Highest confidence - the developer explicitly declared intent.
     */
    EXPLICIT,

    /**
     * Classification based on strong heuristics.
     * High confidence - multiple strong signals converge.
     */
    HIGH,

    /**
     * Classification based on moderate heuristics.
     * Medium confidence - some signals present but not conclusive.
     */
    MEDIUM,

    /**
     * Classification based on weak heuristics.
     * Low confidence - should be verified manually.
     */
    LOW;

    /**
     * Returns true if this confidence level is considered reliable.
     * EXPLICIT and HIGH are considered reliable.
     */
    public boolean isReliable() {
        return this == EXPLICIT || this == HIGH;
    }

    /**
     * Returns true if this confidence level is higher than or equal to the given level.
     */
    public boolean isAtLeast(ConfidenceLevel other) {
        return this.ordinal() <= other.ordinal();
    }
}
