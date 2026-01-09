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
 * Metrics about code size.
 *
 * <p>This record captures quantitative measurements of codebase size including
 * types, methods, fields, and lines of code.
 *
 * @param typeCount   total number of types (classes, interfaces, enums, records)
 * @param methodCount total number of methods across all types
 * @param fieldCount  total number of fields across all types
 * @param locTotal    total lines of code (excluding comments and blank lines)
 * @since 3.0.0
 */
public record CodeSizeMetrics(int typeCount, int methodCount, int fieldCount, int locTotal) {

    /**
     * Calculates average methods per type.
     *
     * @return average methods per type, or 0 if no types
     */
    public double avgMethodsPerType() {
        return typeCount > 0 ? (double) methodCount / typeCount : 0.0;
    }

    /**
     * Calculates average fields per type.
     *
     * @return average fields per type, or 0 if no types
     */
    public double avgFieldsPerType() {
        return typeCount > 0 ? (double) fieldCount / typeCount : 0.0;
    }

    /**
     * Calculates average LOC per type.
     *
     * @return average lines of code per type, or 0 if no types
     */
    public double avgLocPerType() {
        return typeCount > 0 ? (double) locTotal / typeCount : 0.0;
    }

    /**
     * Calculates average LOC per method.
     *
     * @return average lines of code per method, or 0 if no methods
     */
    public double avgLocPerMethod() {
        return methodCount > 0 ? (double) locTotal / methodCount : 0.0;
    }
}
