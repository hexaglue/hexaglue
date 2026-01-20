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

package io.hexaglue.arch.model.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClassificationReport}.
 *
 * @since 4.1.0
 */
@DisplayName("ClassificationReport")
class ClassificationReportTest {

    private static final Instant TEST_TIME = Instant.parse("2026-01-15T10:30:00Z");
    private static final TypeId ORDER_TYPE_ID = TypeId.of("com.example.Order");
    private static final TypeId ITEM_TYPE_ID = TypeId.of("com.example.Item");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create report with all parameters")
        void shouldCreateWithAllParameters() {
            // given
            ClassificationStats stats =
                    ClassificationStats.of(10, 8, 2, Map.of(ArchKind.ENTITY, 8), Map.of(ConfidenceLevel.HIGH, 8), 1);

            UnclassifiedType unclassified = createUnclassifiedType(ORDER_TYPE_ID, UnclassifiedCategory.AMBIGUOUS);
            Map<UnclassifiedCategory, List<UnclassifiedType>> unclassifiedByCategory =
                    Map.of(UnclassifiedCategory.AMBIGUOUS, List.of(unclassified));

            List<ClassificationConflict> conflicts = List.of(ClassificationConflict.of(
                    ORDER_TYPE_ID,
                    "Order",
                    List.of(ClassificationConflict.ConflictingContribution.of(
                            ArchKind.AGGREGATE_ROOT, "CriterionA", 100, ConfidenceLevel.HIGH)),
                    "Resolved",
                    TEST_TIME));

            List<PrioritizedRemediation> remediations = List.of(new PrioritizedRemediation(
                    2, ORDER_TYPE_ID, "Order", UnclassifiedCategory.AMBIGUOUS, "Check", List.of()));

            // when
            ClassificationReport report =
                    new ClassificationReport(stats, unclassifiedByCategory, conflicts, remediations, TEST_TIME);

            // then
            assertThat(report.stats()).isEqualTo(stats);
            assertThat(report.unclassifiedByCategory()).hasSize(1);
            assertThat(report.conflicts()).hasSize(1);
            assertThat(report.remediations()).hasSize(1);
            assertThat(report.generatedAt()).isEqualTo(TEST_TIME);
        }

        @Test
        @DisplayName("should reject null stats")
        void shouldRejectNullStats() {
            assertThatThrownBy(() -> new ClassificationReport(null, Map.of(), List.of(), List.of(), TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("stats");
        }

        @Test
        @DisplayName("should reject null unclassifiedByCategory")
        void shouldRejectNullUnclassifiedByCategory() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);

            // then
            assertThatThrownBy(() -> new ClassificationReport(stats, null, List.of(), List.of(), TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("unclassifiedByCategory");
        }

        @Test
        @DisplayName("should reject null conflicts")
        void shouldRejectNullConflicts() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);

            // then
            assertThatThrownBy(() -> new ClassificationReport(stats, Map.of(), null, List.of(), TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("conflicts");
        }

        @Test
        @DisplayName("should reject null remediations")
        void shouldRejectNullRemediations() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);

            // then
            assertThatThrownBy(() -> new ClassificationReport(stats, Map.of(), List.of(), null, TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("remediations");
        }

        @Test
        @DisplayName("should reject null generatedAt")
        void shouldRejectNullGeneratedAt() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);

            // then
            assertThatThrownBy(() -> new ClassificationReport(stats, Map.of(), List.of(), List.of(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("generatedAt");
        }

        @Test
        @DisplayName("should make defensive copy of conflicts")
        void shouldMakeDefensiveCopyOfConflicts() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);
            var mutableList = new java.util.ArrayList<ClassificationConflict>();
            mutableList.add(ClassificationConflict.of(ORDER_TYPE_ID, "Order", List.of(), "Resolved", TEST_TIME));

            // when
            ClassificationReport report = new ClassificationReport(stats, Map.of(), mutableList, List.of(), TEST_TIME);
            mutableList.add(ClassificationConflict.of(ITEM_TYPE_ID, "Item", List.of(), "Resolved", TEST_TIME));

            // then
            assertThat(report.conflicts()).hasSize(1);
        }

        @Test
        @DisplayName("should make defensive copy of remediations")
        void shouldMakeDefensiveCopyOfRemediations() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);
            var mutableList = new java.util.ArrayList<PrioritizedRemediation>();
            mutableList.add(new PrioritizedRemediation(
                    2, ORDER_TYPE_ID, "Order", UnclassifiedCategory.AMBIGUOUS, "Check", List.of()));

