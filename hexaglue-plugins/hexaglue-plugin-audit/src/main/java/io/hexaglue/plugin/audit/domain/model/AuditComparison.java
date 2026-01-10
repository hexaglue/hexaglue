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

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a comparison between two audit results.
 *
 * <p>This immutable value object captures the delta between a previous audit
 * and a current audit, identifying what has improved, what has degraded, and
 * the overall trend.
 *
 * <p>Violations are matched by their constraint ID and affected types to
 * determine which violations are new, which have been fixed, and which remain
 * unchanged.
 *
 * @param previousScore           the health score from the previous audit (0-100)
 * @param currentScore            the health score from the current audit (0-100)
 * @param scoreChange             the delta between current and previous score (positive = improved)
 * @param trend                   the overall quality trend
 * @param newViolations           violations that appeared since the previous audit
 * @param fixedViolations         violations that were resolved since the previous audit
 * @param unchangedViolationCount the count of violations present in both audits
 * @since 1.0.0
 */
public record AuditComparison(
        double previousScore,
        double currentScore,
        double scoreChange,
        AuditTrend trend,
        List<Violation> newViolations,
        List<Violation> fixedViolations,
        int unchangedViolationCount) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public AuditComparison {
        if (previousScore < 0 || previousScore > 100) {
            throw new IllegalArgumentException("previousScore must be between 0 and 100: " + previousScore);
        }
        if (currentScore < 0 || currentScore > 100) {
            throw new IllegalArgumentException("currentScore must be between 0 and 100: " + currentScore);
        }
        Objects.requireNonNull(trend, "trend required");
        newViolations = newViolations != null ? List.copyOf(newViolations) : List.of();
        fixedViolations = fixedViolations != null ? List.copyOf(fixedViolations) : List.of();
        if (unchangedViolationCount < 0) {
            throw new IllegalArgumentException(
                    "unchangedViolationCount cannot be negative: " + unchangedViolationCount);
        }
    }

    /**
     * Creates a comparison for a first audit (no previous audit).
     *
     * <p>When there is no previous audit to compare against, all violations are
     * considered new and the trend is STABLE (baseline establishment).
     *
     * @param currentScore    the current audit score
     * @param newViolations   all violations found
     * @return a comparison representing the first audit
     */
    public static AuditComparison firstAudit(double currentScore, List<Violation> newViolations) {
        return new AuditComparison(
                currentScore, // previousScore = current for first run
                currentScore,
                0.0, // no change
                AuditTrend.STABLE,
                newViolations,
                List.of(), // no fixed violations
                0 // no unchanged violations
                );
    }

    /**
     * Returns the total number of violations in the current audit.
     *
     * @return sum of new violations and unchanged violations
     */
    public int currentViolationCount() {
        return newViolations.size() + unchangedViolationCount;
    }

    /**
     * Returns the total number of violations in the previous audit.
     *
     * @return sum of fixed violations and unchanged violations
     */
    public int previousViolationCount() {
        return fixedViolations.size() + unchangedViolationCount;
    }

    /**
     * Returns true if quality has improved since the previous audit.
     *
     * @return true if trend is IMPROVED
     */
    public boolean hasImproved() {
        return trend == AuditTrend.IMPROVED;
    }

    /**
     * Returns true if quality has degraded since the previous audit.
     *
     * @return true if trend is DEGRADED
     */
    public boolean hasDegraded() {
        return trend == AuditTrend.DEGRADED;
    }

    /**
     * Returns true if quality remains stable since the previous audit.
     *
     * @return true if trend is STABLE
     */
    public boolean isStable() {
        return trend == AuditTrend.STABLE;
    }
}
