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

package io.hexaglue.core.style;

import java.util.Map;

/**
 * Result of architecture style detection.
 *
 * <p>Contains the detected style, confidence level, and supporting evidence.
 *
 * @param style the detected architecture style
 * @param confidence confidence score (0.0 to 1.0)
 * @param description human-readable explanation of the detection
 * @param evidence supporting evidence for the detection (e.g., package counts, patterns found)
 */
public record DetectedStyle(
        ArchitectureStyle style, double confidence, String description, Map<String, Object> evidence) {

    /**
     * Creates a detected style with validation.
     */
    public DetectedStyle {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0, got: " + confidence);
        }
    }

    /**
     * Creates a high-confidence detection.
     *
     * @param style the detected style
     * @param description the description
     * @return detected style with 0.9 confidence
     */
    public static DetectedStyle highConfidence(ArchitectureStyle style, String description) {
        return new DetectedStyle(style, 0.9, description, Map.of());
    }

    /**
     * Creates a medium-confidence detection.
     *
     * @param style the detected style
     * @param description the description
     * @return detected style with 0.6 confidence
     */
    public static DetectedStyle mediumConfidence(ArchitectureStyle style, String description) {
        return new DetectedStyle(style, 0.6, description, Map.of());
    }

    /**
     * Creates a low-confidence detection.
     *
     * @param style the detected style
     * @param description the description
     * @return detected style with 0.3 confidence
     */
    public static DetectedStyle lowConfidence(ArchitectureStyle style, String description) {
        return new DetectedStyle(style, 0.3, description, Map.of());
    }

    /**
     * Creates an unknown style detection.
     *
     * @return detected style with UNKNOWN and 0.0 confidence
     */
    public static DetectedStyle unknown() {
        return new DetectedStyle(
                ArchitectureStyle.UNKNOWN, 0.0, "No recognizable architecture style detected", Map.of());
    }

    /**
     * Returns true if this is a high-confidence detection (>= 0.7).
     */
    public boolean isHighConfidence() {
        return confidence >= 0.7;
    }

    /**
     * Returns true if this is a medium-confidence detection (0.5-0.7).
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.7;
    }

    /**
     * Returns true if this is a low-confidence detection (< 0.5).
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }

    /**
     * Returns a confidence level description.
     */
    public String confidenceLevel() {
        if (isHighConfidence()) {
            return "HIGH";
        } else if (isMediumConfidence()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
