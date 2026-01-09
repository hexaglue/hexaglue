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

package io.hexaglue.core.audit.metrics;

/**
 * Metrics about documentation coverage.
 *
 * <p>This record captures documentation statistics to help assess how well
 * the codebase is documented. Good documentation improves maintainability
 * and helps onboard new developers.
 *
 * @param documentedTypesRatio         ratio of types with Javadoc (0.0 - 1.0)
 * @param documentedPublicMethodsRatio ratio of public methods with Javadoc (0.0 - 1.0)
 * @since 3.0.0
 */
public record DocumentationMetrics(double documentedTypesRatio, double documentedPublicMethodsRatio) {

    /**
     * Compact constructor with validation.
     */
    public DocumentationMetrics {
        if (documentedTypesRatio < 0.0 || documentedTypesRatio > 1.0) {
            throw new IllegalArgumentException("documentedTypesRatio must be between 0.0 and 1.0");
        }
        if (documentedPublicMethodsRatio < 0.0 || documentedPublicMethodsRatio > 1.0) {
            throw new IllegalArgumentException("documentedPublicMethodsRatio must be between 0.0 and 1.0");
        }
    }

    /**
     * Returns the type documentation coverage as a percentage.
     *
     * @return percentage (0-100)
     */
    public double typesCoveragePercent() {
        return documentedTypesRatio * 100.0;
    }

    /**
     * Returns the public method documentation coverage as a percentage.
     *
     * @return percentage (0-100)
     */
    public double publicMethodsCoveragePercent() {
        return documentedPublicMethodsRatio * 100.0;
    }

    /**
     * Returns true if documentation coverage is adequate.
     *
     * <p>Criteria: both ratios >= 0.8 (80%)
     *
     * @return true if adequately documented
     */
    public boolean isAdequate() {
        return documentedTypesRatio >= 0.8 && documentedPublicMethodsRatio >= 0.8;
    }

    /**
     * Returns the overall documentation score (0-100).
     *
     * <p>Weighted average: types 40%, public methods 60%
     *
     * @return score from 0 to 100
     */
    public double overallScore() {
        return (documentedTypesRatio * 0.4 + documentedPublicMethodsRatio * 0.6) * 100.0;
    }
}
