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

package io.hexaglue.spi.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LakosMetrics}.
 */
class LakosMetricsTest {

    // === empty() tests ===

    @Test
    void testEmpty_returnsMetricsWithAllZeros() {
        // When
        LakosMetrics empty = LakosMetrics.empty();

        // Then
        assertThat(empty.componentCount()).isZero();
        assertThat(empty.ccd()).isZero();
        assertThat(empty.acd()).isZero();
        assertThat(empty.nccd()).isZero();
        assertThat(empty.racd()).isZero();
    }

    @Test
    void testEmpty_isEmpty_returnsTrue() {
        // When
        LakosMetrics empty = LakosMetrics.empty();

        // Then
        assertThat(empty.isEmpty()).isTrue();
    }

    // === qualityLevel() tests ===

    @Test
    void testQualityLevel_returnsExcellent_whenNccdLow() {
        // Given: NCCD = 1.0 (< 1.5 = EXCELLENT)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 1.0, 1.0);

        // When
        QualityLevel level = metrics.qualityLevel();

        // Then
        assertThat(level).isEqualTo(QualityLevel.EXCELLENT);
    }

    @Test
    void testQualityLevel_returnsGood_whenNccdModerate() {
        // Given: NCCD = 1.7 (1.5-2.0 = GOOD)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 1.7, 1.0);

        // When
        QualityLevel level = metrics.qualityLevel();

        // Then
        assertThat(level).isEqualTo(QualityLevel.GOOD);
    }

    @Test
    void testQualityLevel_returnsAcceptable_whenNccdHigh() {
        // Given: NCCD = 2.5 (2.0-3.0 = ACCEPTABLE)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 2.5, 1.0);

        // When
        QualityLevel level = metrics.qualityLevel();

        // Then
        assertThat(level).isEqualTo(QualityLevel.ACCEPTABLE);
    }

    @Test
    void testQualityLevel_returnsWarning_whenNccdVeryHigh() {
        // Given: NCCD = 4.0 (3.0-5.0 = WARNING)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 4.0, 1.0);

        // When
        QualityLevel level = metrics.qualityLevel();

        // Then
        assertThat(level).isEqualTo(QualityLevel.WARNING);
    }

    @Test
    void testQualityLevel_returnsCritical_whenNccdExtreme() {
        // Given: NCCD = 6.0 (>= 5.0 = CRITICAL)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 6.0, 1.0);

        // When
        QualityLevel level = metrics.qualityLevel();

        // Then
        assertThat(level).isEqualTo(QualityLevel.CRITICAL);
    }

    // === assessment() tests ===

    @Test
    void testAssessment_delegatesToQualityLevel() {
        // Given
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 1.0, 1.0);

        // When
        String assessment = metrics.assessment();

        // Then
        assertThat(assessment).isEqualTo(QualityLevel.EXCELLENT.assessment());
    }

    @Test
    void testAssessment_returnsNonEmptyString() {
        // Given
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 3.5, 1.0);

        // When
        String assessment = metrics.assessment();

        // Then
        assertThat(assessment).isNotNull().isNotBlank();
    }

    // === requiresAttention() tests ===

    @Test
    void testRequiresAttention_returnsFalse_forExcellentMetrics() {
        // Given: NCCD = 1.0 (EXCELLENT)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 1.0, 1.0);

        // When/Then
        assertThat(metrics.requiresAttention()).isFalse();
    }

    @Test
    void testRequiresAttention_returnsFalse_forGoodMetrics() {
        // Given: NCCD = 1.7 (GOOD)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 1.7, 1.0);

        // When/Then
        assertThat(metrics.requiresAttention()).isFalse();
    }

    @Test
    void testRequiresAttention_returnsFalse_forAcceptableMetrics() {
        // Given: NCCD = 2.5 (ACCEPTABLE)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 2.5, 1.0);

        // When/Then
        assertThat(metrics.requiresAttention()).isFalse();
    }

    @Test
    void testRequiresAttention_returnsTrue_forWarningMetrics() {
        // Given: NCCD = 4.0 (WARNING)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 4.0, 1.0);

        // When/Then
        assertThat(metrics.requiresAttention()).isTrue();
    }

    @Test
    void testRequiresAttention_returnsTrue_forCriticalMetrics() {
        // Given: NCCD = 6.0 (CRITICAL)
        LakosMetrics metrics = new LakosMetrics(10, 50, 5.0, 6.0, 1.0);

        // When/Then
        assertThat(metrics.requiresAttention()).isTrue();
    }

    // === isEmpty() tests ===

    @Test
    void testIsEmpty_returnsTrue_whenComponentCountIsZero() {
        // Given
        LakosMetrics metrics = new LakosMetrics(0, 0, 0.0, 0.0, 0.0);

        // When/Then
        assertThat(metrics.isEmpty()).isTrue();
    }

    @Test
    void testIsEmpty_returnsFalse_whenComponentCountIsPositive() {
        // Given
        LakosMetrics metrics = new LakosMetrics(1, 0, 0.0, 0.0, 0.0);

        // When/Then
        assertThat(metrics.isEmpty()).isFalse();
    }

    // === Record accessors tests ===

    @Test
    void testRecordAccessors_returnCorrectValues() {
        // Given
        LakosMetrics metrics = new LakosMetrics(10, 45, 4.5, 1.8, 1.2);

        // Then
        assertThat(metrics.componentCount()).isEqualTo(10);
        assertThat(metrics.ccd()).isEqualTo(45);
        assertThat(metrics.acd()).isEqualTo(4.5);
        assertThat(metrics.nccd()).isEqualTo(1.8);
        assertThat(metrics.racd()).isEqualTo(1.2);
    }

    // === equals/hashCode tests ===

    @Test
    void testEquals_twoIdenticalMetrics_areEqual() {
        // Given
        LakosMetrics metrics1 = new LakosMetrics(10, 45, 4.5, 1.8, 1.2);
        LakosMetrics metrics2 = new LakosMetrics(10, 45, 4.5, 1.8, 1.2);

        // Then
        assertThat(metrics1).isEqualTo(metrics2);
        assertThat(metrics1.hashCode()).isEqualTo(metrics2.hashCode());
    }

    @Test
    void testEquals_differentMetrics_areNotEqual() {
        // Given
        LakosMetrics metrics1 = new LakosMetrics(10, 45, 4.5, 1.8, 1.2);
        LakosMetrics metrics2 = new LakosMetrics(10, 50, 5.0, 2.0, 1.5);

        // Then
        assertThat(metrics1).isNotEqualTo(metrics2);
    }
}
