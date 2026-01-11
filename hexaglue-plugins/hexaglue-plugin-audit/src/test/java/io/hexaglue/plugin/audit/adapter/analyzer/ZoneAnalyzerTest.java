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

package io.hexaglue.plugin.audit.adapter.analyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.plugin.audit.domain.model.PackageZoneMetrics;
import io.hexaglue.plugin.audit.domain.model.ZoneCategory;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CouplingMetrics;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ZoneAnalyzer}.
 *
 * <p><b>REFACTORED (v3):</b> Tests now use mocked ArchitectureQuery to verify
 * that ZoneAnalyzer correctly delegates to Core and transforms results.
 */
class ZoneAnalyzerTest {

    private ZoneAnalyzer analyzer;
    private ArchitectureQuery mockQuery;
    private Codebase mockCodebase;

    @BeforeEach
    void setUp() {
        analyzer = new ZoneAnalyzer();
        mockQuery = mock(ArchitectureQuery.class);
        mockCodebase = mock(Codebase.class);
    }

    // === Basic Tests ===

    @Test
    void shouldRejectNullArchitectureQuery() {
        // When/Then
        assertThatThrownBy(() -> analyzer.analyze(mockCodebase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("architectureQuery is required");
    }

    @Test
    void shouldReturnEmptyList_whenNoPackagesFound() {
        // Given
        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of());

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).isEmpty();
    }

    // === Ideal Package Tests ===

