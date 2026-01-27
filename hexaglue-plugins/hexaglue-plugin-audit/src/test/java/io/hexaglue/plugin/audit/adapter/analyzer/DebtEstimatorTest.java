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

import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DebtEstimation;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebtEstimator}.
 */
class DebtEstimatorTest {

    private static final double DEFAULT_COST_PER_DAY = 500.0;
    private static final ConstraintId TEST_CONSTRAINT = ConstraintId.of("TEST");
    private static final SourceLocation TEST_LOCATION = SourceLocation.of("Test.java", 1, 1);

    @Test
    void shouldUseDefaultCostPerDay_whenNotSpecified() {
        // When
        DebtEstimator estimator = new DebtEstimator();

        // Then
        assertThat(estimator.costPerDay()).isEqualTo(DEFAULT_COST_PER_DAY);
    }

    @Test
    void shouldUseCustomCostPerDay() {
        // When
        DebtEstimator estimator = new DebtEstimator(1000.0);

        // Then
        assertThat(estimator.costPerDay()).isEqualTo(1000.0);
    }

    @Test
    void shouldRejectNegativeCostPerDay() {
        // When/Then
        assertThatThrownBy(() -> new DebtEstimator(-100.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("costPerDay cannot be negative");
    }

    @Test
    void shouldReturnZeroDebt_whenNoViolations() {
        // Given
        DebtEstimator estimator = new DebtEstimator();

        // When
        DebtEstimation debt = estimator.estimate(List.of());

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
        assertThat(debt.isZero()).isTrue();
    }

    @Test
    void shouldRejectNullViolations() {
        // Given
        DebtEstimator estimator = new DebtEstimator();

        // When/Then
        assertThatThrownBy(() -> estimator.estimate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("violations required");
    }

    @Test
    void shouldEstimateDebt_forSingleBlockerViolation() {
        // Given: BLOCKER = 3.0 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(createViolation(Severity.BLOCKER));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(3.0);
        assertThat(debt.totalCost()).isEqualTo(1500.0); // 3.0 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(75.0, within(0.01)); // 3.0 * 0.05 * 500
    }

    @Test
    void shouldEstimateDebt_forSingleCriticalViolation() {
        // Given: CRITICAL = 2.0 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(createViolation(Severity.CRITICAL));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(2.0);
        assertThat(debt.totalCost()).isEqualTo(1000.0); // 2.0 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(50.0, within(0.01)); // 2.0 * 0.05 * 500
    }

    @Test
    void shouldEstimateDebt_forSingleMajorViolation() {
        // Given: MAJOR = 0.5 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(createViolation(Severity.MAJOR));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.5);
        assertThat(debt.totalCost()).isEqualTo(250.0); // 0.5 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(12.5, within(0.01)); // 0.5 * 0.05 * 500
    }

    @Test
    void shouldEstimateDebt_forSingleMinorViolation() {
        // Given: MINOR = 0.25 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(createViolation(Severity.MINOR));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.25);
        assertThat(debt.totalCost()).isEqualTo(125.0); // 0.25 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(6.25, within(0.01)); // 0.25 * 0.05 * 500
    }

    @Test
    void shouldEstimateDebt_forSingleInfoViolation() {
        // Given: INFO = 0.0 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(createViolation(Severity.INFO));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
        assertThat(debt.isZero()).isTrue();
    }

    @Test
    void shouldEstimateDebt_forMultipleViolationsOfSameSeverity() {
        // Given: 3 MAJOR violations = 3 * 0.5 = 1.5 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(
                createViolation(Severity.MAJOR), createViolation(Severity.MAJOR), createViolation(Severity.MAJOR));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(1.5);
        assertThat(debt.totalCost()).isEqualTo(750.0); // 1.5 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(37.5, within(0.01)); // 1.5 * 0.05 * 500
    }

    @Test
    void shouldEstimateDebt_forMixedSeverityViolations() {
        // Given: BLOCKER (3.0) + CRITICAL (2.0) + MAJOR (0.5) + MINOR (0.25) = 5.75 days
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(
                createViolation(Severity.BLOCKER),
                createViolation(Severity.CRITICAL),
                createViolation(Severity.MAJOR),
                createViolation(Severity.MINOR));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(5.75);
        assertThat(debt.totalCost()).isEqualTo(2875.0); // 5.75 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(143.75, within(0.01)); // 5.75 * 0.05 * 500
    }

    @Test
    void shouldUseCustomCostPerDay_inCalculation() {
        // Given: Custom cost = $1000/day, 1 BLOCKER (3.0 days)
        DebtEstimator estimator = new DebtEstimator(1000.0);
        List<Violation> violations = List.of(createViolation(Severity.BLOCKER));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(3.0);
        assertThat(debt.totalCost()).isEqualTo(3000.0); // 3.0 * 1000
        assertThat(debt.monthlyInterest()).isCloseTo(150.0, within(0.01)); // 3.0 * 0.05 * 1000
    }

    @Test
    void shouldUseZeroCostPerDay() {
        // Given: Zero cost per day
        DebtEstimator estimator = new DebtEstimator(0.0);
        List<Violation> violations = List.of(createViolation(Severity.BLOCKER));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(3.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
    }

    @Test
    void shouldHandleLargeNumberOfViolations() {
        // Given: 100 violations (50 MAJOR + 50 MINOR)
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            violations.add(createViolation(Severity.MAJOR)); // 0.5 each
            violations.add(createViolation(Severity.MINOR)); // 0.25 each
        }

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then: (50 * 0.5) + (50 * 0.25) = 25 + 12.5 = 37.5 days
        assertThat(debt.totalDays()).isEqualTo(37.5);
        assertThat(debt.totalCost()).isEqualTo(18750.0); // 37.5 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(937.5, within(0.01)); // 37.5 * 0.05 * 500
    }

    @Test
    void shouldUseCustomEffortMapping() {
        // Given: Custom effort mapping where MAJOR = 1.0 day instead of 0.5
        Map<Severity, Double> customEffort = Map.of(
                Severity.BLOCKER, 3.0,
                Severity.CRITICAL, 2.0,
                Severity.MAJOR, 1.0, // Custom value
                Severity.MINOR, 0.25,
                Severity.INFO, 0.0);

        DebtEstimator estimator = new DebtEstimator(500.0, customEffort);
        List<Violation> violations = List.of(createViolation(Severity.MAJOR));

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then: Should use custom 1.0 days instead of default 0.5
        assertThat(debt.totalDays()).isEqualTo(1.0);
        assertThat(debt.totalCost()).isEqualTo(500.0);
        assertThat(debt.monthlyInterest()).isCloseTo(25.0, within(0.01));
    }

    @Test
    void shouldRejectNullEffortMapping() {
        // When/Then
        assertThatThrownBy(() -> new DebtEstimator(500.0, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("effortPerSeverity required");
    }

    @Test
    void shouldHandleUnknownSeverity_asZeroEffort() {
        // Given: Custom effort map missing some severities
        Map<Severity, Double> incompleteEffort = Map.of(Severity.BLOCKER, 3.0);

        DebtEstimator estimator = new DebtEstimator(500.0, incompleteEffort);
        List<Violation> violations = List.of(createViolation(Severity.MAJOR)); // Not in map

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then: Should default to 0.0 for unknown severity
        assertThat(debt.totalDays()).isEqualTo(0.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
    }

    @Test
    void shouldCalculateMonthlyInterestAccurately() {
        // Given: 10 days of debt at $500/day
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(
                createViolation(Severity.BLOCKER), // 3.0
                createViolation(Severity.BLOCKER), // 3.0
                createViolation(Severity.CRITICAL), // 2.0
                createViolation(Severity.CRITICAL) // 2.0
                ); // Total = 10.0 days

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(10.0);
        assertThat(debt.totalCost()).isEqualTo(5000.0); // 10.0 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(250.0, within(0.01)); // 10.0 * 0.05 * 500
    }

    @Test
    void shouldCalculateDebt_withMixedInfoAndRealViolations() {
        // Given: Mix of INFO (0.0) and real violations
        DebtEstimator estimator = new DebtEstimator(500.0);
        List<Violation> violations = List.of(
                createViolation(Severity.INFO), // 0.0
                createViolation(Severity.MAJOR), // 0.5
                createViolation(Severity.INFO), // 0.0
                createViolation(Severity.MINOR) // 0.25
                ); // Total = 0.75 days

        // When
        DebtEstimation debt = estimator.estimate(violations);

        // Then
        assertThat(debt.totalDays()).isEqualTo(0.75);
        assertThat(debt.totalCost()).isEqualTo(375.0); // 0.75 * 500
        assertThat(debt.monthlyInterest()).isCloseTo(18.75, within(0.01)); // 0.75 * 0.05 * 500
    }

    // === Helper Methods ===

    /**
     * Creates a test violation with the given severity.
     */
    private Violation createViolation(Severity severity) {
        return Violation.builder(TEST_CONSTRAINT)
                .severity(severity)
                .message("Test violation")
                .location(TEST_LOCATION)
                .build();
    }
}
