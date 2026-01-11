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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link QualityLevel}.
 */
class QualityLevelTest {

    // === fromNCCD() tests ===

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0, 1.49, 1.499})
    void testFromNCCD_Excellent_whenNccdLessThan1_5(double nccd) {
        // Given: NCCD < 1.5

        // When
        QualityLevel result = QualityLevel.fromNCCD(nccd);

        // Then
        assertThat(result).isEqualTo(QualityLevel.EXCELLENT);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.5, 1.6, 1.8, 1.99, 1.999})
    void testFromNCCD_Good_whenNccdBetween1_5And2_0(double nccd) {
        // Given: 1.5 <= NCCD < 2.0

        // When
        QualityLevel result = QualityLevel.fromNCCD(nccd);

        // Then
        assertThat(result).isEqualTo(QualityLevel.GOOD);
    }

    @ParameterizedTest
    @ValueSource(doubles = {2.0, 2.5, 2.9, 2.99, 2.999})
    void testFromNCCD_Acceptable_whenNccdBetween2_0And3_0(double nccd) {
        // Given: 2.0 <= NCCD < 3.0

        // When
        QualityLevel result = QualityLevel.fromNCCD(nccd);

        // Then
        assertThat(result).isEqualTo(QualityLevel.ACCEPTABLE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {3.0, 3.5, 4.0, 4.9, 4.999})
    void testFromNCCD_Warning_whenNccdBetween3_0And5_0(double nccd) {
        // Given: 3.0 <= NCCD < 5.0

        // When
        QualityLevel result = QualityLevel.fromNCCD(nccd);

        // Then
        assertThat(result).isEqualTo(QualityLevel.WARNING);
    }

    @ParameterizedTest
    @ValueSource(doubles = {5.0, 5.5, 10.0, 100.0})
    void testFromNCCD_Critical_whenNccdGreaterThanOrEqual5_0(double nccd) {
        // Given: NCCD >= 5.0

        // When
        QualityLevel result = QualityLevel.fromNCCD(nccd);

        // Then
        assertThat(result).isEqualTo(QualityLevel.CRITICAL);
    }

    // === requiresAttention() tests ===

    @Test
    void testRequiresAttention_returnsFalse_forExcellent() {
        assertThat(QualityLevel.EXCELLENT.requiresAttention()).isFalse();
    }

    @Test
    void testRequiresAttention_returnsFalse_forGood() {
        assertThat(QualityLevel.GOOD.requiresAttention()).isFalse();
    }

    @Test
    void testRequiresAttention_returnsFalse_forAcceptable() {
        assertThat(QualityLevel.ACCEPTABLE.requiresAttention()).isFalse();
    }

    @Test
    void testRequiresAttention_returnsTrue_forWarning() {
        assertThat(QualityLevel.WARNING.requiresAttention()).isTrue();
    }

    @Test
    void testRequiresAttention_returnsTrue_forCritical() {
        assertThat(QualityLevel.CRITICAL.requiresAttention()).isTrue();
    }

    // === assessment() tests ===

    @Test
    void testAssessment_returnsNonEmptyString_forAllLevels() {
        for (QualityLevel level : QualityLevel.values()) {
            assertThat(level.assessment())
                    .isNotNull()
                    .isNotBlank();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "EXCELLENT, Excellent",
        "GOOD, Good",
        "ACCEPTABLE, Acceptable",
        "WARNING, attention",
        "CRITICAL, Critical"
    })
    void testAssessment_containsExpectedKeyword(QualityLevel level, String expectedKeyword) {
        assertThat(level.assessment()).containsIgnoringCase(expectedKeyword);
    }

    // === Enum values test ===

    @Test
    void testAllEnumValuesAreDefined() {
        assertThat(QualityLevel.values())
                .containsExactlyInAnyOrder(
                        QualityLevel.EXCELLENT,
                        QualityLevel.GOOD,
                        QualityLevel.ACCEPTABLE,
                        QualityLevel.WARNING,
                        QualityLevel.CRITICAL);
    }

    // === Edge cases ===

    @Test
    void testFromNCCD_handlesNegativeValue() {
        // Negative NCCD should still return EXCELLENT (< 1.5)
        QualityLevel result = QualityLevel.fromNCCD(-1.0);
        assertThat(result).isEqualTo(QualityLevel.EXCELLENT);
    }

    @Test
    void testFromNCCD_handlesZero() {
        QualityLevel result = QualityLevel.fromNCCD(0.0);
        assertThat(result).isEqualTo(QualityLevel.EXCELLENT);
    }
}
