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

package io.hexaglue.core.classification;

/**
 * Confidence level of a classification.
 *
 * <p>Higher confidence means more certainty in the classification.
 * Used for tie-breaking when multiple criteria match.
 */
public enum ConfidenceLevel {

    /**
     * Explicit annotation present (e.g., @AggregateRoot).
     * Highest confidence - user explicitly declared intent.
     */
    EXPLICIT(100),

    /**
     * Strong heuristic match (e.g., used in Repository signature + has id field).
     * High confidence - multiple strong signals align.
     */
    HIGH(80),

    /**
     * Medium heuristic match (e.g., naming pattern + package location).
     * Moderate confidence - some signals present.
     */
    MEDIUM(60),

    /**
     * Weak heuristic match (e.g., only naming pattern).
     * Low confidence - minimal signals.
     */
    LOW(40);

    private final int weight;

    ConfidenceLevel(int weight) {
        this.weight = weight;
    }

    /**
     * Returns the numeric weight of this confidence level.
     * Higher weight = higher confidence.
     */
    public int weight() {
        return weight;
    }

    /**
     * Returns true if this confidence is at least as high as the given level.
     */
    public boolean isAtLeast(ConfidenceLevel other) {
        return this.weight >= other.weight;
    }

    /**
     * Returns true if this confidence is higher than the given level.
     */
    public boolean isHigherThan(ConfidenceLevel other) {
        return this.weight > other.weight;
    }
}
