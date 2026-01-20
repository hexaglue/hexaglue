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

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.report.ClassificationConflict.ConflictingContribution;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClassificationConflict}.
 *
 * @since 4.1.0
 */
@DisplayName("ClassificationConflict")
class ClassificationConflictTest {

    private static final TypeId ORDER_TYPE_ID = TypeId.of("com.example.Order");
    private static final Instant TEST_TIME = Instant.parse("2026-01-15T10:30:00Z");

    @Nested
    @DisplayName("ConflictingContribution")
    class ConflictingContributionTests {

        @Test
        @DisplayName("should create contribution with all parameters")
        void shouldCreateWithAllParameters() {
            // when
            ConflictingContribution contribution = new ConflictingContribution(
                    ArchKind.AGGREGATE_ROOT, "ExplicitAggregateRootCriterion", 100, ConfidenceLevel.HIGH);

            // then
            assertThat(contribution.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
            assertThat(contribution.criteriaName()).isEqualTo("ExplicitAggregateRootCriterion");
            assertThat(contribution.priority()).isEqualTo(100);
            assertThat(contribution.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        @DisplayName("should reject null kind")
        void shouldRejectNullKind() {
            assertThatThrownBy(() -> new ConflictingContribution(null, "SomeCriterion", 100, ConfidenceLevel.HIGH))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("kind");
        }

        @Test
        @DisplayName("should reject null criteriaName")
        void shouldRejectNullCriteriaName() {
            assertThatThrownBy(() -> new ConflictingContribution(ArchKind.ENTITY, null, 100, ConfidenceLevel.HIGH))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("criteriaName");
        }

        @Test
        @DisplayName("should reject null confidence")
        void shouldRejectNullConfidence() {
            assertThatThrownBy(() -> new ConflictingContribution(ArchKind.ENTITY, "SomeCriterion", 100, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("confidence");
        }

        @Test
        @DisplayName("should create via factory method")
        void shouldCreateViaFactory() {
            // when
            ConflictingContribution contribution = ConflictingContribution.of(
                    ArchKind.VALUE_OBJECT, "RecordValueObjectCriterion", 70, ConfidenceLevel.MEDIUM);

            // then
            assertThat(contribution.kind()).isEqualTo(ArchKind.VALUE_OBJECT);
            assertThat(contribution.criteriaName()).isEqualTo("RecordValueObjectCriterion");
            assertThat(contribution.priority()).isEqualTo(70);
            assertThat(contribution.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }
    }

    @Nested
    @DisplayName("ClassificationConflict Construction")
    class Construction {

        @Test
        @DisplayName("should create conflict with all parameters")
        void shouldCreateWithAllParameters() {
            // given
            List<ConflictingContribution> contributions = List.of(
                    new ConflictingContribution(ArchKind.AGGREGATE_ROOT, "CriterionA", 100, ConfidenceLevel.HIGH),
                    new ConflictingContribution(ArchKind.ENTITY, "CriterionB", 100, ConfidenceLevel.HIGH));

            // when
            ClassificationConflict conflict = new ClassificationConflict(
                    ORDER_TYPE_ID, "Order", contributions, "Resolved to AGGREGATE_ROOT", TEST_TIME);

            // then
            assertThat(conflict.typeId()).isEqualTo(ORDER_TYPE_ID);
            assertThat(conflict.typeName()).isEqualTo("Order");
            assertThat(conflict.contributions()).hasSize(2);
            assertThat(conflict.resolution()).isEqualTo("Resolved to AGGREGATE_ROOT");
            assertThat(conflict.detectedAt()).isEqualTo(TEST_TIME);
        }

        @Test
        @DisplayName("should reject null typeId")
        void shouldRejectNullTypeId() {
            assertThatThrownBy(() -> new ClassificationConflict(null, "Order", List.of(), "resolution", TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeId");
        }

        @Test
        @DisplayName("should reject null typeName")
        void shouldRejectNullTypeName() {
            assertThatThrownBy(
                            () -> new ClassificationConflict(ORDER_TYPE_ID, null, List.of(), "resolution", TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeName");
        }

        @Test
        @DisplayName("should reject null contributions")
        void shouldRejectNullContributions() {
            assertThatThrownBy(() -> new ClassificationConflict(ORDER_TYPE_ID, "Order", null, "resolution", TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("contributions");
        }

        @Test
        @DisplayName("should reject null resolution")
        void shouldRejectNullResolution() {
            assertThatThrownBy(() -> new ClassificationConflict(ORDER_TYPE_ID, "Order", List.of(), null, TEST_TIME))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("resolution");
        }

        @Test
        @DisplayName("should reject null detectedAt")
        void shouldRejectNullDetectedAt() {
            assertThatThrownBy(() -> new ClassificationConflict(ORDER_TYPE_ID, "Order", List.of(), "resolution", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("detectedAt");
        }

        @Test
        @DisplayName("should make defensive copy of contributions")
        void shouldMakeDefensiveCopyOfContributions() {
            // given
            var mutableList = new java.util.ArrayList<ConflictingContribution>();
            mutableList.add(new ConflictingContribution(ArchKind.ENTITY, "Criterion", 80, ConfidenceLevel.MEDIUM));

            // when
            ClassificationConflict conflict =
                    new ClassificationConflict(ORDER_TYPE_ID, "Order", mutableList, "resolution", TEST_TIME);
            mutableList.add(
                    new ConflictingContribution(ArchKind.AGGREGATE_ROOT, "AnotherCriterion", 90, ConfidenceLevel.HIGH));

            // then
            assertThat(conflict.contributions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("should create conflict via of() factory")
        void shouldCreateViaFactory() {
            // given
            List<ConflictingContribution> contributions = List.of(
                    ConflictingContribution.of(ArchKind.AGGREGATE_ROOT, "CriterionA", 100, ConfidenceLevel.HIGH));

            // when
            ClassificationConflict conflict =
                    ClassificationConflict.of(ORDER_TYPE_ID, "Order", contributions, "Resolved", TEST_TIME);

            // then
            assertThat(conflict.typeId()).isEqualTo(ORDER_TYPE_ID);
            assertThat(conflict.typeName()).isEqualTo("Order");
            assertThat(conflict.contributions()).hasSize(1);
            assertThat(conflict.resolution()).isEqualTo("Resolved");
            assertThat(conflict.detectedAt()).isEqualTo(TEST_TIME);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable contributions list")
        void shouldReturnImmutableContributions() {
            // given
            ClassificationConflict conflict = new ClassificationConflict(
                    ORDER_TYPE_ID,
                    "Order",
                    List.of(ConflictingContribution.of(ArchKind.ENTITY, "Criterion", 80, ConfidenceLevel.MEDIUM)),
                    "resolution",
                    TEST_TIME);

            // then
            assertThatThrownBy(() -> conflict.contributions()
                            .add(ConflictingContribution.of(ArchKind.AGGREGATE_ROOT, "New", 100, ConfidenceLevel.HIGH)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethods {

        @Test
        @DisplayName("should identify conflict with multiple contributions")
        void shouldIdentifyMultipleContributions() {
            // given
            List<ConflictingContribution> contributions = List.of(
                    ConflictingContribution.of(ArchKind.AGGREGATE_ROOT, "CriterionA", 100, ConfidenceLevel.HIGH),
                    ConflictingContribution.of(ArchKind.ENTITY, "CriterionB", 100, ConfidenceLevel.HIGH));

            ClassificationConflict conflict =
                    ClassificationConflict.of(ORDER_TYPE_ID, "Order", contributions, "Resolved", TEST_TIME);

            // then
            assertThat(conflict.isRealConflict()).isTrue();
        }

        @Test
        @DisplayName("should not identify single contribution as conflict")
        void shouldNotIdentifySingleContributionAsConflict() {
            // given
            List<ConflictingContribution> contributions = List.of(
                    ConflictingContribution.of(ArchKind.AGGREGATE_ROOT, "CriterionA", 100, ConfidenceLevel.HIGH));

            ClassificationConflict conflict =
                    ClassificationConflict.of(ORDER_TYPE_ID, "Order", contributions, "Resolved", TEST_TIME);

            // then
            assertThat(conflict.isRealConflict()).isFalse();
        }

        @Test
        @DisplayName("should get conflicting kinds")
        void shouldGetConflictingKinds() {
            // given
            List<ConflictingContribution> contributions = List.of(
                    ConflictingContribution.of(ArchKind.AGGREGATE_ROOT, "CriterionA", 100, ConfidenceLevel.HIGH),
                    ConflictingContribution.of(ArchKind.ENTITY, "CriterionB", 100, ConfidenceLevel.HIGH),
                    ConflictingContribution.of(ArchKind.AGGREGATE_ROOT, "CriterionC", 90, ConfidenceLevel.MEDIUM));

            ClassificationConflict conflict =
                    ClassificationConflict.of(ORDER_TYPE_ID, "Order", contributions, "Resolved", TEST_TIME);

            // then
            assertThat(conflict.conflictingKinds()).containsExactlyInAnyOrder(ArchKind.AGGREGATE_ROOT, ArchKind.ENTITY);
        }
    }
}
