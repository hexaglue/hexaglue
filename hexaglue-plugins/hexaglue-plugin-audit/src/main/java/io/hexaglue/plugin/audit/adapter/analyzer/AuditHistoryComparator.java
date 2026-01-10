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

package io.hexaglue.plugin.audit.adapter.analyzer;

import io.hexaglue.plugin.audit.domain.model.AuditComparison;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.AuditTrend;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compares two audit results to track quality evolution over time.
 *
 * <p>This analyzer identifies violations that have been introduced or fixed
 * between consecutive audit runs, calculates score deltas, and determines the
 * overall quality trend.
 *
 * <p><strong>Violation Matching Algorithm:</strong>
 * <p>Two violations are considered the same if they share:
 * <ul>
 *   <li>The same constraint ID (e.g., "ddd:aggregate-repository")</li>
 *   <li>The same set of affected types (order-independent)</li>
 * </ul>
 *
 * <p>This matching strategy correctly identifies when a violation has been
 * fixed even if its message or location has changed, as long as the fundamental
 * issue (constraint + affected types) is the same.
 *
 * <p><strong>Trend Determination:</strong>
 * <ul>
 *   <li>{@link AuditTrend#IMPROVED IMPROVED}: Score increased OR violation count decreased</li>
 *   <li>{@link AuditTrend#DEGRADED DEGRADED}: Score decreased OR violation count increased</li>
 *   <li>{@link AuditTrend#STABLE STABLE}: No significant change (score within ±1 point, same violation count)</li>
 * </ul>
 *
 * <p><strong>Health Score Calculation:</strong>
 * <p>The health score is extracted from the "health.score" metric in the audit result.
 * If the metric is not present, a default score of 0.0 is used.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * AuditHistoryComparator comparator = new AuditHistoryComparator();
 *
 * // First audit (no previous)
 * AuditComparison first = comparator.compare(null, currentResult);
 * // Returns: trend=STABLE, all violations are new
 *
 * // Subsequent audit (with previous)
 * AuditComparison second = comparator.compare(previousResult, currentResult);
 * // Returns: trend=IMPROVED/DEGRADED/STABLE, identifies new/fixed violations
 * }</pre>
 *
 * @since 1.0.0
 */
public class AuditHistoryComparator {

    /**
     * The metric name used to extract health scores from audit results.
     */
    private static final String HEALTH_SCORE_METRIC = "health.score";

    /**
     * Score change tolerance for considering audits stable (±1 point).
     */
    private static final double SCORE_TOLERANCE = 1.0;

    /**
     * Compares two audit results to determine quality evolution.
     *
     * <p>If the previous result is null, this is treated as the first audit run,
     * and all violations in the current result are considered new. The trend
     * will be {@link AuditTrend#STABLE STABLE} to establish a baseline.
     *
     * @param previous the previous audit result (null if this is the first audit)
     * @param current  the current audit result
     * @return an audit comparison capturing the delta
     * @throws NullPointerException if current is null
     */
    public AuditComparison compare(AuditResult previous, AuditResult current) {
        Objects.requireNonNull(current, "current audit result required");

        // Handle first audit case
        if (previous == null) {
            double currentScore = extractHealthScore(current);
            return AuditComparison.firstAudit(currentScore, current.violations());
        }

        // Extract health scores
        double previousScore = extractHealthScore(previous);
        double currentScore = extractHealthScore(current);
        double scoreChange = currentScore - previousScore;

        // Identify violation changes
        Set<ViolationKey> previousKeys = buildViolationKeys(previous.violations());
        Set<ViolationKey> currentKeys = buildViolationKeys(current.violations());

        List<Violation> newViolations = findNewViolations(current.violations(), currentKeys, previousKeys);
        List<Violation> fixedViolations = findFixedViolations(previous.violations(), previousKeys, currentKeys);
        int unchangedCount = countUnchangedViolations(previousKeys, currentKeys);

        // Determine trend
        AuditTrend trend = determineTrend(
                scoreChange, previous.violations().size(), current.violations().size());

        return new AuditComparison(
                previousScore, currentScore, scoreChange, trend, newViolations, fixedViolations, unchangedCount);
    }

    /**
     * Extracts the health score from an audit result.
     *
     * <p>If the health.score metric is not present, returns 0.0 as the default.
     *
     * @param result the audit result
     * @return the health score (0-100)
     */
    private double extractHealthScore(AuditResult result) {
        return Optional.ofNullable(result.metrics().get(HEALTH_SCORE_METRIC))
                .map(Metric::value)
                .orElse(0.0);
    }

    /**
     * Builds a set of violation keys for matching.
     *
     * @param violations the violations to build keys for
     * @return a set of violation keys
     */
    private Set<ViolationKey> buildViolationKeys(List<Violation> violations) {
        Set<ViolationKey> keys = new HashSet<>();
        for (Violation violation : violations) {
            keys.add(ViolationKey.of(violation));
        }
        return keys;
    }

    /**
     * Finds violations that are new in the current audit.
     *
     * @param currentViolations all violations in current audit
     * @param currentKeys       keys of current violations
     * @param previousKeys      keys of previous violations
     * @return list of new violations
     */
    private List<Violation> findNewViolations(
            List<Violation> currentViolations, Set<ViolationKey> currentKeys, Set<ViolationKey> previousKeys) {
        List<Violation> newViolations = new ArrayList<>();
        for (Violation violation : currentViolations) {
            ViolationKey key = ViolationKey.of(violation);
            if (!previousKeys.contains(key)) {
                newViolations.add(violation);
            }
        }
        return newViolations;
    }

    /**
     * Finds violations that were fixed (present in previous, absent in current).
     *
     * @param previousViolations all violations in previous audit
     * @param previousKeys       keys of previous violations
     * @param currentKeys        keys of current violations
     * @return list of fixed violations
     */
    private List<Violation> findFixedViolations(
            List<Violation> previousViolations, Set<ViolationKey> previousKeys, Set<ViolationKey> currentKeys) {
        List<Violation> fixedViolations = new ArrayList<>();
        for (Violation violation : previousViolations) {
            ViolationKey key = ViolationKey.of(violation);
            if (!currentKeys.contains(key)) {
                fixedViolations.add(violation);
            }
        }
        return fixedViolations;
    }

    /**
     * Counts violations that appear in both audits.
     *
     * @param previousKeys keys of previous violations
     * @param currentKeys  keys of current violations
     * @return count of unchanged violations
     */
    private int countUnchangedViolations(Set<ViolationKey> previousKeys, Set<ViolationKey> currentKeys) {
        Set<ViolationKey> intersection = new HashSet<>(previousKeys);
        intersection.retainAll(currentKeys);
        return intersection.size();
    }

    /**
     * Determines the overall quality trend.
     *
     * <p>The trend is determined by analyzing both score changes and violation count changes:
     * <ul>
     *   <li>IMPROVED: Score increased significantly OR violations decreased</li>
     *   <li>DEGRADED: Score decreased significantly OR violations increased</li>
     *   <li>STABLE: Score within tolerance AND same violation count</li>
     * </ul>
     *
     * @param scoreChange       the change in health score (positive = improved)
     * @param previousViolCount the number of violations in previous audit
     * @param currentViolCount  the number of violations in current audit
     * @return the trend
     */
    private AuditTrend determineTrend(double scoreChange, int previousViolCount, int currentViolCount) {
        boolean scoreImproved = scoreChange > SCORE_TOLERANCE;
        boolean scoreDegraded = scoreChange < -SCORE_TOLERANCE;
        boolean violationsDecreased = currentViolCount < previousViolCount;
        boolean violationsIncreased = currentViolCount > previousViolCount;

        // Score or violations improved -> IMPROVED
        if (scoreImproved || violationsDecreased) {
            return AuditTrend.IMPROVED;
        }

        // Score or violations degraded -> DEGRADED
        if (scoreDegraded || violationsIncreased) {
            return AuditTrend.DEGRADED;
        }

        // No significant change -> STABLE
        return AuditTrend.STABLE;
    }

    /**
     * Immutable key for matching violations across audit runs.
     *
     * <p>Two violations match if they have the same constraint ID and the same
     * set of affected types (order-independent).
     */
    private record ViolationKey(String constraintId, Set<String> affectedTypes) {

        /**
         * Creates a violation key from a violation.
         *
         * @param violation the violation
         * @return the violation key
         */
        static ViolationKey of(Violation violation) {
            return new ViolationKey(violation.constraintId().value(), Set.copyOf(violation.affectedTypes()));
        }
    }
}
