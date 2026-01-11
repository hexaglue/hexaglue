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

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.ir.IrSnapshot;
import java.util.List;
import java.util.Objects;

/**
 * Calculates DDD and Hexagonal Architecture compliance percentages.
 *
 * <p>Compliance is computed based on the number and severity of violations:
 * <ul>
 *   <li>BLOCKER violations: -10 points each</li>
 *   <li>CRITICAL violations: -7 points each</li>
 *   <li>MAJOR violations: -5 points each</li>
 *   <li>MINOR violations: -2 points each</li>
 *   <li>INFO violations: -1 point each</li>
 * </ul>
 *
 * <p>The base score is 100, and penalties are deducted based on violations
 * in the respective category (ddd:* or hexagonal:*).
 *
 * @since 1.0.0
 */
public class ComplianceCalculator {

    /** Category prefix for DDD constraints. */
    public static final String DDD_CATEGORY = "ddd";

    /** Category prefix for hexagonal architecture constraints. */
    public static final String HEXAGONAL_CATEGORY = "hexagonal";

    /** Penalty points for BLOCKER severity. */
    public static final int BLOCKER_PENALTY = 10;

    /** Penalty points for CRITICAL severity. */
    public static final int CRITICAL_PENALTY = 7;

    /** Penalty points for MAJOR severity. */
    public static final int MAJOR_PENALTY = 5;

    /** Penalty points for MINOR severity. */
    public static final int MINOR_PENALTY = 2;

    /** Penalty points for INFO severity. */
    public static final int INFO_PENALTY = 1;

    /**
     * Calculates the DDD compliance percentage.
     *
     * @param violations the list of violations
     * @param ir         the IR snapshot (for context, currently unused)
     * @return DDD compliance percentage (0-100)
     */
    public int calculateDddCompliance(List<Violation> violations, IrSnapshot ir) {
        return calculateCompliance(violations, DDD_CATEGORY);
    }

    /**
     * Calculates the hexagonal architecture compliance percentage.
     *
     * @param violations the list of violations
     * @param ir         the IR snapshot (for context, currently unused)
     * @return hexagonal compliance percentage (0-100)
     */
    public int calculateHexCompliance(List<Violation> violations, IrSnapshot ir) {
        return calculateCompliance(violations, HEXAGONAL_CATEGORY);
    }

    /**
     * Calculates compliance for a specific category.
     *
     * @param violations the list of all violations
     * @param category   the category to filter (e.g., "ddd", "hexagonal")
     * @return compliance percentage (0-100)
     */
    private int calculateCompliance(List<Violation> violations, String category) {
        Objects.requireNonNull(violations, "violations required");
        Objects.requireNonNull(category, "category required");

        int penalty = violations.stream()
                .filter(v -> category.equals(v.constraintId().category()))
                .mapToInt(this::getPenalty)
                .sum();

        return Math.max(0, 100 - penalty);
    }

    /**
     * Returns the penalty points for a violation based on its severity.
     *
     * @param violation the violation
     * @return penalty points
     */
    private int getPenalty(Violation violation) {
        return switch (violation.severity()) {
            case BLOCKER -> BLOCKER_PENALTY;
            case CRITICAL -> CRITICAL_PENALTY;
            case MAJOR -> MAJOR_PENALTY;
            case MINOR -> MINOR_PENALTY;
            case INFO -> INFO_PENALTY;
        };
    }

    /**
     * Counts violations by category.
     *
     * @param violations the list of violations
     * @param category   the category to count
     * @return the number of violations in that category
     */
    public int countViolations(List<Violation> violations, String category) {
        Objects.requireNonNull(violations, "violations required");
        Objects.requireNonNull(category, "category required");

        return (int) violations.stream()
                .filter(v -> category.equals(v.constraintId().category()))
                .count();
    }
}
