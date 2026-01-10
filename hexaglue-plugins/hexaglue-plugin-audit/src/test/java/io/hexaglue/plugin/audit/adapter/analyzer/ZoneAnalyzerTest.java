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

import io.hexaglue.plugin.audit.domain.model.PackageZoneMetrics;
import io.hexaglue.plugin.audit.domain.model.ZoneCategory;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ZoneAnalyzer}.
 */
class ZoneAnalyzerTest {

    private static final CodeMetrics DEFAULT_METRICS = new CodeMetrics(50, 5, 3, 2, 80.0);
    private static final DocumentationInfo DEFAULT_DOC = new DocumentationInfo(true, 100, List.of());

    private final ZoneAnalyzer analyzer = new ZoneAnalyzer();

    // === Basic Tests ===

    @Test
    void shouldRejectNullCodebase() {
        // When/Then
        assertThatThrownBy(() -> analyzer.analyze(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("codebase required");
    }

    @Test
    void shouldReturnEmptyList_whenCodebaseIsEmpty() {
        // Given
        Codebase codebase = new Codebase("test", "com.example", List.of(), Map.of());

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).isEmpty();
    }

    // === Ideal Package Tests ===

    @Test
    void shouldCategorizeAsIdeal_whenPackageOnMainSequence() {
        // Given: Package with A=0.5, I=0.5 -> D = |0.5 + 0.5 - 1| = 0
        CodeUnit interface1 = createInterface("com.example.domain", "Repository");
        CodeUnit class1 = createClass("com.example.domain", "Order");

        Map<String, Set<String>> deps = new HashMap<>();
        // domain has 1 outgoing dependency to infra
        deps.put("com.example.domain.Order", Set.of("com.example.infra.Database"));
        // app has 1 incoming dependency from app
        deps.put("com.example.app.Service", Set.of("com.example.domain.Repository"));

        Codebase codebase = new Codebase("test", "com.example", List.of(interface1, class1), deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.domain");
        assertThat(metrics.abstractness()).isCloseTo(0.5, within(0.01));
        assertThat(metrics.instability()).isCloseTo(0.5, within(0.01)); // 1 out (infra), 1 in (app)
        assertThat(metrics.distance()).isCloseTo(0.0, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.IDEAL);
        assertThat(metrics.isHealthy()).isTrue();
    }

    // === Main Sequence Tests ===

    @Test
    void shouldCategorizeAsMainSequence_whenCloseToIdeal() {
        // Given: Package with A=0.6, I=0.3 -> D = |0.6 + 0.3 - 1| = 0.1
        List<CodeUnit> units = List.of(
                createInterface("com.example.domain", "Port1"),
                createInterface("com.example.domain", "Port2"),
                createInterface("com.example.domain", "Port3"),
                createClass("com.example.domain", "Service1"),
                createClass("com.example.domain", "Service2"));

        // 2 outgoing, 4 incoming -> I = 2/6 = 0.33
        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.domain.Service1", Set.of("com.example.infra.Adapter1"));
        deps.put("com.example.domain.Service2", Set.of("com.example.infra.Adapter2"));
        deps.put("com.example.app.UseCase1", Set.of("com.example.domain.Port1"));
        deps.put("com.example.app.UseCase2", Set.of("com.example.domain.Port2"));
        deps.put("com.example.app.UseCase3", Set.of("com.example.domain.Service1"));
        deps.put("com.example.app.UseCase4", Set.of("com.example.domain.Service2"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.domain");
        assertThat(metrics.abstractness()).isCloseTo(0.6, within(0.01)); // 3 interfaces / 5 total
        assertThat(metrics.distance()).isLessThanOrEqualTo(0.3);
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.MAIN_SEQUENCE);
        assertThat(metrics.isHealthy()).isTrue();
    }

    // === Zone of Pain Tests ===

    @Test
    void shouldCategorizeAsZoneOfPain_whenConcreteAndStable() {
        // Given: Package with all concrete classes, many incoming dependencies
        // A = 0.0 (no abstractions), I = 0.0 (only incoming) -> D = |0 + 0 - 1| = 1.0
        List<CodeUnit> units = List.of(
                createClass("com.example.util", "StringUtil"),
                createClass("com.example.util", "DateUtil"),
                createClass("com.example.util", "MathUtil"));

        // Only incoming dependencies (stable)
        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.app.Service1", Set.of("com.example.util.StringUtil"));
        deps.put("com.example.app.Service2", Set.of("com.example.util.DateUtil"));
        deps.put("com.example.domain.Model", Set.of("com.example.util.MathUtil"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.util");
        assertThat(metrics.abstractness()).isCloseTo(0.0, within(0.01)); // All concrete
        assertThat(metrics.instability()).isCloseTo(0.0, within(0.01)); // Only incoming
        assertThat(metrics.distance()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
        assertThat(metrics.isProblematic()).isTrue();
    }

    @Test
    void shouldCategorizeAsZoneOfPain_whenMostlyConcreteAndStable() {
        // Given: Package with low abstractness and low instability
        // A = 0.25, I = 0.25 -> D = |0.25 + 0.25 - 1| = 0.5
        List<CodeUnit> units = List.of(
                createInterface("com.example.common", "Validator"),
                createClass("com.example.common", "StringValidator"),
                createClass("com.example.common", "EmailValidator"),
                createClass("com.example.common", "PhoneValidator"));

        // 1 outgoing, 3 incoming -> I = 1/4 = 0.25
        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.common.StringValidator", Set.of("java.util.regex.Pattern"));
        deps.put("com.example.app.Service1", Set.of("com.example.common.Validator"));
        deps.put("com.example.app.Service2", Set.of("com.example.common.EmailValidator"));
        deps.put("com.example.domain.Model", Set.of("com.example.common.PhoneValidator"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(0.25, within(0.01)); // 1/4 abstract
        assertThat(metrics.distance()).isGreaterThan(0.3);
        assertThat(metrics.instability()).isLessThan(0.5);
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    // === Zone of Uselessness Tests ===

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenAbstractAndUnstable() {
        // Given: Package with all interfaces, only outgoing dependencies
        // A = 1.0 (all abstract), I = 1.0 (only outgoing) -> D = |1 + 1 - 1| = 1.0
        List<CodeUnit> units = List.of(
                createInterface("com.example.api", "ApiService"),
                createInterface("com.example.api", "ApiClient"),
                createInterface("com.example.api", "ApiHandler"));

        // Only outgoing dependencies (unstable) - the interfaces reference external types
        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.api.ApiService", Set.of("java.util.List"));
        deps.put("com.example.api.ApiClient", Set.of("java.net.HttpClient"));
        deps.put("com.example.api.ApiHandler", Set.of("java.util.Optional"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.packageName()).isEqualTo("com.example.api");
        assertThat(metrics.abstractness()).isCloseTo(1.0, within(0.01)); // All interfaces
        assertThat(metrics.instability()).isCloseTo(1.0, within(0.01)); // Only outgoing
        assertThat(metrics.distance()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
        assertThat(metrics.isProblematic()).isTrue();
    }

    @Test
    void shouldCategorizeAsZoneOfUselessness_whenMostlyAbstractAndUnstable() {
        // Given: Package with high abstractness and high instability
        // A = 0.8, I = 0.8 -> D = |0.8 + 0.8 - 1| = 0.6 (well above 0.3 threshold)
        List<CodeUnit> units = List.of(
                createInterface("com.example.spi", "Plugin"),
                createInterface("com.example.spi", "Extension"),
                createInterface("com.example.spi", "Provider"),
                createInterface("com.example.spi", "Handler"),
                createClass("com.example.spi", "PluginManager"));

        // 4 outgoing to different packages, 1 incoming -> I = 4/5 = 0.8
        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.spi.Plugin", Set.of("java.util.ServiceLoader"));
        deps.put("com.example.spi.Extension", Set.of("java.io.Serializable"));
        deps.put("com.example.spi.Provider", Set.of("java.net.URI"));
        deps.put("com.example.spi.PluginManager", Set.of("javax.inject.Provider"));
        deps.put("com.example.core.Engine", Set.of("com.example.spi.Plugin"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(0.8, within(0.01)); // 4/5 abstract
        assertThat(metrics.instability()).isCloseTo(0.8, within(0.01)); // 4 out, 1 in
        assertThat(metrics.distance()).isCloseTo(0.6, within(0.01)); // |0.8 + 0.8 - 1| = 0.6
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_USELESSNESS);
    }

    // === Edge Case Tests ===

    @Test
    void shouldHandlePackageWithOnlyInterfaces() {
        // Given: Package with only interfaces (A=1.0)
        List<CodeUnit> units = List.of(
                createInterface("com.example.ports", "InputPort"), createInterface("com.example.ports", "OutputPort"));

        Map<String, Set<String>> deps = new HashMap<>();
        // 1 outgoing, 1 incoming -> I = 0.5
        deps.put("com.example.ports.InputPort", Set.of("com.example.domain.Model"));
        deps.put("com.example.app.UseCase", Set.of("com.example.ports.OutputPort"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(1.0, within(0.01));
        assertThat(metrics.instability()).isCloseTo(0.5, within(0.01));
        assertThat(metrics.distance()).isCloseTo(0.5, within(0.01)); // |1.0 + 0.5 - 1| = 0.5
    }

    @Test
    void shouldHandlePackageWithOnlyConcreteClasses() {
        // Given: Package with only concrete classes (A=0.0)
        List<CodeUnit> units =
                List.of(createClass("com.example.model", "Customer"), createClass("com.example.model", "Order"));

        Map<String, Set<String>> deps = new HashMap<>();
        // Intra-package dependency (ignored for instability)
        deps.put("com.example.model.Order", Set.of("com.example.model.Customer"));
        // 1 incoming from service
        deps.put("com.example.service.OrderService", Set.of("com.example.model.Order"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(0.0, within(0.01));
        // No inter-package outgoing, 1 inter-package incoming -> I = 0 / (1 + 0) = 0.0
        assertThat(metrics.instability()).isCloseTo(0.0, within(0.01));
        assertThat(metrics.distance()).isCloseTo(1.0, within(0.01)); // |0.0 + 0.0 - 1| = 1.0
    }

    @Test
    void shouldHandlePackageWithNoDependencies() {
        // Given: Package with no dependencies (I=1.0 by convention)
        List<CodeUnit> units = List.of(createClass("com.example.isolated", "Util"));

        Codebase codebase = new Codebase("test", "com.example", units, Map.of());

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.abstractness()).isCloseTo(0.0, within(0.01));
        assertThat(metrics.instability()).isCloseTo(1.0, within(0.01)); // No deps = maximally unstable
        assertThat(metrics.distance()).isCloseTo(0.0, within(0.01)); // |0 + 1 - 1| = 0
        assertThat(metrics.zone()).isEqualTo(ZoneCategory.IDEAL); // Surprisingly on main sequence!
    }

    @Test
    void shouldHandlePackageWithOnlyIncomingDependencies() {
        // Given: Package with only incoming dependencies (I=0.0)
        List<CodeUnit> units = List.of(createClass("com.example.foundation", "BaseClass"));

        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.app.Service1", Set.of("com.example.foundation.BaseClass"));
        deps.put("com.example.app.Service2", Set.of("com.example.foundation.BaseClass"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.instability()).isCloseTo(0.0, within(0.01)); // Only incoming = maximally stable
    }

    @Test
    void shouldHandlePackageWithOnlyOutgoingDependencies() {
        // Given: Package with only outgoing dependencies (I=1.0)
        List<CodeUnit> units = List.of(createClass("com.example.client", "ApiClient"));

        Map<String, Set<String>> deps = new HashMap<>();
        deps.put("com.example.client.ApiClient", Set.of("java.net.HttpClient"));
        deps.put("com.example.client.ApiClient", Set.of("java.util.List"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        assertThat(metrics.instability()).isCloseTo(1.0, within(0.01)); // Only outgoing = maximally unstable
    }

    // === Multi-Package Tests ===

    @Test
    void shouldAnalyzeMultiplePackagesSeparately() {
        // Given: Two packages with different characteristics
        List<CodeUnit> units = List.of(
                // Package 1: domain (healthy)
                createInterface("com.example.domain", "Repository"),
                createClass("com.example.domain", "Order"),

                // Package 2: util (zone of pain)
                createClass("com.example.util", "StringUtil"),
                createClass("com.example.util", "DateUtil"));

        Map<String, Set<String>> deps = new HashMap<>();
        // Domain: balanced
        deps.put("com.example.domain.Order", Set.of("com.example.infra.Database"));
        deps.put("com.example.app.UseCase", Set.of("com.example.domain.Repository"));

        // Util: stable (only incoming)
        deps.put("com.example.domain.Order", Set.of("com.example.util.StringUtil"));
        deps.put("com.example.app.Service", Set.of("com.example.util.DateUtil"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(2);

        PackageZoneMetrics domainMetrics = result.stream()
                .filter(m -> m.packageName().equals("com.example.domain"))
                .findFirst()
                .orElseThrow();

        PackageZoneMetrics utilMetrics = result.stream()
                .filter(m -> m.packageName().equals("com.example.util"))
                .findFirst()
                .orElseThrow();

        // Domain should be healthy
        assertThat(domainMetrics.isHealthy()).isTrue();

        // Util should be in zone of pain
        assertThat(utilMetrics.zone()).isEqualTo(ZoneCategory.ZONE_OF_PAIN);
    }

    @Test
    void shouldIgnoreIntraPackageDependencies_whenCalculatingInstability() {
        // Given: Package with internal dependencies
        List<CodeUnit> units = List.of(
                createClass("com.example.domain", "Order"),
                createClass("com.example.domain", "OrderLine"),
                createClass("com.example.domain", "Customer"));

        Map<String, Set<String>> deps = new HashMap<>();
        // Intra-package dependencies (should be ignored)
        deps.put("com.example.domain.Order", Set.of("com.example.domain.OrderLine"));
        deps.put("com.example.domain.Order", Set.of("com.example.domain.Customer"));
        deps.put("com.example.domain.OrderLine", Set.of("com.example.domain.Customer"));

        // One inter-package dependency
        deps.put("com.example.domain.Order", Set.of("com.example.util.DateUtil"));

        Codebase codebase = new Codebase("test", "com.example", units, deps);

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        // Instability should only consider the 1 outgoing inter-package dependency
        assertThat(metrics.instability()).isCloseTo(1.0, within(0.01)); // 1 out, 0 in
    }

    @Test
    void shouldHandleAbstractClassNamingConvention() {
        // Given: Class with "Abstract" prefix
        List<CodeUnit> units = List.of(
                createClass("com.example.base", "AbstractService"), createClass("com.example.base", "ConcreteService"));

        Codebase codebase = new Codebase("test", "com.example", units, Map.of());

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        // AbstractService should be counted as abstract due to naming convention
        assertThat(metrics.abstractness()).isCloseTo(0.5, within(0.01));
    }

    @Test
    void shouldHandleBaseClassNamingConvention() {
        // Given: Class ending with "Base"
        List<CodeUnit> units = List.of(
                createClass("com.example.base", "ServiceBase"), createClass("com.example.base", "ConcreteService"));

        Codebase codebase = new Codebase("test", "com.example", units, Map.of());

        // When
        List<PackageZoneMetrics> result = analyzer.analyze(codebase);

        // Then
        assertThat(result).hasSize(1);
        PackageZoneMetrics metrics = result.get(0);
        // ServiceBase should be counted as abstract due to naming convention
        assertThat(metrics.abstractness()).isCloseTo(0.5, within(0.01));
    }

    // === Helper Methods ===

    /**
     * Creates a test interface code unit.
     */
    private CodeUnit createInterface(String packageName, String simpleName) {
        String qualifiedName = packageName + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.INTERFACE,
                LayerClassification.DOMAIN,
                RoleClassification.PORT,
                List.of(),
                List.of(),
                DEFAULT_METRICS,
                DEFAULT_DOC);
    }

    /**
     * Creates a test class code unit.
     */
    private CodeUnit createClass(String packageName, String simpleName) {
        String qualifiedName = packageName + "." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                List.of(),
                List.of(),
                DEFAULT_METRICS,
                DEFAULT_DOC);
    }
}
