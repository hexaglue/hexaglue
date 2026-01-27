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
import static org.assertj.core.api.Assertions.within;

import io.hexaglue.arch.model.audit.SourceLocation;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.DebtEstimation;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration tests demonstrating how to use {@link DebtEstimator} with {@link AuditResult}.
 *
 * <p>These tests show real-world usage patterns for estimating technical debt
 * from audit violations.
 */
class DebtEstimatorIntegrationTest {

    private static final ConstraintId DDD_001 = ConstraintId.of("DDD-001");
    private static final ConstraintId DDD_002 = ConstraintId.of("DDD-002");
    private static final ConstraintId HEX_001 = ConstraintId.of("HEX-001");
    private static final SourceLocation TEST_LOCATION = SourceLocation.of("Test.java", 1, 1);

    @Test
    void shouldEstimateDebtFromAuditResult() {
        // Given: An audit result with multiple violations
        List<Violation> violations = List.of(
                createViolation(DDD_001, Severity.BLOCKER, "Circular aggregate dependency detected"),
                createViolation(DDD_002, Severity.CRITICAL, "Mutable value object detected"),
                createViolation(HEX_001, Severity.MAJOR, "Port not properly isolated"));

        AuditResult auditResult = new AuditResult(violations, Map.of(), BuildOutcome.FAIL);

        // When: Estimating debt from violations
        DebtEstimator estimator = new DebtEstimator(500.0);
        DebtEstimation debt = estimator.estimate(auditResult.violations());

        // Then: Should calculate based on severity mapping
        // BLOCKER (3.0) + CRITICAL (2.0) + MAJOR (0.5) = 5.5 days
        assertThat(debt.totalDays()).isEqualTo(5.5);
        assertThat(debt.totalCost()).isEqualTo(2750.0); // 5.5 * 500
        assertThat(debt.monthlyInterest()).isEqualTo(137.5); // 5.5 * 0.05 * 500
    }

    @Test
    void shouldHandleEmptyAuditResult() {
        // Given: An audit result with no violations (passed)
        AuditResult auditResult = new AuditResult(List.of(), Map.of(), BuildOutcome.SUCCESS);

        // When: Estimating debt
        DebtEstimator estimator = new DebtEstimator();
        DebtEstimation debt = estimator.estimate(auditResult.violations());

        // Then: Should have zero debt
        assertThat(debt.isZero()).isTrue();
        assertThat(debt.totalDays()).isEqualTo(0.0);
        assertThat(debt.totalCost()).isEqualTo(0.0);
        assertThat(debt.monthlyInterest()).isEqualTo(0.0);
    }

    @Test
    void shouldFilterByseverity_beforeEstimation() {
        // Given: Audit result with various severity levels
        List<Violation> violations = List.of(
                createViolation(DDD_001, Severity.BLOCKER, "Blocker issue"),
                createViolation(DDD_002, Severity.CRITICAL, "Critical issue"),
                createViolation(HEX_001, Severity.MAJOR, "Major issue"),
                createViolation(HEX_001, Severity.MINOR, "Minor issue"),
                createViolation(HEX_001, Severity.INFO, "Info issue"));

        AuditResult auditResult = new AuditResult(violations, Map.of(), BuildOutcome.FAIL);

        // When: Estimating debt only for high-severity violations
        List<Violation> highSeverityOnly = auditResult.violations().stream()
                .filter(v -> v.severity() == Severity.BLOCKER || v.severity() == Severity.CRITICAL)
                .toList();

        DebtEstimator estimator = new DebtEstimator(500.0);
        DebtEstimation debt = estimator.estimate(highSeverityOnly);

        // Then: Should only include BLOCKER + CRITICAL
        // BLOCKER (3.0) + CRITICAL (2.0) = 5.0 days
        assertThat(debt.totalDays()).isEqualTo(5.0);
        assertThat(debt.totalCost()).isEqualTo(2500.0);
    }

