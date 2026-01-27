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

package io.hexaglue.arch.model.audit;

/**
 * Overall quality metrics for the codebase.
 *
 * @param testCoverage            test coverage percentage (0-100)
 * @param documentationCoverage   documentation coverage percentage (0-100)
 * @param technicalDebtMinutes    estimated technical debt in minutes
 * @param maintainabilityRating   maintainability rating (0-5, higher is better)
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record QualityMetrics(
        double testCoverage, double documentationCoverage, int technicalDebtMinutes, double maintainabilityRating) {

    /**
     * Returns true if test coverage is adequate.
     *
     * @return true if coverage >= 80%
     */
    public boolean hasAdequateTestCoverage() {
        return testCoverage >= 80.0;
    }

    /**
     * Returns true if documentation coverage is adequate.
     *
     * @return true if coverage >= 80%
     */
    public boolean hasAdequateDocumentation() {
        return documentationCoverage >= 80.0;
    }

    /**
     * Returns true if maintainability is good.
     *
     * @return true if rating >= 4.0
     */
    public boolean isMaintainable() {
        return maintainabilityRating >= 4.0;
    }
}
