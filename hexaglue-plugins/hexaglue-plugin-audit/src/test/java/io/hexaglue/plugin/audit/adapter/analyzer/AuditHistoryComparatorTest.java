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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.hexaglue.plugin.audit.domain.model.AuditComparison;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.AuditTrend;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.arch.model.audit.SourceLocation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AuditHistoryComparator}.
 */
class AuditHistoryComparatorTest {

    private static final ConstraintId CONSTRAINT_A = ConstraintId.of("ddd:aggregate-repository");
    private static final ConstraintId CONSTRAINT_B = ConstraintId.of("ddd:value-immutable");
    private static final ConstraintId CONSTRAINT_C = ConstraintId.of("hexagonal:dependency-direction");
    private static final SourceLocation TEST_LOCATION = SourceLocation.of("Test.java", 1, 1);

    private final AuditHistoryComparator comparator = new AuditHistoryComparator();

    // === First Audit (No Previous) Tests ===

    @Test
    void shouldHandleFirstAudit_withNoPrevious() {
        // Given: First audit with 2 violations
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        AuditResult current = createAuditResult(75.0, List.of(v1, v2));

        // When
        AuditComparison comparison = comparator.compare(null, current);

        // Then
        assertThat(comparison.previousScore()).isEqualTo(75.0);
        assertThat(comparison.currentScore()).isEqualTo(75.0);
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
        assertThat(comparison.trend()).isEqualTo(AuditTrend.STABLE);
        assertThat(comparison.newViolations()).hasSize(2).containsExactlyInAnyOrder(v1, v2);
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    @Test
    void shouldHandleFirstAudit_withNoViolations() {
        // Given: First audit with perfect score
        AuditResult current = createAuditResult(100.0, List.of());

        // When
        AuditComparison comparison = comparator.compare(null, current);

        // Then
        assertThat(comparison.previousScore()).isEqualTo(100.0);
        assertThat(comparison.currentScore()).isEqualTo(100.0);
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
        assertThat(comparison.trend()).isEqualTo(AuditTrend.STABLE);
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    @Test
    void shouldRejectNullCurrentAudit() {
        // When/Then
        assertThatThrownBy(() -> comparator.compare(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("current audit result required");
    }

    // === Identical Audits (Stable) Tests ===

    @Test
    void shouldDetectStable_whenAuditsAreIdentical() {
        // Given: Two identical audits with same violations
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        AuditResult previous = createAuditResult(75.0, List.of(v1, v2));
        AuditResult current = createAuditResult(75.0, List.of(v1, v2));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.previousScore()).isEqualTo(75.0);
        assertThat(comparison.currentScore()).isEqualTo(75.0);
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
        assertThat(comparison.trend()).isEqualTo(AuditTrend.STABLE);
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(2);
        assertThat(comparison.isStable()).isTrue();
        assertThat(comparison.hasImproved()).isFalse();
        assertThat(comparison.hasDegraded()).isFalse();
    }

    @Test
    void shouldDetectStable_whenScoreChangeWithinTolerance() {
        // Given: Score changed by 0.5 (within ±1 tolerance), same violations
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        AuditResult previous = createAuditResult(75.0, List.of(v1));
        AuditResult current = createAuditResult(75.5, List.of(v1));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isCloseTo(0.5, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.STABLE);
    }

    @Test
    void shouldDetectStable_whenBothAuditsHaveNoViolations() {
        // Given: Two perfect audits
        AuditResult previous = createAuditResult(100.0, List.of());
        AuditResult current = createAuditResult(100.0, List.of());

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.trend()).isEqualTo(AuditTrend.STABLE);
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    // === Improved Quality Tests ===

    @Test
    void shouldDetectImproved_whenScoreIncreased() {
        // Given: Score improved from 70 to 85
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        AuditResult previous = createAuditResult(70.0, List.of(v1));
        AuditResult current = createAuditResult(85.0, List.of(v1));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.previousScore()).isEqualTo(70.0);
        assertThat(comparison.currentScore()).isEqualTo(85.0);
        assertThat(comparison.scoreChange()).isCloseTo(15.0, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.IMPROVED);
        assertThat(comparison.hasImproved()).isTrue();
    }

    @Test
    void shouldDetectImproved_whenViolationsDecreased() {
        // Given: Violations reduced from 3 to 1
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        Violation v3 = createViolation(CONSTRAINT_C, "com.example.Customer");
        AuditResult previous = createAuditResult(70.0, List.of(v1, v2, v3));
        AuditResult current = createAuditResult(70.0, List.of(v1));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
        assertThat(comparison.trend()).isEqualTo(AuditTrend.IMPROVED);
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).hasSize(2).containsExactlyInAnyOrder(v2, v3);
        assertThat(comparison.unchangedViolationCount()).isEqualTo(1);
        assertThat(comparison.previousViolationCount()).isEqualTo(3);
        assertThat(comparison.currentViolationCount()).isEqualTo(1);
    }

    @Test
    void shouldDetectImproved_whenAllViolationsFixed() {
        // Given: All violations fixed
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        AuditResult previous = createAuditResult(70.0, List.of(v1, v2));
        AuditResult current = createAuditResult(100.0, List.of());

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isCloseTo(30.0, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.IMPROVED);
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).hasSize(2).containsExactlyInAnyOrder(v1, v2);
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    @Test
    void shouldDetectImproved_whenScoreAndViolationsBothImproved() {
        // Given: Score improved AND violations decreased
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        AuditResult previous = createAuditResult(70.0, List.of(v1, v2));
        AuditResult current = createAuditResult(90.0, List.of(v1));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isCloseTo(20.0, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.IMPROVED);
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).containsExactly(v2);
        assertThat(comparison.unchangedViolationCount()).isEqualTo(1);
    }