    @Test
    void shouldEstimateWithCustomCostPerDay() {
        // Given: High-cost team environment ($1000/day)
        List<Violation> violations =
                List.of(createViolation(DDD_001, Severity.BLOCKER, "Critical architectural issue"));

        AuditResult auditResult = new AuditResult(violations, Map.of(), BuildOutcome.FAIL);

        // When: Estimating with custom cost
        DebtEstimator estimator = new DebtEstimator(1000.0);
        DebtEstimation debt = estimator.estimate(auditResult.violations());

        // Then: Should use custom cost in calculation
        assertThat(debt.totalDays()).isEqualTo(3.0); // BLOCKER
        assertThat(debt.totalCost()).isEqualTo(3000.0); // 3.0 * 1000
        assertThat(debt.monthlyInterest()).isCloseTo(150.0, within(0.01)); // 3.0 * 0.05 * 1000
    }

    @Test
    void shouldCalculateTotalDebt_forLargeCodebase() {
        // Given: Large codebase with many violations
        List<Violation> violations = List.of(
                // 5 critical DDD violations
                createViolation(DDD_001, Severity.CRITICAL, "Issue 1"),
                createViolation(DDD_001, Severity.CRITICAL, "Issue 2"),
                createViolation(DDD_001, Severity.CRITICAL, "Issue 3"),
                createViolation(DDD_001, Severity.CRITICAL, "Issue 4"),
                createViolation(DDD_001, Severity.CRITICAL, "Issue 5"),
                // 10 major hexagonal violations
                createViolation(HEX_001, Severity.MAJOR, "Issue 1"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 2"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 3"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 4"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 5"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 6"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 7"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 8"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 9"),
                createViolation(HEX_001, Severity.MAJOR, "Issue 10"));

        AuditResult auditResult = new AuditResult(violations, Map.of(), BuildOutcome.FAIL);

        // When: Estimating total debt
        DebtEstimator estimator = new DebtEstimator(500.0);
        DebtEstimation debt = estimator.estimate(auditResult.violations());

        // Then: Should aggregate all violations
        // (5 * 2.0) + (10 * 0.5) = 10 + 5 = 15.0 days
        assertThat(debt.totalDays()).isEqualTo(15.0);
        assertThat(debt.totalCost()).isEqualTo(7500.0); // 15.0 * 500
        assertThat(debt.monthlyInterest()).isEqualTo(375.0); // 15.0 * 0.05 * 500
    }

    @Test
    void shouldProvideUsefulMetrics_forReporting() {
        // Given: Audit result
        List<Violation> violations = List.of(
                createViolation(DDD_001, Severity.BLOCKER, "Issue"), createViolation(DDD_002, Severity.MAJOR, "Issue"));

        AuditResult auditResult = new AuditResult(violations, Map.of(), BuildOutcome.FAIL);

        // When: Estimating debt
        DebtEstimator estimator = new DebtEstimator(500.0);
        DebtEstimation debt = estimator.estimate(auditResult.violations());

        // Then: Can use debt metrics for reporting
        // BLOCKER (3.0) + MAJOR (0.5) = 3.5 days
        assertThat(debt.totalDays()).isEqualTo(3.5);
        assertThat(debt.totalCost()).isEqualTo(1750.0);
        assertThat(debt.monthlyInterest()).isCloseTo(87.5, within(0.01));

        // Verify the debt estimation has useful values
        assertThat(debt.totalDays()).isGreaterThan(0.0);
        assertThat(debt.totalCost()).isGreaterThan(0.0);
        assertThat(debt.monthlyInterest()).isGreaterThan(0.0);
    }

    // === Helper Methods ===

    private Violation createViolation(ConstraintId constraintId, Severity severity, String message) {
        return Violation.builder(constraintId)
                .severity(severity)
                .message(message)
                .location(TEST_LOCATION)
                .build();
    }
}
