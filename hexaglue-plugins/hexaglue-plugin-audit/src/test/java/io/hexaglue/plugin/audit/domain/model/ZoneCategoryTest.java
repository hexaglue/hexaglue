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

package io.hexaglue.plugin.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ZoneCategory}.
 */
class ZoneCategoryTest {

    // === Categorization Tests ===

    @Test
    void shouldCategorizeAsIdeal_whenDistanceIsZero() {
        // When
        ZoneCategory category = ZoneCategory.categorize(0.0, 0.5);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.IDEAL);
    }

    @Test
    void shouldCategorizeAsIdeal_whenPerfectBalance_highAbstractnessLowInstability() {
        // Given: A=1.0, I=0.0 -> D = |1 + 0 - 1| = 0
        ZoneCategory category = ZoneCategory.categorize(0.0, 0.0);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.IDEAL);
    }

    @Test
    void shouldCategorizeAsIdeal_whenPerfectBalance_lowAbstractnessHighInstability() {
        // Given: A=0.0, I=1.0 -> D = |0 + 1 - 1| = 0
        ZoneCategory category = ZoneCategory.categorize(0.0, 1.0);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.IDEAL);
    }

    @Test
    void shouldCategorizeAsMainSequence_whenDistanceIsSmall() {
        // Given: Distance = 0.1 (within threshold)
        ZoneCategory category = ZoneCategory.categorize(0.1, 0.5);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
    }

    @Test
    void shouldCategorizeAsMainSequence_whenDistanceAtThreshold() {
        // Given: Distance = 0.3 (exactly at threshold)
        ZoneCategory category = ZoneCategory.categorize(0.3, 0.5);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
    }

    @Test
    void shouldCategorizeAsZoneOfPain_whenDistanceLargeAndLowInstability() {
        // Given: Distance > 0.3, Instability < 0.5
        ZoneCategory category = ZoneCategory.categorize(0.5, 0.2);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    @Test
    void shouldCategorizeAsZoneOfPain_whenDistanceLargeAndInstabilityAtBoundary() {
        // Given: Distance > 0.3, Instability = 0.49 (just below 0.5)
        ZoneCategory category = ZoneCategory.categorize(0.5, 0.49);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    @Test
    void shouldCategorizeAsZoneOfPain_whenMaximumDistance_zeroInstability() {
        // Given: A=0.0, I=0.0 -> D = |0 + 0 - 1| = 1.0
        ZoneCategory category = ZoneCategory.categorize(1.0, 0.0);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenDistanceLargeAndHighInstability() {
        // Given: Distance > 0.3, Instability >= 0.5
        ZoneCategory category = ZoneCategory.categorize(0.5, 0.7);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenDistanceLargeAndInstabilityAtBoundary() {
        // Given: Distance > 0.3, Instability = 0.5 (exactly at boundary)
        ZoneCategory category = ZoneCategory.categorize(0.5, 0.5);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenMaximumDistance_maxInstability() {
        // Given: A=1.0, I=1.0 -> D = |1 + 1 - 1| = 1.0
        ZoneCategory category = ZoneCategory.categorize(1.0, 1.0);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    // === Health Status Tests ===

    @Test
    void shouldBeHealthy_whenIdeal() {
        // When/Then
        assertThat(ZoneCategory.IDEAL.isHealthy()).isTrue();
        assertThat(ZoneCategory.IDEAL.isProblematic()).isFalse();
    }

    @Test
    void shouldBeHealthy_whenMainSequence() {
        // When/Then
        assertThat(ZoneCategory.MAIN_SEQUENCE.isHealthy()).isTrue();
        assertThat(ZoneCategory.MAIN_SEQUENCE.isProblematic()).isFalse();
    }

    @Test
    void shouldBeProblematic_whenZoneOfPain() {
        // When/Then
        assertThat(ZoneCategory.ZONE_OF_PAIN.isProblematic()).isTrue();
        assertThat(ZoneCategory.ZONE_OF_PAIN.isHealthy()).isFalse();
    }

    @Test
    void shouldBeProblematic_whenZoneOfUselessness() {
        // When/Then
        assertThat(ZoneCategory.ZONE_OF_USELESSNESS.isProblematic()).isTrue();
        assertThat(ZoneCategory.ZONE_OF_USELESSNESS.isHealthy()).isFalse();
    }

    // === Edge Case Tests ===

    @Test
    void shouldHandleVerySmallNonZeroDistance() {
        // Given: Distance = 0.001 (very close to ideal but not exact)
        ZoneCategory category = ZoneCategory.categorize(0.001, 0.5);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
    }

    @Test
    void shouldHandleDistanceJustAboveThreshold() {
        // Given: Distance = 0.301 (just above 0.3 threshold)
        ZoneCategory category1 = ZoneCategory.categorize(0.301, 0.2); // Low instability
        ZoneCategory category2 = ZoneCategory.categorize(0.301, 0.7); // High instability

        // Then
        assertThat(category1).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
        assertThat(category2).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    @Test
    void shouldHandleMinimumValues() {
        // Given: All values at minimum
        ZoneCategory category = ZoneCategory.categorize(0.0, 0.0);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.IDEAL);
    }

    @Test
    void shouldHandleMaximumValues() {
        // Given: All values at maximum
        ZoneCategory category = ZoneCategory.categorize(1.0, 1.0);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    // === Realistic Scenario Tests ===

    @Test
    void shouldCategorizeTypicalDomainPackage() {
        // Given: Typical domain package (A=0.4, I=0.6 -> D = 0.0)
        ZoneCategory category = ZoneCategory.categorize(0.0, 0.6);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.IDEAL);
    }

    @Test
    void shouldCategorizeTypicalUtilityPackage() {
        // Given: Typical utility package (A=0.1, I=0.1 -> D = 0.8)
        // Many classes depend on it, few abstractions
        ZoneCategory category = ZoneCategory.categorize(0.8, 0.1);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    @Test
    void shouldCategorizeTypicalApiPackage() {
        // Given: Typical API package (A=0.9, I=0.8 -> D = 0.7)
        // Mostly interfaces, few users
        ZoneCategory category = ZoneCategory.categorize(0.7, 0.8);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    @Test
    void shouldCategorizeTypicalApplicationServicePackage() {
        // Given: Typical application service (A=0.3, I=0.6 -> D = 0.1)
        ZoneCategory category = ZoneCategory.categorize(0.1, 0.6);

        // Then
        assertThat(category).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
    }
}
