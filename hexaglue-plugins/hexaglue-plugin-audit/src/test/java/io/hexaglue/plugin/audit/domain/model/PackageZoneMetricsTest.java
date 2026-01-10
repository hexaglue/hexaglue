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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PackageZoneMetrics}.
 */
class PackageZoneMetricsTest {

    private static final String TEST_PACKAGE = "com.example.domain";

    // === Construction Tests ===

    @Test
    void shouldCreateValidMetrics() {
        // When
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);

        // Then
        assertThat(metrics.packageName()).isEqualTo(TEST_PACKAGE);
        assertThat(metrics.abstractness()).isEqualTo(0.5);
        assertThat(metrics.instability()).isEqualTo(0.5);
        assertThat(metrics.distance()).isEqualTo(0.0);
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.IDEAL);
    }

    @Test
    void shouldRejectNullPackageName() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(null, 0.5, 0.5, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("packageName cannot be null or blank");
    }

    @Test
    void shouldRejectBlankPackageName() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics("", 0.5, 0.5, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("packageName cannot be null or blank");

        assertThatThrownBy(() -> new PackageZoneMetrics("   ", 0.5, 0.5, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("packageName cannot be null or blank");
    }

    @Test
    void shouldRejectNullZone() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("zone cannot be null");
    }

    @Test
    void shouldRejectNegativeAbstractness() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, -0.1, 0.5, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abstractness must be in range [0.0, 1.0]");
    }

    @Test
    void shouldRejectAbstractnessAboveOne() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, 1.1, 0.5, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abstractness must be in range [0.0, 1.0]");
    }

    @Test
    void shouldRejectNegativeInstability() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, 0.5, -0.1, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instability must be in range [0.0, 1.0]");
    }

    @Test
    void shouldRejectInstabilityAboveOne() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, 0.5, 1.1, 0.0, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instability must be in range [0.0, 1.0]");
    }

    @Test
    void shouldRejectNegativeDistance() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, -0.1, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distance must be in range [0.0, 1.0]");
    }

    @Test
    void shouldRejectDistanceAboveOne() {
        // When/Then
        assertThatThrownBy(() -> new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 1.1, ZoneCategory.IDEAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distance must be in range [0.0, 1.0]");
    }

    @Test
    void shouldAcceptBoundaryValues_minimum() {
        // When
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.0, 0.0, 0.0, ZoneCategory.IDEAL);

        // Then
        assertThat(metrics.abstractness()).isEqualTo(0.0);
        assertThat(metrics.instability()).isEqualTo(0.0);
        assertThat(metrics.distance()).isEqualTo(0.0);
    }

    @Test
    void shouldAcceptBoundaryValues_maximum() {
        // When
        PackageZoneMetrics metrics =
                new PackageZoneMetrics(TEST_PACKAGE, 1.0, 1.0, 1.0, ZoneCategory.ZONE_OF_USELESSNESS);

        // Then
        assertThat(metrics.abstractness()).isEqualTo(1.0);
        assertThat(metrics.instability()).isEqualTo(1.0);
        assertThat(metrics.distance()).isEqualTo(1.0);
    }

    // === Health Status Tests ===

    @Test
    void shouldBeHealthy_whenIdeal() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);

        // When/Then
        assertThat(metrics.isHealthy()).isTrue();
        assertThat(metrics.isProblematic()).isFalse();
    }

    @Test
    void shouldBeHealthy_whenMainSequence() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.6, 0.3, 0.1, ZoneCategory.MAIN_SEQUENCE);

        // When/Then
        assertThat(metrics.isHealthy()).isTrue();
        assertThat(metrics.isProblematic()).isFalse();
    }

    @Test
    void shouldBeProblematic_whenZoneOfPain() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.0, 0.0, 1.0, ZoneCategory.ZONE_OF_PAIN);

        // When/Then
        assertThat(metrics.isProblematic()).isTrue();
        assertThat(metrics.isHealthy()).isFalse();
    }

    @Test
    void shouldBeProblematic_whenZoneOfUselessness() {
        // Given
        PackageZoneMetrics metrics =
                new PackageZoneMetrics(TEST_PACKAGE, 1.0, 1.0, 1.0, ZoneCategory.ZONE_OF_USELESSNESS);

        // When/Then
        assertThat(metrics.isProblematic()).isTrue();
        assertThat(metrics.isHealthy()).isFalse();
    }

    // === Description Tests ===

    @Test
    void shouldProvideDescriptiveText_forIdeal() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);

        // When
        String description = metrics.description();

        // Then
        assertThat(description)
                .contains(TEST_PACKAGE)
                .contains("ideal")
                .contains("A=0.50")
                .contains("I=0.50")
                .contains("D=0.00")
                .contains("Perfect balance");
    }

    @Test
    void shouldProvideDescriptiveText_forMainSequence() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.6, 0.3, 0.1, ZoneCategory.MAIN_SEQUENCE);

        // When
        String description = metrics.description();

        // Then
        assertThat(description)
                .contains(TEST_PACKAGE)
                .contains("main sequence")
                .contains("A=0.60")
                .contains("I=0.30")
                .contains("D=0.10")
                .contains("Good balance");
    }

    @Test
    void shouldProvideDescriptiveText_forZoneOfPain() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.1, 0.2, 0.7, ZoneCategory.ZONE_OF_PAIN);

        // When
        String description = metrics.description();

        // Then
        assertThat(description)
                .contains(TEST_PACKAGE)
                .contains("Zone of Pain")
                .contains("A=0.10")
                .contains("I=0.20")
                .contains("D=0.70")
                .contains("concrete and stable")
                .contains("difficult to change");
    }

    @Test
    void shouldProvideDescriptiveText_forZoneOfUselessness() {
        // Given
        PackageZoneMetrics metrics =
                new PackageZoneMetrics(TEST_PACKAGE, 0.9, 0.8, 0.7, ZoneCategory.ZONE_OF_USELESSNESS);

        // When
        String description = metrics.description();

        // Then
        assertThat(description)
                .contains(TEST_PACKAGE)
                .contains("Zone of Uselessness")
                .contains("A=0.90")
                .contains("I=0.80")
                .contains("D=0.70")
                .contains("abstract and unstable")
                .contains("unused abstractions");
    }

    // === Equality and Record Tests ===

    @Test
    void shouldImplementEquality_basedOnFields() {
        // Given
        PackageZoneMetrics metrics1 = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);
        PackageZoneMetrics metrics2 = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);

        // When/Then
        assertThat(metrics1).isEqualTo(metrics2);
        assertThat(metrics1.hashCode()).isEqualTo(metrics2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentPackageName() {
        // Given
        PackageZoneMetrics metrics1 = new PackageZoneMetrics("com.example.a", 0.5, 0.5, 0.0, ZoneCategory.IDEAL);
        PackageZoneMetrics metrics2 = new PackageZoneMetrics("com.example.b", 0.5, 0.5, 0.0, ZoneCategory.IDEAL);

        // When/Then
        assertThat(metrics1).isNotEqualTo(metrics2);
    }

    @Test
    void shouldNotBeEqual_whenDifferentMetrics() {
        // Given
        PackageZoneMetrics metrics1 = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);
        PackageZoneMetrics metrics2 = new PackageZoneMetrics(TEST_PACKAGE, 0.6, 0.5, 0.0, ZoneCategory.IDEAL);

        // When/Then
        assertThat(metrics1).isNotEqualTo(metrics2);
    }

    @Test
    void shouldNotBeEqual_whenDifferentZone() {
        // Given
        PackageZoneMetrics metrics1 = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);
        PackageZoneMetrics metrics2 = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.MAIN_SEQUENCE);

        // When/Then
        assertThat(metrics1).isNotEqualTo(metrics2);
    }

    @Test
    void shouldProvideStringRepresentation() {
        // Given
        PackageZoneMetrics metrics = new PackageZoneMetrics(TEST_PACKAGE, 0.5, 0.5, 0.0, ZoneCategory.IDEAL);

        // When
        String toString = metrics.toString();

        // Then
        assertThat(toString)
                .contains(TEST_PACKAGE)
                .contains("0.5")
                .contains("0.0")
                .contains("IDEAL");
    }

    // === Realistic Scenario Tests ===

    @Test
    void shouldRepresentHealthyDomainPackage() {
        // Given: A well-designed domain package
        PackageZoneMetrics metrics =
                new PackageZoneMetrics("com.example.order.domain", 0.4, 0.6, 0.0, ZoneCategory.IDEAL);

        // When/Then
        assertThat(metrics.isHealthy()).isTrue();
        assertThat(metrics.description()).contains("Perfect balance");
    }

    @Test
    void shouldRepresentProblematicUtilityPackage() {
        // Given: A problematic utility package (concrete, stable)
        PackageZoneMetrics metrics =
                new PackageZoneMetrics("com.example.util", 0.1, 0.1, 0.8, ZoneCategory.ZONE_OF_PAIN);

        // When/Then
        assertThat(metrics.isProblematic()).isTrue();
        assertThat(metrics.description()).contains("Zone of Pain").contains("difficult to change");
    }

    @Test
    void shouldRepresentProblematicSpiPackage() {
        // Given: An unused SPI package (abstract, unstable)
        PackageZoneMetrics metrics =
                new PackageZoneMetrics("com.example.spi", 0.9, 0.9, 0.8, ZoneCategory.ZONE_OF_USELESSNESS);

        // When/Then
        assertThat(metrics.isProblematic()).isTrue();
        assertThat(metrics.description()).contains("Zone of Uselessness").contains("unused abstractions");
    }

    @Test
    void shouldRepresentAcceptableApplicationPackage() {
        // Given: An application service package on main sequence
        PackageZoneMetrics metrics =
                new PackageZoneMetrics("com.example.application", 0.3, 0.6, 0.1, ZoneCategory.MAIN_SEQUENCE);

        // When/Then
        assertThat(metrics.isHealthy()).isTrue();
        assertThat(metrics.description()).contains("main sequence").contains("Good balance");
    }
}
