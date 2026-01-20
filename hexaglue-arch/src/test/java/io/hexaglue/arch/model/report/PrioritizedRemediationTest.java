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
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PrioritizedRemediation}.
 *
 * @since 4.1.0
 */
@DisplayName("PrioritizedRemediation")
class PrioritizedRemediationTest {

    private static final TypeId ORDER_TYPE_ID = TypeId.of("com.example.Order");

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create remediation with all parameters")
        void shouldCreateWithAllParameters() {
            // given
            List<String> annotations = List.of("@AggregateRoot", "@Entity");

            // when
            PrioritizedRemediation remediation = new PrioritizedRemediation(
                    1,
                    ORDER_TYPE_ID,
                    "Order",
                    UnclassifiedCategory.CONFLICTING,
                    "Resolve the conflict by adding explicit annotation",
                    annotations);

            // then
            assertThat(remediation.priority()).isEqualTo(1);
            assertThat(remediation.typeId()).isEqualTo(ORDER_TYPE_ID);
            assertThat(remediation.typeName()).isEqualTo("Order");
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.CONFLICTING);
            assertThat(remediation.suggestion()).isEqualTo("Resolve the conflict by adding explicit annotation");
            assertThat(remediation.possibleAnnotations()).containsExactly("@AggregateRoot", "@Entity");
        }

        @Test
        @DisplayName("should reject null typeId")
        void shouldRejectNullTypeId() {
            assertThatThrownBy(() -> new PrioritizedRemediation(
                            1, null, "Order", UnclassifiedCategory.UNKNOWN, "suggestion", List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeId");
        }

        @Test
        @DisplayName("should reject null typeName")
        void shouldRejectNullTypeName() {
            assertThatThrownBy(() -> new PrioritizedRemediation(
                            1, ORDER_TYPE_ID, null, UnclassifiedCategory.UNKNOWN, "suggestion", List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeName");
        }

        @Test
        @DisplayName("should reject null category")
        void shouldRejectNullCategory() {
            assertThatThrownBy(
                            () -> new PrioritizedRemediation(1, ORDER_TYPE_ID, "Order", null, "suggestion", List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("category");
        }

        @Test
        @DisplayName("should reject null suggestion")
        void shouldRejectNullSuggestion() {
            assertThatThrownBy(() -> new PrioritizedRemediation(
                            1, ORDER_TYPE_ID, "Order", UnclassifiedCategory.UNKNOWN, null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("suggestion");
        }

        @Test
        @DisplayName("should reject null possibleAnnotations")
        void shouldRejectNullPossibleAnnotations() {
            assertThatThrownBy(() -> new PrioritizedRemediation(
                            1, ORDER_TYPE_ID, "Order", UnclassifiedCategory.UNKNOWN, "suggestion", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("possibleAnnotations");
        }

        @Test
        @DisplayName("should reject priority less than 1")
        void shouldRejectPriorityLessThanOne() {
            assertThatThrownBy(() -> new PrioritizedRemediation(
                            0, ORDER_TYPE_ID, "Order", UnclassifiedCategory.UNKNOWN, "suggestion", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priority");
        }

        @Test
        @DisplayName("should reject priority greater than 5")
        void shouldRejectPriorityGreaterThanFive() {
            assertThatThrownBy(() -> new PrioritizedRemediation(
                            6, ORDER_TYPE_ID, "Order", UnclassifiedCategory.UNKNOWN, "suggestion", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priority");
        }

        @Test
        @DisplayName("should make defensive copy of possibleAnnotations")
        void shouldMakeDefensiveCopyOfPossibleAnnotations() {
            // given
            var mutableList = new java.util.ArrayList<String>();
            mutableList.add("@Entity");

            // when
            PrioritizedRemediation remediation = new PrioritizedRemediation(
                    1, ORDER_TYPE_ID, "Order", UnclassifiedCategory.UNKNOWN, "suggestion", mutableList);
            mutableList.add("@AggregateRoot");

            // then
            assertThat(remediation.possibleAnnotations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Comparable")
    class ComparableTests {

        @Test
        @DisplayName("should sort by priority ascending")
        void shouldSortByPriorityAscending() {
            // given
            PrioritizedRemediation urgent = new PrioritizedRemediation(
                    1, ORDER_TYPE_ID, "Order", UnclassifiedCategory.CONFLICTING, "Urgent", List.of());
            PrioritizedRemediation medium = new PrioritizedRemediation(
                    3, TypeId.of("com.example.Item"), "Item", UnclassifiedCategory.AMBIGUOUS, "Medium", List.of());
            PrioritizedRemediation optional = new PrioritizedRemediation(
                    5, TypeId.of("com.example.Utils"), "Utils", UnclassifiedCategory.UTILITY, "Optional", List.of());

            // when
            List<PrioritizedRemediation> sorted =
                    List.of(optional, urgent, medium).stream().sorted().toList();

            // then
            assertThat(sorted.get(0).priority()).isEqualTo(1);
            assertThat(sorted.get(1).priority()).isEqualTo(3);
            assertThat(sorted.get(2).priority()).isEqualTo(5);
        }

        @Test
        @DisplayName("should sort by typeName when priority is equal")
        void shouldSortByTypeNameWhenPriorityEqual() {
            // given
            PrioritizedRemediation orderRemediation = new PrioritizedRemediation(
                    2, ORDER_TYPE_ID, "Order", UnclassifiedCategory.AMBIGUOUS, "Check", List.of());
            PrioritizedRemediation itemRemediation = new PrioritizedRemediation(
                    2, TypeId.of("com.example.Item"), "Item", UnclassifiedCategory.AMBIGUOUS, "Check", List.of());

            // when
            List<PrioritizedRemediation> sorted =
                    List.of(orderRemediation, itemRemediation).stream().sorted().toList();

            // then
            assertThat(sorted.get(0).typeName()).isEqualTo("Item");
            assertThat(sorted.get(1).typeName()).isEqualTo("Order");
        }
    }

    @Nested
    @DisplayName("Factory for UnclassifiedType")
    class FactoryForUnclassifiedType {

        @Test
        @DisplayName("should create remediation for CONFLICTING type")
        void shouldCreateForConflictingType() {
            // given
            UnclassifiedType unclassified = createUnclassifiedType(UnclassifiedCategory.CONFLICTING);

            // when
            PrioritizedRemediation remediation = PrioritizedRemediation.forUnclassified(unclassified);

            // then
            assertThat(remediation.priority()).isEqualTo(1);
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.CONFLICTING);
            assertThat(remediation.typeId()).isEqualTo(ORDER_TYPE_ID);
        }

        @Test
        @DisplayName("should create remediation for AMBIGUOUS type")
        void shouldCreateForAmbiguousType() {
            // given
            UnclassifiedType unclassified = createUnclassifiedType(UnclassifiedCategory.AMBIGUOUS);

            // when
            PrioritizedRemediation remediation = PrioritizedRemediation.forUnclassified(unclassified);

            // then
            assertThat(remediation.priority()).isEqualTo(2);
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.AMBIGUOUS);
        }

        @Test
        @DisplayName("should create remediation for UNKNOWN type")
        void shouldCreateForUnknownType() {
            // given
            UnclassifiedType unclassified = createUnclassifiedType(UnclassifiedCategory.UNKNOWN);

            // when
            PrioritizedRemediation remediation = PrioritizedRemediation.forUnclassified(unclassified);

            // then
            assertThat(remediation.priority()).isEqualTo(3);
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.UNKNOWN);
        }

        @Test
        @DisplayName("should create remediation for TECHNICAL type")
        void shouldCreateForTechnicalType() {
            // given
            UnclassifiedType unclassified = createUnclassifiedType(UnclassifiedCategory.TECHNICAL);

            // when
            PrioritizedRemediation remediation = PrioritizedRemediation.forUnclassified(unclassified);

            // then
            assertThat(remediation.priority()).isEqualTo(4);
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.TECHNICAL);
        }

        @Test
        @DisplayName("should create remediation for UTILITY type")
        void shouldCreateForUtilityType() {
            // given
            UnclassifiedType unclassified = createUnclassifiedType(UnclassifiedCategory.UTILITY);

            // when
            PrioritizedRemediation remediation = PrioritizedRemediation.forUnclassified(unclassified);

            // then
            assertThat(remediation.priority()).isEqualTo(5);
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.UTILITY);
        }

        @Test
        @DisplayName("should create remediation for OUT_OF_SCOPE type")
        void shouldCreateForOutOfScopeType() {
            // given
            UnclassifiedType unclassified = createUnclassifiedType(UnclassifiedCategory.OUT_OF_SCOPE);

            // when
            PrioritizedRemediation remediation = PrioritizedRemediation.forUnclassified(unclassified);

            // then
            assertThat(remediation.priority()).isEqualTo(5);
            assertThat(remediation.category()).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }

        private UnclassifiedType createUnclassifiedType(UnclassifiedCategory category) {
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS).build();
            ClassificationTrace trace = ClassificationTrace.unclassified("Test unclassified type", List.of());
            return UnclassifiedType.of(ORDER_TYPE_ID, structure, trace, category);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable possibleAnnotations list")
        void shouldReturnImmutablePossibleAnnotations() {
            // given
            PrioritizedRemediation remediation = new PrioritizedRemediation(
                    1, ORDER_TYPE_ID, "Order", UnclassifiedCategory.CONFLICTING, "suggestion", List.of("@Entity"));

            // then
            assertThatThrownBy(() -> remediation.possibleAnnotations().add("@AggregateRoot"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethods {

        @Test
        @DisplayName("should identify urgent remediation")
        void shouldIdentifyUrgentRemediation() {
            // given
            PrioritizedRemediation urgent = new PrioritizedRemediation(
                    1, ORDER_TYPE_ID, "Order", UnclassifiedCategory.CONFLICTING, "Urgent", List.of());

            // then
            assertThat(urgent.isUrgent()).isTrue();
        }

        @Test
        @DisplayName("should identify non-urgent remediation")
        void shouldIdentifyNonUrgentRemediation() {
            // given
            PrioritizedRemediation optional = new PrioritizedRemediation(
                    5, ORDER_TYPE_ID, "Order", UnclassifiedCategory.UTILITY, "Optional", List.of());

            // then
            assertThat(optional.isUrgent()).isFalse();
        }

        @Test
        @DisplayName("should identify actionable remediation")
        void shouldIdentifyActionableRemediation() {
            // given
            PrioritizedRemediation actionable = new PrioritizedRemediation(
                    2, ORDER_TYPE_ID, "Order", UnclassifiedCategory.AMBIGUOUS, "Actionable", List.of("@Entity"));

            // then
            assertThat(actionable.isActionable()).isTrue();
        }

        @Test
        @DisplayName("should identify non-actionable remediation")
        void shouldIdentifyNonActionableRemediation() {
            // given
            PrioritizedRemediation nonActionable = new PrioritizedRemediation(
                    5, ORDER_TYPE_ID, "Order", UnclassifiedCategory.OUT_OF_SCOPE, "Out of scope", List.of());

            // then
            assertThat(nonActionable.isActionable()).isFalse();
        }
    }
}