    // === Degraded Quality Tests ===

    @Test
    void shouldDetectDegraded_whenScoreDecreased() {
        // Given: Score degraded from 85 to 70
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        AuditResult previous = createAuditResult(85.0, List.of(v1));
        AuditResult current = createAuditResult(70.0, List.of(v1));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.previousScore()).isEqualTo(85.0);
        assertThat(comparison.currentScore()).isEqualTo(70.0);
        assertThat(comparison.scoreChange()).isCloseTo(-15.0, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.DEGRADED);
        assertThat(comparison.hasDegraded()).isTrue();
    }

    @Test
    void shouldDetectDegraded_whenViolationsIncreased() {
        // Given: Violations increased from 1 to 3
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        Violation v3 = createViolation(CONSTRAINT_C, "com.example.Customer");
        AuditResult previous = createAuditResult(70.0, List.of(v1));
        AuditResult current = createAuditResult(70.0, List.of(v1, v2, v3));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
        assertThat(comparison.trend()).isEqualTo(AuditTrend.DEGRADED);
        assertThat(comparison.newViolations()).hasSize(2).containsExactlyInAnyOrder(v2, v3);
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(1);
        assertThat(comparison.previousViolationCount()).isEqualTo(1);
        assertThat(comparison.currentViolationCount()).isEqualTo(3);
    }

    @Test
    void shouldDetectDegraded_whenNewViolationsIntroduced() {
        // Given: New violations added
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        AuditResult previous = createAuditResult(100.0, List.of());
        AuditResult current = createAuditResult(70.0, List.of(v1));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isCloseTo(-30.0, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.DEGRADED);
        assertThat(comparison.newViolations()).containsExactly(v1);
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    @Test
    void shouldDetectDegraded_whenScoreAndViolationsBothDegraded() {
        // Given: Score decreased AND violations increased
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        AuditResult previous = createAuditResult(90.0, List.of(v1));
        AuditResult current = createAuditResult(70.0, List.of(v1, v2));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isCloseTo(-20.0, within(0.01));
        assertThat(comparison.trend()).isEqualTo(AuditTrend.DEGRADED);
        assertThat(comparison.newViolations()).containsExactly(v2);
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(1);
    }

    // === Same Score, Different Violations Tests ===

    @Test
    void shouldDetectChanges_whenSameScoreDifferentViolations() {
        // Given: Score unchanged but different violations
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Product");
        AuditResult previous = createAuditResult(75.0, List.of(v1));
        AuditResult current = createAuditResult(75.0, List.of(v2));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
        assertThat(comparison.trend()).isEqualTo(AuditTrend.STABLE); // Same count, same score
        assertThat(comparison.newViolations()).containsExactly(v2);
        assertThat(comparison.fixedViolations()).containsExactly(v1);
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    // === Violation Matching Tests ===

    @Test
    void shouldMatchViolations_bySameConstraintAndAffectedTypes() {
        // Given: Same violation appears in both audits (same constraint + affected types)
        Violation v1Previous = createViolation(CONSTRAINT_A, "com.example.Order", "com.example.Product");
        Violation v1Current = createViolation(CONSTRAINT_A, "com.example.Order", "com.example.Product");
        AuditResult previous = createAuditResult(75.0, List.of(v1Previous));
        AuditResult current = createAuditResult(75.0, List.of(v1Current));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(1);
    }

    @Test
    void shouldNotMatchViolations_whenConstraintDiffers() {
        // Given: Different constraints, same affected types
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_B, "com.example.Order");
        AuditResult previous = createAuditResult(75.0, List.of(v1));
        AuditResult current = createAuditResult(75.0, List.of(v2));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.newViolations()).containsExactly(v2);
        assertThat(comparison.fixedViolations()).containsExactly(v1);
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    @Test
    void shouldNotMatchViolations_whenAffectedTypesDiffer() {
        // Given: Same constraint, different affected types
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        Violation v2 = createViolation(CONSTRAINT_A, "com.example.Product");
        AuditResult previous = createAuditResult(75.0, List.of(v1));
        AuditResult current = createAuditResult(75.0, List.of(v2));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.newViolations()).containsExactly(v2);
        assertThat(comparison.fixedViolations()).containsExactly(v1);
        assertThat(comparison.unchangedViolationCount()).isEqualTo(0);
    }

    @Test
    void shouldMatchViolations_withMultipleAffectedTypes_orderIndependent() {
        // Given: Same violation with affected types in different order
        Violation v1Previous = createViolation(CONSTRAINT_A, "com.example.Order", "com.example.Product");
        Violation v1Current = createViolation(CONSTRAINT_A, "com.example.Product", "com.example.Order");
        AuditResult previous = createAuditResult(75.0, List.of(v1Previous));
        AuditResult current = createAuditResult(75.0, List.of(v1Current));

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then: Should match despite different order
        assertThat(comparison.newViolations()).isEmpty();
        assertThat(comparison.fixedViolations()).isEmpty();
        assertThat(comparison.unchangedViolationCount()).isEqualTo(1);
    }

    // === Edge Cases ===

    @Test
    void shouldHandleMissingHealthScoreMetric() {
        // Given: Audit result without health.score metric
        Violation v1 = createViolation(CONSTRAINT_A, "com.example.Order");
        AuditResult previous = new AuditResult(List.of(v1), Map.of(), BuildOutcome.SUCCESS);
        AuditResult current = new AuditResult(List.of(v1), Map.of(), BuildOutcome.SUCCESS);

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then: Should default to 0.0 score
        assertThat(comparison.previousScore()).isEqualTo(0.0);
        assertThat(comparison.currentScore()).isEqualTo(0.0);
        assertThat(comparison.scoreChange()).isEqualTo(0.0);
    }

    @Test
    void shouldHandleLargeNumberOfViolations() {
        // Given: 50 violations in each audit with various changes
        List<Violation> previousViolations = List.of(
                createViolation(CONSTRAINT_A, "Type1"),
                createViolation(CONSTRAINT_A, "Type2"),
                createViolation(CONSTRAINT_A, "Type3"),
                createViolation(CONSTRAINT_B, "Type1"),
                createViolation(CONSTRAINT_B, "Type2"));

        List<Violation> currentViolations = List.of(
                createViolation(CONSTRAINT_A, "Type1"), // Unchanged
                createViolation(CONSTRAINT_A, "Type2"), // Unchanged
                createViolation(CONSTRAINT_B, "Type3"), // New
                createViolation(CONSTRAINT_C, "Type1")); // New

        AuditResult previous = createAuditResult(60.0, previousViolations);
        AuditResult current = createAuditResult(65.0, currentViolations);

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.unchangedViolationCount()).isEqualTo(2); // Type1/A, Type2/A
        assertThat(comparison.fixedViolations()).hasSize(3); // Type3/A, Type1/B, Type2/B
        assertThat(comparison.newViolations()).hasSize(2); // Type3/B, Type1/C
        assertThat(comparison.previousViolationCount()).isEqualTo(5);
        assertThat(comparison.currentViolationCount()).isEqualTo(4);
    }

    @Test
    void shouldCalculateScoreChangePrecisely() {
        // Given: Precise score change
        AuditResult previous = createAuditResult(73.456, List.of());
        AuditResult current = createAuditResult(81.234, List.of());

        // When
        AuditComparison comparison = comparator.compare(previous, current);

        // Then
        assertThat(comparison.scoreChange()).isCloseTo(7.778, within(0.001));
    }

    // === Helper Methods ===

    /**
     * Creates a test audit result with the given health score and violations.
     */
    private AuditResult createAuditResult(double healthScore, List<Violation> violations) {
        Metric scoreMetric = Metric.of("health.score", healthScore, "points", "Overall health score");
        return new AuditResult(violations, Map.of("health.score", scoreMetric), BuildOutcome.SUCCESS);
    }

    /**
     * Creates a test violation with the given constraint and single affected type.
     */
    private Violation createViolation(ConstraintId constraint, String affectedType) {
        return Violation.builder(constraint)
                .severity(Severity.MAJOR)
                .message("Test violation")
                .affectedType(affectedType)
                .location(TEST_LOCATION)
                .build();
    }

    /**
     * Creates a test violation with the given constraint and multiple affected types.
     */
    private Violation createViolation(ConstraintId constraint, String... affectedTypes) {
        return Violation.builder(constraint)
                .severity(Severity.MAJOR)
                .message("Test violation")
                .affectedTypes(List.of(affectedTypes))
                .location(TEST_LOCATION)
                .build();
    }
}
