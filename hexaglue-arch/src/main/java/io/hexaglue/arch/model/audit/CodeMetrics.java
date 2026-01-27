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
 * Code quality metrics for a code unit.
 *
 * <p>This record captures quantitative metrics about code complexity,
 * size, and maintainability.
 *
 * @param linesOfCode            total lines of code (excluding comments and blank lines)
 * @param cyclomaticComplexity   cyclomatic complexity (McCabe)
 * @param numberOfMethods        total number of methods
 * @param numberOfFields         total number of fields
 * @param maintainabilityIndex   maintainability index (0-100, higher is better)
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record CodeMetrics(
        int linesOfCode,
        int cyclomaticComplexity,
        int numberOfMethods,
        int numberOfFields,
        double maintainabilityIndex) {

    /**
     * Returns true if the code is considered complex (high cyclomatic complexity).
     *
     * @return true if cyclomatic complexity > 10
     */
    public boolean isComplex() {
        return cyclomaticComplexity > 10;
    }

    /**
     * Returns true if maintainability is considered good.
     *
     * @return true if maintainability index >= 70
     */
    public boolean isMaintainable() {
        return maintainabilityIndex >= 70.0;
    }
}
