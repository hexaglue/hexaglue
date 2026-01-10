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

package io.hexaglue.plugin.audit.adapter.metric;

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.withUnits;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.MethodDeclaration;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CodeComplexityMetricCalculator}.
 */
class CodeComplexityMetricCalculatorTest {

    private CodeComplexityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CodeComplexityMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("code.complexity.average");
    }

    @Test
    void shouldReturnZero_whenNoDomainTypes() {
        // Given: Codebase with no domain types
        Codebase codebase = withUnits();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.name()).isEqualTo("code.complexity.average");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("complexity");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturnZero_whenDomainTypeHasNoMethods() {
        // Given: Domain type with no methods
        CodeUnit domainType =
                createDomainType("Order", RoleClassification.AGGREGATE_ROOT, CodeUnitKind.CLASS, List.of());

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateAverageComplexity_forSimpleMethods() {
        // Given: Domain type with simple methods (complexity = 1)
        CodeUnit domainType = createDomainType(
                "Order",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("getId", "java.lang.Long", 1),
                        methodWithComplexity("getName", "java.lang.String", 1),
                        methodWithComplexity("getTotal", "java.math.BigDecimal", 1)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average complexity = (1 + 1 + 1) / 3 = 1.0
        assertThat(metric.value()).isEqualTo(1.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateAverageComplexity_forModerateMethods() {
        // Given: Domain type with moderate complexity methods
        CodeUnit domainType = createDomainType(
                "Order",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("getId", "java.lang.Long", 1),
                        methodWithComplexity("validate", "boolean", 5),
                        methodWithComplexity("process", "void", 8)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average complexity = (1 + 5 + 8) / 3 = 4.67
        assertThat(metric.value()).isCloseTo(4.67, org.assertj.core.data.Percentage.withPercentage(1.0));
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateAverageComplexity_forHighComplexityMethods() {
        // Given: Domain type with high complexity methods
        CodeUnit domainType = createDomainType(
                "OrderProcessor",
                RoleClassification.SERVICE,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("processOrder", "void", 15),
                        methodWithComplexity("validatePayment", "boolean", 12),
                        methodWithComplexity("calculateTotal", "java.math.BigDecimal", 8)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average complexity = (15 + 12 + 8) / 3 = 11.67
        assertThat(metric.value()).isCloseTo(11.67, org.assertj.core.data.Percentage.withPercentage(1.0));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldExceedThreshold_whenAverageComplexityAboveTen() {
        // Given: Domain type with average complexity > 10
        CodeUnit domainType = createDomainType(
                "ComplexService",
                RoleClassification.SERVICE,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("complexMethod1", "void", 20),
                        methodWithComplexity("complexMethod2", "void", 15),
                        methodWithComplexity("simpleMethod", "void", 1)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average complexity = (20 + 15 + 1) / 3 = 12.0
        assertThat(metric.value()).isEqualTo(12.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldNotExceedThreshold_whenAverageComplexityEqualsThreshold() {
        // Given: Domain type with average complexity exactly at threshold (10)
        CodeUnit domainType = createDomainType(
                "Order",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(methodWithComplexity("method1", "void", 10), methodWithComplexity("method2", "void", 10)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average complexity = 10.0, threshold is > 10 (not >=)
        assertThat(metric.value()).isEqualTo(10.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateAverage_forMultipleDomainTypes() {
        // Given: Multiple domain types with varying complexity
        CodeUnit simpleAggregate = createDomainType(
                "Customer",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("getId", "java.lang.Long", 1),
                        methodWithComplexity("getName", "java.lang.String", 1)));

        CodeUnit complexAggregate = createDomainType(
                "Order",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(methodWithComplexity("process", "void", 15), methodWithComplexity("validate", "boolean", 10)));

        CodeUnit valueObject = createDomainType(
                "Money",
                RoleClassification.VALUE_OBJECT,
                CodeUnitKind.CLASS,
                List.of(methodWithComplexity("add", "Money", 2), methodWithComplexity("subtract", "Money", 2)));

        Codebase codebase = withUnits(simpleAggregate, complexAggregate, valueObject);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (1+1+15+10+2+2) / 6 = 5.17
        assertThat(metric.value()).isCloseTo(5.17, org.assertj.core.data.Percentage.withPercentage(1.0));
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldSkipInterfaces() {
        // Given: Domain layer with both interface and class
        CodeUnit domainInterface = createDomainType(
                "OrderRepository",
                RoleClassification.REPOSITORY,
                CodeUnitKind.INTERFACE,
                List.of(methodWithComplexity("save", "void", 1), methodWithComplexity("findById", "Order", 1)));

        CodeUnit domainClass = createDomainType(
                "Order",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(methodWithComplexity("process", "void", 5)));

        Codebase codebase = withUnits(domainInterface, domainClass);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only the class methods are counted, interface is skipped
        assertThat(metric.value()).isEqualTo(5.0);
    }

    @Test
    void shouldOnlyAnalyzeDomainLayer() {
        // Given: Types in different layers
        CodeUnit domainType = createDomainType(
                "Order",
                RoleClassification.AGGREGATE_ROOT,
                CodeUnitKind.CLASS,
                List.of(methodWithComplexity("process", "void", 10)));

        // Infrastructure layer type (should be excluded)
        CodeUnit infraType = new CodeUnit(
                "com.example.infrastructure.OrderRepositoryImpl",
                CodeUnitKind.CLASS,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                List.of(methodWithComplexity("save", "void", 20)),
                List.of(),
                new CodeMetrics(100, 5, 1, 0, 80.0),
                new DocumentationInfo(true, 100, List.of()));

        Codebase codebase = withUnits(domainType, infraType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only domain type is analyzed
        assertThat(metric.value()).isEqualTo(10.0);
    }

    @Test
    void shouldHandleMixedComplexity() {
        // Given: Domain type with mixed complexity levels
        CodeUnit domainType = createDomainType(
                "OrderProcessor",
                RoleClassification.SERVICE,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("simpleGetter", "String", 1),
                        methodWithComplexity("moderateValidation", "boolean", 7),
                        methodWithComplexity("complexProcessing", "void", 25),
                        methodWithComplexity("anotherGetter", "Long", 1)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (1 + 7 + 25 + 1) / 4 = 8.5
        assertThat(metric.value()).isEqualTo(8.5);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldHandleEmptyCodebase() {
        // Given: Empty codebase
        Codebase codebase = withUnits();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
        assertThat(metric.description()).contains("no domain types found");
    }

    @Test
    void shouldHandleVeryHighComplexity() {
        // Given: Domain type with very high complexity methods
        CodeUnit domainType = createDomainType(
                "LegacyProcessor",
                RoleClassification.SERVICE,
                CodeUnitKind.CLASS,
                List.of(
                        methodWithComplexity("legacyMethod", "void", 50),
                        methodWithComplexity("anotherComplexMethod", "void", 40),
                        methodWithComplexity("yetAnotherOne", "void", 30)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (50 + 40 + 30) / 3 = 40.0
        assertThat(metric.value()).isEqualTo(40.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldHandleZeroComplexityMethods() {
        // Given: Domain type with zero complexity (edge case, shouldn't happen normally)
        CodeUnit domainType = createDomainType(
                "EmptyMethods",
                RoleClassification.ENTITY,
                CodeUnitKind.CLASS,
                List.of(methodWithComplexity("empty1", "void", 0), methodWithComplexity("empty2", "void", 0)));

        Codebase codebase = withUnits(domainType);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    // === Helper Methods ===

    /**
     * Creates a domain type with specified methods.
     *
     * @param simpleName the simple name
     * @param role       the role classification
     * @param kind       the code unit kind
     * @param methods    the method declarations
     * @return a CodeUnit representing the domain type
     */
    private CodeUnit createDomainType(
            String simpleName, RoleClassification role, CodeUnitKind kind, List<MethodDeclaration> methods) {

        String qualifiedName = "com.example.domain." + simpleName;
        return new CodeUnit(
                qualifiedName,
                kind,
                LayerClassification.DOMAIN,
                role,
                methods,
                List.of(),
                new CodeMetrics(100, 5, methods.size(), 0, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates a method declaration with specified complexity.
     *
     * @param name       the method name
     * @param returnType the return type
     * @param complexity the cyclomatic complexity
     * @return a MethodDeclaration
     */
    private MethodDeclaration methodWithComplexity(String name, String returnType, int complexity) {
        return new MethodDeclaration(name, returnType, List.of(), Set.of("public"), Set.of(), complexity);
    }
}
