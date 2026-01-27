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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Result of evaluating a single constraint.
 *
 * @param id constraint identifier (e.g., "ddd:aggregate-cycle")
 * @param violations number of violations found
 * @since 5.0.0
 */
public record ConstraintResult(
        String id,
        int violations) {

    /**
     * Creates a constraint result with validation.
     */
    public ConstraintResult {
        Objects.requireNonNull(id, "id is required");
        if (violations < 0) {
            throw new IllegalArgumentException("violations cannot be negative");
        }
    }

    /**
     * Creates a constraint result with no violations.
     *
     * @param id constraint id
     * @return constraint result
     */
    public static ConstraintResult passed(String id) {
        return new ConstraintResult(id, 0);
    }

    /**
     * Creates a constraint result with violations.
     *
     * @param id constraint id
     * @param violations number of violations
     * @return constraint result
     */
    public static ConstraintResult failed(String id, int violations) {
        return new ConstraintResult(id, violations);
    }

    /**
     * Checks if the constraint passed (no violations).
     *
     * @return true if passed
     */
    public boolean passed() {
        return violations == 0;
    }
}
