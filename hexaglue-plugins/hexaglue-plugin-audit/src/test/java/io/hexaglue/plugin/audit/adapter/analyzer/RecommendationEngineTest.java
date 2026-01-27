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

import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.ConstraintId;
import io.hexaglue.plugin.audit.domain.model.Recommendation;
import io.hexaglue.plugin.audit.domain.model.RecommendationPriority;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.arch.model.audit.SourceLocation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RecommendationEngine}.
 */
class RecommendationEngineTest {

    private static final SourceLocation TEST_LOCATION = SourceLocation.of("Test.java", 1, 1);

    private RecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
    }

    // === Constructor Tests ===

    @Test
    void shouldCreateEngine_withDefaultDebtEstimator() {
        // When
        RecommendationEngine defaultEngine = new RecommendationEngine();

        // Then
        assertThat(defaultEngine).isNotNull();
    }

    @Test
    void shouldCreateEngine_withCustomDebtEstimator() {
        // Given
        DebtEstimator customEstimator = new DebtEstimator(1000.0);

        // When
        RecommendationEngine customEngine = new RecommendationEngine(customEstimator);

        // Then
        assertThat(customEngine).isNotNull();
    }

    @Test
    void shouldRejectNullDebtEstimator() {
        // When/Then
        assertThatThrownBy(() -> new RecommendationEngine(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("debtEstimator required");
    }

    // === Empty Results Tests ===

    @Test
    void shouldReturnEmptyRecommendations_whenNoViolations() {
        // Given
        AuditResult result = new AuditResult(List.of(), Map.of(), BuildOutcome.SUCCESS);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).isEmpty();
    }

    @Test
    void shouldRejectNullAuditResult() {
        // When/Then
        assertThatThrownBy(() -> engine.generateRecommendations(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("result required");
    }

    // === Single Violation Tests ===

    @Test
    void shouldGenerateRecommendation_forSingleBlockerViolation() {
        // Given
        Violation violation = createViolation(
                ConstraintId.of("ddd:aggregate-repository"), Severity.BLOCKER, "Aggregate without repository", "Order");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);

        Recommendation rec = recommendations.get(0);
        assertThat(rec.priority()).isEqualTo(RecommendationPriority.IMMEDIATE);
        assertThat(rec.title()).contains("Aggregate Repository");
        assertThat(rec.description()).contains("ddd:aggregate-repository").contains("Order");
        assertThat(rec.affectedTypes()).containsExactly("Order");
        assertThat(rec.estimatedEffort()).isEqualTo(3.0); // BLOCKER = 3.0 days
        assertThat(rec.expectedImpact()).isNotBlank();
        assertThat(rec.relatedViolations()).containsExactly(ConstraintId.of("ddd:aggregate-repository"));
    }

    @Test
    void shouldGenerateRecommendation_forSingleCriticalViolation() {
        // Given
        Violation violation = createViolation(
                ConstraintId.of("ddd:value-object-immutable"), Severity.CRITICAL, "Mutable value object", "Address");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);

        Recommendation rec = recommendations.get(0);
        assertThat(rec.priority()).isEqualTo(RecommendationPriority.IMMEDIATE);
        assertThat(rec.estimatedEffort()).isEqualTo(2.0); // CRITICAL = 2.0 days
    }

    @Test
    void shouldGenerateRecommendation_forSingleMajorViolation() {
        // Given
        Violation violation =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);

        Recommendation rec = recommendations.get(0);
        assertThat(rec.priority()).isEqualTo(RecommendationPriority.MEDIUM_TERM);
        assertThat(rec.estimatedEffort()).isEqualTo(0.5); // MAJOR = 0.5 days
    }

    @Test
    void shouldGenerateRecommendation_forSingleMinorViolation() {
        // Given
        Violation violation = createViolation(
                ConstraintId.of("ddd:naming-convention"), Severity.MINOR, "Non-standard name", "OrderInfo");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);

        Recommendation rec = recommendations.get(0);
        assertThat(rec.priority()).isEqualTo(RecommendationPriority.LOW);
        assertThat(rec.estimatedEffort()).isEqualTo(0.25); // MINOR = 0.25 days
    }

    // === Priority Tests ===

    @Test
    void shouldAssignImmediatePriority_forArchitecturalViolations() {
        // Given: Hexagonal architecture violation (even if MAJOR)
        Violation violation = createViolation(
                ConstraintId.of("hexagonal:dependency-direction"), Severity.MAJOR, "Wrong dependency", "OrderService");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).priority()).isEqualTo(RecommendationPriority.IMMEDIATE);
    }

    @Test
    void shouldAssignShortTermPriority_forMajorViolationsAffectingMultipleTypes() {
        // Given: 3+ types affected (threshold)
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");
        Violation v2 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Customer");
        Violation v3 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Product");

        AuditResult result = createResult(v1, v2, v3);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        Recommendation rec = recommendations.get(0);
        assertThat(rec.priority()).isEqualTo(RecommendationPriority.SHORT_TERM);
        assertThat(rec.affectedTypes()).hasSize(3);
    }

    @Test
    void shouldAssignMediumTermPriority_forMajorViolationsAffectingFewTypes() {
        // Given: Only 2 types affected
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");
        Violation v2 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Customer");

        AuditResult result = createResult(v1, v2);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        Recommendation rec = recommendations.get(0);
        assertThat(rec.priority()).isEqualTo(RecommendationPriority.MEDIUM_TERM);
        assertThat(rec.affectedTypes()).hasSize(2);
    }

    @Test
    void shouldAssignLowPriority_forInfoViolations() {
        // Given
        Violation violation = createViolation(
                ConstraintId.of("metrics:aggregate-count"), Severity.INFO, "10 aggregates found", "Order");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).priority()).isEqualTo(RecommendationPriority.LOW);
    }

    // === Grouping Tests ===

    @Test
    void shouldGroupViolations_bySameConstraint() {
        // Given: Multiple violations of same constraint
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");
        Violation v2 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Customer");

        AuditResult result = createResult(v1, v2);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then: Should create single recommendation
        assertThat(recommendations).hasSize(1);

        Recommendation rec = recommendations.get(0);
        assertThat(rec.affectedTypes()).containsExactlyInAnyOrder("Order", "Customer");
        assertThat(rec.estimatedEffort()).isEqualTo(1.0); // 2 * 0.5 days
        assertThat(rec.title()).contains("(2 violations)");
    }

    @Test
    void shouldCreateSeparateRecommendations_forDifferentConstraints() {
        // Given: Different constraints
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");
        Violation v2 = createViolation(
                ConstraintId.of("ddd:value-object-immutable"), Severity.CRITICAL, "Mutable value object", "Address");

        AuditResult result = createResult(v1, v2);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then: Should create two recommendations
        assertThat(recommendations).hasSize(2);

        // Verify separate recommendations
        assertThat(recommendations).anyMatch(r -> r.title().contains("Value Object Immutable"));
        assertThat(recommendations).anyMatch(r -> r.title().contains("Aggregate Size"));
    }

    @Test
    void shouldDeduplicateAffectedTypes_inGroupedRecommendations() {
        // Given: Same type affected multiple times
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");
        Violation v2 = createViolation(
                ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Too many entities", "Order"); // Same type

        AuditResult result = createResult(v1, v2);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).affectedTypes()).containsExactly("Order"); // No duplicates
    }

    // === Sorting Tests ===

    @Test
    void shouldSortRecommendations_byPriority() {
        // Given: Mixed priorities
        Violation blocker =
                createViolation(ConstraintId.of("ddd:aggregate-deps"), Severity.BLOCKER, "Circular deps", "Order");
        Violation major =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Customer");
        Violation minor =
                createViolation(ConstraintId.of("ddd:naming"), Severity.MINOR, "Non-standard name", "Product");

        AuditResult result = createResult(major, minor, blocker); // Deliberately unordered

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then: Should be sorted IMMEDIATE, MEDIUM_TERM, LOW
        assertThat(recommendations).hasSize(3);
        assertThat(recommendations.get(0).priority()).isEqualTo(RecommendationPriority.IMMEDIATE);
        assertThat(recommendations.get(1).priority()).isEqualTo(RecommendationPriority.MEDIUM_TERM);
        assertThat(recommendations.get(2).priority()).isEqualTo(RecommendationPriority.LOW);
    }

    // === Content Generation Tests ===

    @Test
    void shouldGenerateTitle_withViolationCount() {
        // Given: Multiple violations
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");
        Violation v2 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Customer");
        Violation v3 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Product");

        AuditResult result = createResult(v1, v2, v3);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).title()).isEqualTo("Aggregate Size (3 violations)");
    }

    @Test
    void shouldGenerateTitle_withoutCount_forSingleViolation() {
        // Given
        Violation violation =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).title()).isEqualTo("Aggregate Size");
    }

    @Test
    void shouldGenerateDescription_withConstraintIdAndTypes() {
        // Given
        Violation violation =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Order");

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        Recommendation rec = recommendations.get(0);
        assertThat(rec.description())
                .contains("ddd:aggregate-size")
                .contains("Order")
                .contains("Large aggregate");
    }

    @Test
    void shouldLimitAffectedTypes_inDescription() {
        // Given: 6 affected types (more than limit of 5)
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Type1");
        Violation v2 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Type2");
        Violation v3 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Type3");
        Violation v4 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Type4");
        Violation v5 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Type5");
        Violation v6 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Type6");

        AuditResult result = createResult(v1, v2, v3, v4, v5, v6);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        String description = recommendations.get(0).description();
        assertThat(description).contains("... and 1 more"); // 6 types, show 5 + "1 more"
    }

    @Test
    void shouldLimitViolationMessages_inDescription() {
        // Given: 5 violations (more than limit of 3)
        Violation v1 = createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Message 1", "Type1");
        Violation v2 = createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Message 2", "Type2");
        Violation v3 = createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Message 3", "Type3");
        Violation v4 = createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Message 4", "Type4");
        Violation v5 = createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Message 5", "Type5");

        AuditResult result = createResult(v1, v2, v3, v4, v5);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        String description = recommendations.get(0).description();
        assertThat(description).contains("... and 2 more violations"); // 5 violations, show 3 + "2 more"
    }

    @Test
    void shouldGenerateExpectedImpact_basedOnPriority() {
        // Given: Different priorities
        Violation blocker =
                createViolation(ConstraintId.of("ddd:aggregate-deps"), Severity.BLOCKER, "Circular deps", "Order");
        Violation major =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Customer");

        AuditResult result = createResult(blocker, major);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        Recommendation immediateRec = recommendations.stream()
                .filter(r -> r.priority() == RecommendationPriority.IMMEDIATE)
                .findFirst()
                .get();
        Recommendation mediumRec = recommendations.stream()
                .filter(r -> r.priority() == RecommendationPriority.MEDIUM_TERM)
                .findFirst()
                .get();

        assertThat(immediateRec.expectedImpact()).contains("architectural integrity");
        assertThat(mediumRec.expectedImpact()).contains("code quality");
    }

    // === Edge Cases ===

    @Test
    void shouldHandleViolation_withoutAffectedTypes() {
        // Given: Violation without affected types
        Violation violation = Violation.builder(ConstraintId.of("ddd:general-issue"))
                .severity(Severity.MAJOR)
                .message("General issue")
                .location(TEST_LOCATION)
                .build(); // No affected types

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).affectedTypes()).isEmpty();
    }

    @Test
    void shouldHandleViolation_withMultipleAffectedTypes() {
        // Given: Single violation with multiple types
        Violation violation = Violation.builder(ConstraintId.of("ddd:aggregate-boundary"))
                .severity(Severity.MAJOR)
                .message("Boundary violation")
                .affectedTypes(List.of("Order", "Customer", "Product"))
                .location(TEST_LOCATION)
                .build();

        AuditResult result = createResult(violation);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).affectedTypes()).containsExactlyInAnyOrder("Order", "Customer", "Product");
    }

    @Test
    void shouldSortAffectedTypes_alphabetically() {
        // Given: Types in non-alphabetical order
        Violation v1 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Zebra");
        Violation v2 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Apple");
        Violation v3 =
                createViolation(ConstraintId.of("ddd:aggregate-size"), Severity.MAJOR, "Large aggregate", "Mango");

        AuditResult result = createResult(v1, v2, v3);

        // When
        List<Recommendation> recommendations = engine.generateRecommendations(result);

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).affectedTypes()).containsExactly("Apple", "Mango", "Zebra");
    }

    // === Helper Methods ===

    /**
     * Creates a violation with the given parameters.
     */
    private Violation createViolation(
            ConstraintId constraintId, Severity severity, String message, String affectedType) {
        return Violation.builder(constraintId)
                .severity(severity)
                .message(message)
                .affectedType(affectedType)
                .location(TEST_LOCATION)
                .build();
    }

    /**
     * Creates an AuditResult with the given violations.
     */
    private AuditResult createResult(Violation... violations) {
        return new AuditResult(List.of(violations), Map.of(), BuildOutcome.FAIL);
    }
}