    @Test
    void shouldCategorizeAsIdeal_whenPackageOnMainSequence() {
        // Given: Package with A=0.5, I=0.5 -> D = |0.5 + 0.5 - 1| = 0
        CouplingMetrics idealMetrics = new CouplingMetrics("com.example.domain", 1, 1, 0.5);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(idealMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.domain");
        assertThat(metrics.abstractness()).isCloseTo(0.5, within(0.01));
        assertThat(metrics.instability()).isCloseTo(0.5, within(0.01));
        assertThat(metrics.distance()).isCloseTo(0.0, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.IDEAL);
        assertThat(metrics.isHealthy()).isTrue();
    }

    // === Main Sequence Tests ===

    @Test
    void shouldCategorizeAsMainSequence_whenCloseToIdeal() {
        // Given: Package with A=0.6, I=0.33 -> D = |0.6 + 0.33 - 1| = 0.07
        // afferent=2, efferent=1 -> I = 1/3 = 0.33
        CouplingMetrics mainSeqMetrics = new CouplingMetrics("com.example.domain", 2, 1, 0.6);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(mainSeqMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.domain");
        assertThat(metrics.abstractness()).isCloseTo(0.6, within(0.01));
        assertThat(metrics.distance()).isLessThanOrEqualTo(0.3);
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
        assertThat(metrics.isHealthy()).isTrue();
    }

    // === Zone of Pain Tests ===

    @Test
    void shouldCategorizeAsZoneOfPain_whenConcreteAndStable() {
        // Given: Package with all concrete classes, many incoming dependencies
        // A = 0.0 (no abstractions), I = 0.0 (only incoming) -> D = |0 + 0 - 1| = 1.0
        // afferent=3, efferent=0 -> I = 0/3 = 0.0
        CouplingMetrics painMetrics = new CouplingMetrics("com.example.util", 3, 0, 0.0);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(painMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.util");
        assertThat(metrics.abstractness()).isCloseTo(0.0, within(0.01));
        assertThat(metrics.instability()).isCloseTo(0.0, within(0.01));
        assertThat(metrics.distance()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
        assertThat(metrics.isProblematic()).isTrue();
    }

    @Test
    void shouldCategorizeAsZoneOfPain_whenMostlyConcreteAndStable() {
        // Given: Package with low abstractness and low instability
        // A = 0.25, I = 0.25 -> D = |0.25 + 0.25 - 1| = 0.5
        // afferent=3, efferent=1 -> I = 1/4 = 0.25
        CouplingMetrics painMetrics = new CouplingMetrics("com.example.common", 3, 1, 0.25);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(painMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(0.25, within(0.01));
        assertThat(metrics.distance()).isGreaterThan(0.3);
        assertThat(metrics.instability()).isLessThan(0.5);
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    // === Zone of Uselessness Tests ===

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenAbstractAndUnstable() {
        // Given: Package with all interfaces, only outgoing dependencies
        // A = 1.0 (all abstract), I = 1.0 (only outgoing) -> D = |1 + 1 - 1| = 1.0
        // afferent=0, efferent=3 -> I = 3/3 = 1.0
        CouplingMetrics uselessMetrics = new CouplingMetrics("com.example.api", 0, 3, 1.0);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(uselessMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.api");
        assertThat(metrics.abstractness()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.instability()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.distance()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
        assertThat(metrics.isProblematic()).isTrue();
    }

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenMostlyAbstractAndUnstable() {
        // Given: Package with high abstractness and high instability
        // A = 0.8, I = 0.8 -> D = |0.8 + 0.8 - 1| = 0.6 (well above 0.3 threshold)
        // afferent=1, efferent=4 -> I = 4/5 = 0.8
        CouplingMetrics uselessMetrics = new CouplingMetrics("com.example.spi", 1, 4, 0.8);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(uselessMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(0.8, within(0.01));
        assertThat(metrics.instability()).isCloseTo(0.8, within(0.01));
        assertThat(metrics.distance()).isCloseTo(0.6, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    // === Edge Case Tests ===

    @Test
    void shouldHandlePackageWithOnlyInterfaces() {
        // Given: Package with only interfaces (A=1.0)
        // 1 outgoing, 1 incoming -> I = 0.5
        CouplingMetrics metrics = new CouplingMetrics("com.example.ports", 1, 1, 1.0);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(metrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics packageMetrics = result.get(0);
        assertThat(packageMetrics.abstractness()).isCloseTo(1.0, within(0.01));
        assertThat(packageMetrics.instability()).isCloseTo(0.5, within(0.01));
        assertThat(packageMetrics.distance()).isCloseTo(0.5, within(0.01));
    }

    @Test
    void shouldHandlePackageWithOnlyConcreteClasses() {
        // Given: Package with only concrete classes (A=0.0)
        // No outgoing, 1 incoming -> I = 0.0
        CouplingMetrics metrics = new CouplingMetrics("com.example.model", 1, 0, 0.0);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(metrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics packageMetrics = result.get(0);
        assertThat(packageMetrics.abstractness()).isCloseTo(0.0, within(0.01));
        assertThat(packageMetrics.instability()).isCloseTo(0.0, within(0.01));
        assertThat(packageMetrics.distance()).isCloseTo(1.0, within(0.01));
    }

    @Test
    void shouldHandlePackageWithNoDependencies() {
        // Given: Package with no dependencies (I=0.0 when afferent+efferent=0)
        // Note: CouplingMetrics returns 0.0 for instability when total is 0
        CouplingMetrics metrics = new CouplingMetrics("com.example.isolated", 0, 0, 0.0);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(metrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics packageMetrics = result.get(0);
        assertThat(packageMetrics.abstractness()).isCloseTo(0.0, within(0.01));
        assertThat(packageMetrics.instability()).isCloseTo(0.0, within(0.01));
        assertThat(packageMetrics.distance()).isCloseTo(1.0, within(0.01));
    }

    // === Multi-Package Tests ===

    @Test
    void shouldAnalyzeMultiplePackagesSeparately() {
        // Given: Two packages with different characteristics
        CouplingMetrics domainMetrics = new CouplingMetrics("com.example.domain", 1, 1, 0.5); // IDEAL
        CouplingMetrics utilMetrics = new CouplingMetrics("com.example.util", 2, 0, 0.0); // ZONE_OF_PAIN

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(domainMetrics, utilMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(2);

        PackageZoneMetrics domain =
                result.stream().filter(m -> m.packageName().equals("com.example.domain")).findFirst().orElseThrow();

        PackageZoneMetrics util =
                result.stream().filter(m -> m.packageName().equals("com.example.util")).findFirst().orElseThrow();

        // Domain should be healthy (IDEAL)
        assertThat(domain.zone()).isEqualTo(ZoneCategory.IDEAL);
        assertThat(domain.isHealthy()).isTrue();

        // Util should be in zone of pain
        assertThat(util.zone()).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
        assertThat(util.isProblematic()).isTrue();
    }

    // === Zone Classification Mapping Tests ===

    @Test
    void shouldCorrectlyMapAllZoneClassifications() {
        // Given: Packages representing all four zones
        CouplingMetrics ideal = new CouplingMetrics("pkg.ideal", 1, 1, 0.5); // D=0
        CouplingMetrics mainSeq = new CouplingMetrics("pkg.mainseq", 2, 1, 0.6); // D~0.07
        CouplingMetrics pain = new CouplingMetrics("pkg.pain", 3, 0, 0.0); // D=1.0, I=0
        CouplingMetrics useless = new CouplingMetrics("pkg.useless", 0, 3, 1.0); // D=1.0, I=1.0

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(ideal, mainSeq, pain, useless));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then
        assertThat(result).hasSize(4);

        Map<String, ZoneCategory> zones = Map.of(
                "pkg.ideal",
                result.stream()
                        .filter(m -> m.packageName().equals("pkg.ideal"))
                        .findFirst()
                        .orElseThrow()
                        .zone(),
                "pkg.mainseq",
                result.stream()
                        .filter(m -> m.packageName().equals("pkg.mainseq"))
                        .findFirst()
                        .orElseThrow()
                        .zone(),
                "pkg.pain",
                result.stream()
                        .filter(m -> m.packageName().equals("pkg.pain"))
                        .findFirst()
                        .orElseThrow()
                        .zone(),
                "pkg.useless",
                result.stream()
                        .filter(m -> m.packageName().equals("pkg.useless"))
                        .findFirst()
                        .orElseThrow()
                        .zone());

        assertThat(zones.get("pkg.ideal")).isEqualTo(ZoneCategory.IDEAL);
        assertThat(zones.get("pkg.mainseq")).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
        assertThat(zones.get("pkg.pain")).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
        assertThat(zones.get("pkg.useless")).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    // === Delegation Verification Tests ===

    @Test
    void shouldDelegateToCoreForMetricsCalculation() {
        // Given: Core returns pre-calculated metrics
        CouplingMetrics coreMetrics = new CouplingMetrics("com.example.domain", 5, 3, 0.4);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(coreMetrics));

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(mockCodebase, mockQuery);

        // Then: Values should match what Core provided
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);

        // Abstractness comes from Core
        assertThat(metrics.abstractness()).isCloseTo(0.4, within(0.01));

        // Instability is calculated from Core's afferent/efferent: 3/(5+3) = 0.375
        assertThat(metrics.instability()).isCloseTo(0.375, within(0.01));

        // Distance is calculated from Core's values: |0.4 + 0.375 - 1| = 0.225
        assertThat(metrics.distance()).isCloseTo(0.225, within(0.01));
    }
}
