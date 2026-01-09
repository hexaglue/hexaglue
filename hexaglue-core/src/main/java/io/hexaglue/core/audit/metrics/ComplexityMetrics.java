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
 * Metrics about code complexity.
 *
 * <p>This record captures cyclomatic complexity measurements across the codebase,
 * helping identify areas that may be difficult to understand, test, or maintain.
 *
 * @param maxCyclomaticComplexity  the highest cyclomatic complexity of any method
 * @param avgCyclomaticComplexity  the average cyclomatic complexity across all methods
 * @param methodsAboveThreshold    number of methods with complexity > 10
 * @since 3.0.0
 */
public record ComplexityMetrics(
        int maxCyclomaticComplexity, double avgCyclomaticComplexity, int methodsAboveThreshold) {

    /**
     * Returns true if the codebase has acceptable complexity.
     *
     * <p>Criteria: max complexity <= 20 and less than 10% of methods above threshold.
     *
     * @param totalMethods the total number of methods in the codebase
     * @return true if complexity is acceptable
     */
    public boolean isAcceptable(int totalMethods) {
        if (totalMethods == 0) {
            return true;
        }
        double percentAboveThreshold = (double) methodsAboveThreshold / totalMethods * 100;
        return maxCyclomaticComplexity <= 20 && percentAboveThreshold < 10.0;
    }

    /**
     * Returns the percentage of methods above the complexity threshold.
     *
     * @param totalMethods the total number of methods
     * @return percentage (0-100)
     */
    public double percentAboveThreshold(int totalMethods) {
        if (totalMethods == 0) {
            return 0.0;
        }
        return (double) methodsAboveThreshold / totalMethods * 100.0;
    }
}
