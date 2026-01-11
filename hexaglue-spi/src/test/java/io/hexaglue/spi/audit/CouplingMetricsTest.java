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
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CouplingMetrics}.
 */
class CouplingMetricsTest {

    // === instability() tests ===

    @Test
    void testInstability_returnsZero_whenNoCouplings() {
        // Given: Ca = 0, Ce = 0
        CouplingMetrics metrics = new CouplingMetrics("pkg", 0, 0, 0.5);

        // When
        double instability = metrics.instability();

        // Then: I = 0 / 0 = 0.0 (by convention)
        assertThat(instability).isEqualTo(0.0);
    }

    @Test
    void testInstability_returnsZero_whenOnlyAfferent() {
        // Given: Ca = 5, Ce = 0 (stable package)
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 0, 0.5);

        // When
        double instability = metrics.instability();

        // Then: I = 0 / 5 = 0.0
        assertThat(instability).isEqualTo(0.0);
    }

    @Test
    void testInstability_returnsOne_whenOnlyEfferent() {
        // Given: Ca = 0, Ce = 5 (unstable package)
        CouplingMetrics metrics = new CouplingMetrics("pkg", 0, 5, 0.5);

        // When
        double instability = metrics.instability();

        // Then: I = 5 / 5 = 1.0
        assertThat(instability).isEqualTo(1.0);
    }

    @Test
    void testInstability_returnsHalf_whenBalanced() {
        // Given: Ca = 5, Ce = 5
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 5, 0.5);

        // When
        double instability = metrics.instability();

        // Then: I = 5 / 10 = 0.5
        assertThat(instability).isCloseTo(0.5, within(0.001));
    }

    // === distanceFromMainSequence() tests ===

    @Test
    void testDistanceFromMainSequence_returnsZero_whenOnMainSequence() {
        // Given: A = 0.5, I = 0.5 -> D = |0.5 + 0.5 - 1| = 0
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 5, 0.5);

        // When
        double distance = metrics.distanceFromMainSequence();

        // Then
        assertThat(distance).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testDistanceFromMainSequence_returnsOne_whenCompletelyConcreteAndStable() {
        // Given: A = 0.0, I = 0.0 -> D = |0 + 0 - 1| = 1.0
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 0, 0.0);

        // When
        double distance = metrics.distanceFromMainSequence();

        // Then
        assertThat(distance).isCloseTo(1.0, within(0.001));
    }

    @Test
    void testDistanceFromMainSequence_returnsOne_whenCompletelyAbstractAndUnstable() {
        // Given: A = 1.0, I = 1.0 -> D = |1 + 1 - 1| = 1.0
        CouplingMetrics metrics = new CouplingMetrics("pkg", 0, 5, 1.0);

        // When
        double distance = metrics.distanceFromMainSequence();

        // Then
        assertThat(distance).isCloseTo(1.0, within(0.001));
    }

    // === zone() tests ===

    @Test
    void testZone_returnsIdeal_whenOnMainSequence() {
        // Given: A = 0.5, I = 0.5 -> D = 0
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 5, 0.5);

        // When
        ZoneClassification zone = metrics.zone();

        // Then
        assertThat(zone).isEqualTo(ZoneClassification.IDEAL);
    }

    @Test
    void testZone_returnsMainSequence_whenCloseToIdeal() {
        // Given: A = 0.6, I = 0.33 -> D = |0.6 + 0.33 - 1| = 0.07
        // Ca = 2, Ce = 1 -> I = 1/3 = 0.33
        CouplingMetrics metrics = new CouplingMetrics("pkg", 2, 1, 0.6);

        // When
        ZoneClassification zone = metrics.zone();

        // Then
        assertThat(zone).isEqualTo(ZoneClassification.MAIN_SEQUENCE);
    }

    @Test
    void testZone_returnsZoneOfPain_whenStableAndConcrete() {
        // Given: A = 0.0, I = 0.0 -> D = 1.0 (>0.3), I < 0.5
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 0, 0.0);

        // When
        ZoneClassification zone = metrics.zone();

        // Then
        assertThat(zone).isEqualTo(ZoneClassification.ZONE_OF_PAIN);
    }

    @Test
    void testZone_returnsZoneOfUselessness_whenUnstableAndAbstract() {
        // Given: A = 1.0, I = 1.0 -> D = 1.0 (>0.3), I >= 0.5
        CouplingMetrics metrics = new CouplingMetrics("pkg", 0, 5, 1.0);

        // When
        ZoneClassification zone = metrics.zone();

        // Then
        assertThat(zone).isEqualTo(ZoneClassification.ZONE_OF_USELESSNESS);
    }

    // === isProblematic() tests ===

    @Test
    void testIsProblematic_returnsFalse_forIdealPackage() {
        // Given: Package on main sequence
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 5, 0.5);

        // When/Then
        assertThat(metrics.isProblematic()).isFalse();
    }

    @Test
    void testIsProblematic_returnsFalse_forMainSequencePackage() {
        // Given: Package close to main sequence
        CouplingMetrics metrics = new CouplingMetrics("pkg", 2, 1, 0.6);

        // When/Then
        assertThat(metrics.isProblematic()).isFalse();
    }

    @Test
    void testIsProblematic_returnsTrue_forZoneOfPainPackage() {
        // Given: Stable and concrete package
        CouplingMetrics metrics = new CouplingMetrics("pkg", 5, 0, 0.0);

        // When/Then
        assertThat(metrics.isProblematic()).isTrue();
    }

    @Test
    void testIsProblematic_returnsTrue_forZoneOfUselessnessPackage() {
        // Given: Unstable and abstract package
        CouplingMetrics metrics = new CouplingMetrics("pkg", 0, 5, 1.0);

        // When/Then
        assertThat(metrics.isProblematic()).isTrue();
    }

    // === isInZoneOfPain() tests (legacy) ===

    @Test
    void testIsInZoneOfPain_returnsTrue_whenLowInstabilityAndLowAbstractness() {
        // Given: I < 0.3, A < 0.3
        CouplingMetrics metrics = new CouplingMetrics("pkg", 10, 1, 0.1);

        // When/Then
        assertThat(metrics.isInZoneOfPain()).isTrue();
    }

    @Test
    void testIsInZoneOfPain_returnsFalse_whenHighInstability() {
        // Given: I > 0.3
        CouplingMetrics metrics = new CouplingMetrics("pkg", 1, 10, 0.1);

        // When/Then
        assertThat(metrics.isInZoneOfPain()).isFalse();
    }

    // === isInZoneOfUselessness() tests (legacy) ===

    @Test
    void testIsInZoneOfUselessness_returnsTrue_whenHighInstabilityAndHighAbstractness() {
        // Given: I > 0.7, A > 0.7
        CouplingMetrics metrics = new CouplingMetrics("pkg", 1, 10, 0.9);

        // When/Then
        assertThat(metrics.isInZoneOfUselessness()).isTrue();
    }

    @Test
    void testIsInZoneOfUselessness_returnsFalse_whenLowAbstractness() {
        // Given: A < 0.7
        CouplingMetrics metrics = new CouplingMetrics("pkg", 1, 10, 0.5);

        // When/Then
        assertThat(metrics.isInZoneOfUselessness()).isFalse();
    }

    // === Record accessors tests ===

    @Test
    void testRecordAccessors_returnCorrectValues() {
        // Given
        CouplingMetrics metrics = new CouplingMetrics("com.example.domain", 5, 3, 0.6);

        // Then
        assertThat(metrics.packageName()).isEqualTo("com.example.domain");
        assertThat(metrics.afferentCoupling()).isEqualTo(5);
        assertThat(metrics.efferentCoupling()).isEqualTo(3);
        assertThat(metrics.abstractness()).isEqualTo(0.6);
    }

    // === Integration tests ===

    @Test
    void testZoneAndIsProblematic_areConsistent() {
        // Test that zone() and isProblematic() are consistent
        CouplingMetrics[] testCases = {
            new CouplingMetrics("pkg1", 5, 5, 0.5), // IDEAL
            new CouplingMetrics("pkg2", 2, 1, 0.6), // MAIN_SEQUENCE
            new CouplingMetrics("pkg3", 5, 0, 0.0), // ZONE_OF_PAIN
            new CouplingMetrics("pkg4", 0, 5, 1.0) // ZONE_OF_USELESSNESS
        };

        for (CouplingMetrics metrics : testCases) {
            ZoneClassification zone = metrics.zone();
            boolean problematic = metrics.isProblematic();

            // zone.isProblematic() should match metrics.isProblematic()
            assertThat(zone.isProblematic())
                    .as("Consistency for %s with zone %s", metrics.packageName(), zone)
                    .isEqualTo(problematic);
        }
    }
}