            // when
            ClassificationReport report = new ClassificationReport(stats, Map.of(), List.of(), mutableList, TEST_TIME);
            mutableList.add(new PrioritizedRemediation(
                    3, ITEM_TYPE_ID, "Item", UnclassifiedCategory.UNKNOWN, "Check", List.of()));

            // then
            assertThat(report.remediations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("actionRequired()")
    class ActionRequired {

        @Test
        @DisplayName("should return unclassified types that require action")
        void shouldReturnActionRequiredTypes() {
            // given
            UnclassifiedType conflicting = createUnclassifiedType(ORDER_TYPE_ID, UnclassifiedCategory.CONFLICTING);
            UnclassifiedType ambiguous = createUnclassifiedType(ITEM_TYPE_ID, UnclassifiedCategory.AMBIGUOUS);
            UnclassifiedType utility =
                    createUnclassifiedType(TypeId.of("com.example.Utils"), UnclassifiedCategory.UTILITY);

            Map<UnclassifiedCategory, List<UnclassifiedType>> unclassifiedByCategory = Map.of(
                    UnclassifiedCategory.CONFLICTING, List.of(conflicting),
                    UnclassifiedCategory.AMBIGUOUS, List.of(ambiguous),
                    UnclassifiedCategory.UTILITY, List.of(utility));

            ClassificationStats stats = ClassificationStats.of(10, 7, 3, Map.of(), Map.of(), 1);

            ClassificationReport report =
                    new ClassificationReport(stats, unclassifiedByCategory, List.of(), List.of(), TEST_TIME);

            // when
            List<UnclassifiedType> actionRequired = report.actionRequired();

            // then
            assertThat(actionRequired)
                    .hasSize(2)
                    .extracting(UnclassifiedType::id)
                    .containsExactlyInAnyOrder(ORDER_TYPE_ID, ITEM_TYPE_ID);
        }

        @Test
        @DisplayName("should return empty list when no action required")
        void shouldReturnEmptyWhenNoActionRequired() {
            // given
            UnclassifiedType utility =
                    createUnclassifiedType(TypeId.of("com.example.Utils"), UnclassifiedCategory.UTILITY);

            Map<UnclassifiedCategory, List<UnclassifiedType>> unclassifiedByCategory =
                    Map.of(UnclassifiedCategory.UTILITY, List.of(utility));

            ClassificationStats stats = ClassificationStats.of(10, 9, 1, Map.of(), Map.of(), 0);

            ClassificationReport report =
                    new ClassificationReport(stats, unclassifiedByCategory, List.of(), List.of(), TEST_TIME);

            // when
            List<UnclassifiedType> actionRequired = report.actionRequired();

            // then
            assertThat(actionRequired).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasIssues()")
    class HasIssues {

        @Test
        @DisplayName("should return true when there are unclassified types")
        void shouldReturnTrueWhenUnclassified() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);

            ClassificationReport report = new ClassificationReport(stats, Map.of(), List.of(), List.of(), TEST_TIME);

            // then
            assertThat(report.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("should return true when there are conflicts")
        void shouldReturnTrueWhenConflicts() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 10, 0, Map.of(), Map.of(), 2);

            ClassificationReport report = new ClassificationReport(stats, Map.of(), List.of(), List.of(), TEST_TIME);

            // then
            assertThat(report.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("should return false when all classified without conflicts")
        void shouldReturnFalseWhenNoIssues() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 10, 0, Map.of(), Map.of(), 0);

            ClassificationReport report = new ClassificationReport(stats, Map.of(), List.of(), List.of(), TEST_TIME);

            // then
            assertThat(report.hasIssues()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build report with builder")
        void shouldBuildWithBuilder() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 1);

            // when
            ClassificationReport report = ClassificationReport.builder()
                    .stats(stats)
                    .generatedAt(TEST_TIME)
                    .build();

            // then
            assertThat(report.stats()).isEqualTo(stats);
            assertThat(report.generatedAt()).isEqualTo(TEST_TIME);
            assertThat(report.conflicts()).isEmpty();
            assertThat(report.remediations()).isEmpty();
        }

        @Test
        @DisplayName("should add conflicts to builder")
        void shouldAddConflictsToBuilder() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 1);
            ClassificationConflict conflict =
                    ClassificationConflict.of(ORDER_TYPE_ID, "Order", List.of(), "Resolved", TEST_TIME);

            // when
            ClassificationReport report = ClassificationReport.builder()
                    .stats(stats)
                    .addConflict(conflict)
                    .generatedAt(TEST_TIME)
                    .build();

            // then
            assertThat(report.conflicts()).containsExactly(conflict);
        }

