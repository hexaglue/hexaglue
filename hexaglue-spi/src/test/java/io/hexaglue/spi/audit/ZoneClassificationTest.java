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

/**
 * Tests for {@link ZoneClassification}.
 */
class ZoneClassificationTest {

    // === classify() tests ===

    @Test
    void testClassify_Ideal_whenDistanceIsZero() {
        // Given: D = 0.0 (perfect balance)
        double distance = 0.0;
        double instability = 0.5; // Any value

        // When
        ZoneClassification result = ZoneClassification.classify(distance, instability);

        // Then
        assertThat(result).isEqualTo(ZoneClassification.IDEAL);
    }

    @ParameterizedTest
    @CsvSource({"0.1, 0.5", "0.2, 0.3", "0.3, 0.7", "0.15, 0.0", "0.25, 1.0"})
    void testClassify_MainSequence_whenDistanceLessThanOrEqualToThreshold(double distance, double instability) {
        // Given: 0 < D <= 0.3

        // When
        ZoneClassification result = ZoneClassification.classify(distance, instability);

        // Then
        assertThat(result).isEqualTo(ZoneClassification.MAIN_SEQUENCE);
    }

    @ParameterizedTest
    @CsvSource({"0.5, 0.0", "0.7, 0.3", "1.0, 0.4", "0.4, 0.49"})
    void testClassify_ZoneOfPain_whenDistanceHighAndInstabilityLow(double distance, double instability) {
        // Given: D > 0.3 AND I < 0.5 (stable + concrete)

        // When
        ZoneClassification result = ZoneClassification.classify(distance, instability);

        // Then
        assertThat(result).isEqualTo(ZoneClassification.ZONE_OF_PAIN);
    }

    @ParameterizedTest
    @CsvSource({"0.5, 0.5", "0.7, 0.7", "1.0, 1.0", "0.4, 0.6", "0.6, 0.8"})
    void testClassify_ZoneOfUselessness_whenDistanceHighAndInstabilityHigh(double distance, double instability) {
        // Given: D > 0.3 AND I >= 0.5 (unstable + abstract)

        // When
        ZoneClassification result = ZoneClassification.classify(distance, instability);

        // Then
        assertThat(result).isEqualTo(ZoneClassification.ZONE_OF_USELESSNESS);
    }

    // === isProblematic() tests ===

    @Test
    void testIsProblematic_returnsFalse_forIdeal() {
        assertThat(ZoneClassification.IDEAL.isProblematic()).isFalse();
    }

    @Test
    void testIsProblematic_returnsFalse_forMainSequence() {
        assertThat(ZoneClassification.MAIN_SEQUENCE.isProblematic()).isFalse();
    }

    @Test
    void testIsProblematic_returnsTrue_forZoneOfPain() {
        assertThat(ZoneClassification.ZONE_OF_PAIN.isProblematic()).isTrue();
    }

    @Test
    void testIsProblematic_returnsTrue_forZoneOfUselessness() {
        assertThat(ZoneClassification.ZONE_OF_USELESSNESS.isProblematic()).isTrue();
    }

    // === Boundary tests ===

    @Test
    void testClassify_exactlyAtThreshold_isMainSequence() {
        // D = 0.3 exactly should be MAIN_SEQUENCE
        ZoneClassification result = ZoneClassification.classify(0.3, 0.5);
        assertThat(result).isEqualTo(ZoneClassification.MAIN_SEQUENCE);
    }

    @Test
    void testClassify_justAboveThreshold_withLowInstability_isZoneOfPain() {
        // D = 0.31 with I < 0.5 should be ZONE_OF_PAIN
        ZoneClassification result = ZoneClassification.classify(0.31, 0.49);
        assertThat(result).isEqualTo(ZoneClassification.ZONE_OF_PAIN);
    }

    @Test
    void testClassify_justAboveThreshold_withExactlyHalfInstability_isZoneOfUselessness() {
        // D = 0.31 with I = 0.5 exactly should be ZONE_OF_USELESSNESS
        ZoneClassification result = ZoneClassification.classify(0.31, 0.5);
        assertThat(result).isEqualTo(ZoneClassification.ZONE_OF_USELESSNESS);
    }

    // === Enum values test ===

    @Test
    void testAllEnumValuesAreDefined() {
        assertThat(ZoneClassification.values())
                .containsExactlyInAnyOrder(
                        ZoneClassification.IDEAL,
                        ZoneClassification.MAIN_SEQUENCE,
                        ZoneClassification.ZONE_OF_PAIN,
                        ZoneClassification.ZONE_OF_USELESSNESS);
    }
}
