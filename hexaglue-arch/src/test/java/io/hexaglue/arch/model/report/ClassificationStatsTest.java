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
import static org.assertj.core.api.Assertions.within;

import io.hexaglue.arch.ConfidenceLevel;
import io.hexaglue.arch.model.ArchKind;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ClassificationStats}.
 *
 * @since 4.1.0
 */
@DisplayName("ClassificationStats")
class ClassificationStatsTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create stats with all parameters")
        void shouldCreateWithAllParameters() {
            // given
            Map<ArchKind, Integer> countByKind = Map.of(
                    ArchKind.AGGREGATE_ROOT, 3,
                    ArchKind.ENTITY, 5,
                    ArchKind.VALUE_OBJECT, 10);
            Map<ConfidenceLevel, Integer> countByConfidence = Map.of(
                    ConfidenceLevel.HIGH, 12,
                    ConfidenceLevel.MEDIUM, 4,
                    ConfidenceLevel.LOW, 2);

            // when
            ClassificationStats stats = new ClassificationStats(
                    20, // totalTypes
                    18, // classifiedTypes
                    2, // unclassifiedTypes
                    countByKind,
                    countByConfidence,
                    1, // conflictCount
                    0 // outOfScopeTypes
                    );

            // then
            assertThat(stats.totalTypes()).isEqualTo(20);
            assertThat(stats.classifiedTypes()).isEqualTo(18);
            assertThat(stats.unclassifiedTypes()).isEqualTo(2);
            assertThat(stats.countByKind()).isEqualTo(countByKind);
            assertThat(stats.countByConfidence()).isEqualTo(countByConfidence);
            assertThat(stats.conflictCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject null countByKind")
        void shouldRejectNullCountByKind() {
            assertThatThrownBy(() -> new ClassificationStats(10, 8, 2, null, Map.of(ConfidenceLevel.HIGH, 8), 0, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("countByKind");
        }

        @Test
        @DisplayName("should reject null countByConfidence")
        void shouldRejectNullCountByConfidence() {
            assertThatThrownBy(() -> new ClassificationStats(10, 8, 2, Map.of(ArchKind.ENTITY, 8), null, 0, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("countByConfidence");
        }

        @Test
        @DisplayName("should reject negative totalTypes")
        void shouldRejectNegativeTotalTypes() {
            assertThatThrownBy(() -> new ClassificationStats(-1, 0, 0, Map.of(), Map.of(), 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalTypes");
        }

        @Test
        @DisplayName("should reject negative conflictCount")
        void shouldRejectNegativeConflictCount() {
            assertThatThrownBy(() -> new ClassificationStats(10, 8, 2, Map.of(), Map.of(), -1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conflictCount");
        }

        @Test
        @DisplayName("should make defensive copy of countByKind")
        void shouldMakeDefensiveCopyOfCountByKind() {
            // given
            var mutableMap = new java.util.HashMap<ArchKind, Integer>();
            mutableMap.put(ArchKind.ENTITY, 5);

            // when
            ClassificationStats stats = new ClassificationStats(5, 5, 0, mutableMap, Map.of(), 0, 0);
            mutableMap.put(ArchKind.AGGREGATE_ROOT, 3);

            // then
            assertThat(stats.countByKind()).hasSize(1);
            assertThat(stats.countByKind()).containsOnlyKeys(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("should make defensive copy of countByConfidence")
        void shouldMakeDefensiveCopyOfCountByConfidence() {
            // given
            var mutableMap = new java.util.HashMap<ConfidenceLevel, Integer>();
            mutableMap.put(ConfidenceLevel.HIGH, 5);

            // when
            ClassificationStats stats = new ClassificationStats(5, 5, 0, Map.of(), mutableMap, 0, 0);
            mutableMap.put(ConfidenceLevel.LOW, 2);

            // then
            assertThat(stats.countByConfidence()).hasSize(1);
            assertThat(stats.countByConfidence()).containsOnlyKeys(ConfidenceLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("classificationRate()")
    class ClassificationRate {

        @Test
        @DisplayName("should return 1.0 when all types are classified")
        void shouldReturnOneWhenAllClassified() {
            // given
            ClassificationStats stats = new ClassificationStats(10, 10, 0, Map.of(), Map.of(), 0, 0);

            // when
            double rate = stats.classificationRate();

            // then
            assertThat(rate).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("should return 0.0 when no types are classified")
        void shouldReturnZeroWhenNoneClassified() {
            // given
            ClassificationStats stats = new ClassificationStats(10, 0, 10, Map.of(), Map.of(), 0, 0);

            // when
            double rate = stats.classificationRate();

            // then
            assertThat(rate).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("should return correct rate for partial classification")
        void shouldReturnCorrectRateForPartial() {
            // given
            ClassificationStats stats = new ClassificationStats(20, 15, 5, Map.of(), Map.of(), 0, 0);

            // when
            double rate = stats.classificationRate();

            // then
            assertThat(rate).isCloseTo(0.75, within(0.001));
        }

        @Test
        @DisplayName("should return 0.0 when there are no types")
        void shouldReturnZeroWhenNoTypes() {
            // given
            ClassificationStats stats = new ClassificationStats(0, 0, 0, Map.of(), Map.of(), 0, 0);

            // when
            double rate = stats.classificationRate();

            // then
            assertThat(rate).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("highConfidenceRate()")
    class HighConfidenceRate {

        @Test
        @DisplayName("should return 1.0 when all are high confidence")
        void shouldReturnOneWhenAllHighConfidence() {
            // given
            ClassificationStats stats =
                    new ClassificationStats(10, 10, 0, Map.of(), Map.of(ConfidenceLevel.HIGH, 10), 0, 0);

            // when
            double rate = stats.highConfidenceRate();

            // then
            assertThat(rate).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("should return 0.0 when no high confidence")
        void shouldReturnZeroWhenNoHighConfidence() {
            // given
            ClassificationStats stats = new ClassificationStats(
                    10, 8, 2, Map.of(), Map.of(ConfidenceLevel.MEDIUM, 5, ConfidenceLevel.LOW, 3), 0, 0);

            // when
            double rate = stats.highConfidenceRate();

            // then
            assertThat(rate).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("should return correct rate for mixed confidence")
        void shouldReturnCorrectRateForMixed() {
            // given
            ClassificationStats stats = new ClassificationStats(
                    20,
                    18,
                    2,
                    Map.of(),
                    Map.of(ConfidenceLevel.HIGH, 12, ConfidenceLevel.MEDIUM, 4, ConfidenceLevel.LOW, 2),
                    0,
                    0);

            // when
            double rate = stats.highConfidenceRate();

            // then
            // 12 / 18 = 0.666...
            assertThat(rate).isCloseTo(0.667, within(0.001));
        }

        @Test
        @DisplayName("should return 0.0 when no classified types")
        void shouldReturnZeroWhenNoClassifiedTypes() {
            // given
            ClassificationStats stats = new ClassificationStats(10, 0, 10, Map.of(), Map.of(), 0, 0);

            // when
            double rate = stats.highConfidenceRate();

            // then
            assertThat(rate).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return immutable countByKind")
        void shouldReturnImmutableCountByKind() {
            // given
            ClassificationStats stats = new ClassificationStats(10, 10, 0, Map.of(ArchKind.ENTITY, 10), Map.of(), 0, 0);

            // then
            assertThatThrownBy(() -> stats.countByKind().put(ArchKind.AGGREGATE_ROOT, 5))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable countByConfidence")
        void shouldReturnImmutableCountByConfidence() {
            // given
            ClassificationStats stats =
                    new ClassificationStats(10, 10, 0, Map.of(), Map.of(ConfidenceLevel.HIGH, 10), 0, 0);

            // then
            assertThatThrownBy(() -> stats.countByConfidence().put(ConfidenceLevel.LOW, 5))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("should create stats via of() factory")
        void shouldCreateViaFactory() {
            // given
            Map<ArchKind, Integer> countByKind = Map.of(ArchKind.ENTITY, 5);
            Map<ConfidenceLevel, Integer> countByConfidence = Map.of(ConfidenceLevel.HIGH, 5);

            // when
            ClassificationStats stats = ClassificationStats.of(10, 8, 2, countByKind, countByConfidence, 1);

            // then
            assertThat(stats.totalTypes()).isEqualTo(10);
            assertThat(stats.classifiedTypes()).isEqualTo(8);
            assertThat(stats.unclassifiedTypes()).isEqualTo(2);
            assertThat(stats.conflictCount()).isEqualTo(1);
        }
    }
}