        @Test
        @DisplayName("should add unclassified to builder")
        void shouldAddUnclassifiedToBuilder() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);
            UnclassifiedType unclassified = createUnclassifiedType(ORDER_TYPE_ID, UnclassifiedCategory.AMBIGUOUS);

            // when
            ClassificationReport report = ClassificationReport.builder()
                    .stats(stats)
                    .addUnclassified(unclassified)
                    .generatedAt(TEST_TIME)
                    .build();

            // then
            assertThat(report.unclassifiedByCategory()).containsKey(UnclassifiedCategory.AMBIGUOUS);
            assertThat(report.unclassifiedByCategory().get(UnclassifiedCategory.AMBIGUOUS))
                    .containsExactly(unclassified);
        }

        @Test
        @DisplayName("should add remediations to builder")
        void shouldAddRemediationsToBuilder() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, Map.of(), Map.of(), 0);
            PrioritizedRemediation remediation = new PrioritizedRemediation(
                    2, ORDER_TYPE_ID, "Order", UnclassifiedCategory.AMBIGUOUS, "Check", List.of());

            // when
            ClassificationReport report = ClassificationReport.builder()
                    .stats(stats)
                    .addRemediation(remediation)
                    .generatedAt(TEST_TIME)
                    .build();

            // then
            assertThat(report.remediations()).containsExactly(remediation);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable conflicts list")
        void shouldReturnImmutableConflicts() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 10, 0, Map.of(), Map.of(), 0);
            ClassificationReport report = new ClassificationReport(stats, Map.of(), List.of(), List.of(), TEST_TIME);

            // then
            assertThatThrownBy(() -> report.conflicts()
                            .add(ClassificationConflict.of(ORDER_TYPE_ID, "Order", List.of(), "Resolved", TEST_TIME)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable remediations list")
        void shouldReturnImmutableRemediations() {
            // given
            ClassificationStats stats = ClassificationStats.of(10, 10, 0, Map.of(), Map.of(), 0);
            ClassificationReport report = new ClassificationReport(stats, Map.of(), List.of(), List.of(), TEST_TIME);

            // then
            assertThatThrownBy(() -> report.remediations()
                            .add(new PrioritizedRemediation(
                                    2, ORDER_TYPE_ID, "Order", UnclassifiedCategory.AMBIGUOUS, "Check", List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private UnclassifiedType createUnclassifiedType(TypeId typeId, UnclassifiedCategory category) {
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS).build();
        ClassificationTrace trace = ClassificationTrace.unclassified("Test unclassified type", List.of());
        return UnclassifiedType.of(typeId, structure, trace, category);
    }
}
