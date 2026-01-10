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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.*;
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
 * Tests for {@link BoilerplateMetricCalculator}.
 */
class BoilerplateMetricCalculatorTest {

    private BoilerplateMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BoilerplateMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("code.boilerplate.ratio");
    }

    @Test
    void shouldReturnZero_whenNoDomainTypes() {
        // Given: Codebase with only infrastructure types
        Codebase codebase = withUnits(infraClass("Adapter1"), applicationClass("UseCase1"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no domain types found");
    }

    @Test
    void shouldReturnZero_whenDomainTypesHaveNoMethods() {
        // Given: Domain types with no methods
        CodeUnit aggregate = domainClassWithMethods("Order", List.of());
        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no methods found");
    }

    @Test
    void shouldReturn100Percent_whenAllMethodsAreBoilerplate() {
        // Given: Class with only boilerplate methods
        List<MethodDeclaration> methods = List.of(
                getter("getName", "java.lang.String"),
                getter("getAge", "int"),
                setter("setName", "java.lang.String"),
                setter("setAge", "int"),
                standardMethod("equals"),
                standardMethod("hashCode"),
                standardMethod("toString"),
                constructor());

        CodeUnit unit = domainClassWithMethods("Person", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 8 boilerplate / 8 total = 100%
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // 100% > 50%
    }

    @Test
    void shouldReturn0Percent_whenNoBoilerplate() {
        // Given: Class with only domain logic methods
        List<MethodDeclaration> methods = List.of(
                domainMethod("placeOrder", "void"),
                domainMethod("calculateTotal", "java.math.BigDecimal"),
                domainMethod("validateInventory", "boolean"),
                domainMethod("applyDiscount", "void"));

        CodeUnit unit = domainClassWithMethods("OrderService", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 0 boilerplate / 4 total = 0%
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateMixedRatio() {
        // Given: Class with 6 boilerplate and 4 domain methods
        List<MethodDeclaration> methods = List.of(
                // Boilerplate (6)
                getter("getId", "java.lang.Long"),
                getter("getName", "java.lang.String"),
                setter("setName", "java.lang.String"),
                standardMethod("equals"),
                standardMethod("hashCode"),
                constructor(),
                // Domain logic (4)
                domainMethod("placeOrder", "void"),
                domainMethod("cancel", "void"),
                domainMethod("calculateTotal", "java.math.BigDecimal"),
                domainMethod("validateRules", "boolean"));

        CodeUnit unit = domainClassWithMethods("Order", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 6 boilerplate / 10 total = 60%
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // 60% > 50%
    }

    @Test
    void shouldRecognizeGetterVariants() {
        // Given: Different getter patterns
        List<MethodDeclaration> methods = List.of(
                getter("getName", "java.lang.String"), // standard getter
                getter("isActive", "boolean"), // boolean getter
                getter("getTotal", "java.math.BigDecimal"), // complex type getter
                domainMethod("processOrder", "void") // domain logic
                );

        CodeUnit unit = domainClassWithMethods("Product", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 3 boilerplate / 4 total = 75%
        assertThat(metric.value()).isEqualTo(75.0);
    }

    @Test
    void shouldRecognizeBuilderPatterns() {
        // Given: Builder pattern methods
        List<MethodDeclaration> methods = List.of(
                builderMethod("builder"),
                builderMethod("build"),
                builderMethod("withName"),
                builderMethod("withAge"),
                domainMethod("validate", "boolean"));

        CodeUnit unit = domainClassWithMethods("PersonBuilder", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 4 builder methods / 5 total = 80%
        assertThat(metric.value()).isEqualTo(80.0);
    }

    @Test
    void shouldNotCountInterfaceMethods() {
        // Given: Interface with method declarations
        List<MethodDeclaration> methods = List.of(getter("getName", "java.lang.String"), getter("getAge", "int"));

        CodeUnit interfaceUnit = domainInterfaceWithMethods("Person", methods);
        Codebase codebase = withUnits(interfaceUnit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Interfaces are skipped, no methods counted
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no methods found");
    }

    @Test
    void shouldAverageAcrossMultipleDomainTypes() {
        // Given: Multiple domain types with different ratios
        // Class 1: 4 boilerplate / 5 total = 80%
        List<MethodDeclaration> methods1 = List.of(
                getter("getName", "java.lang.String"),
                setter("setName", "java.lang.String"),
                standardMethod("equals"),
                standardMethod("hashCode"),
                domainMethod("validate", "boolean"));

        // Class 2: 2 boilerplate / 6 total = 33.33%
        List<MethodDeclaration> methods2 = List.of(
                getter("getId", "java.lang.Long"),
                constructor(),
                domainMethod("process", "void"),
                domainMethod("calculate", "double"),
                domainMethod("validate", "boolean"),
                domainMethod("execute", "void"));

        CodeUnit unit1 = domainClassWithMethods("Entity1", methods1);
        CodeUnit unit2 = domainClassWithMethods("Entity2", methods2);

        Codebase codebase = withUnits(unit1, unit2);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Total = (4 + 2) boilerplate / (5 + 6) total = 6/11 = 54.54%
        assertThat(metric.value()).isCloseTo(54.54, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldWarn_whenRatioExceedsThreshold() {
        // Given: Exactly at 51% boilerplate
        List<MethodDeclaration> methods = List.of(
                getter("getId", "java.lang.Long"),
                getter("getName", "java.lang.String"),
                setter("setName", "java.lang.String"),
                standardMethod("equals"),
                standardMethod("hashCode"),
                constructor(), // 6 boilerplate
                domainMethod("process", "void"),
                domainMethod("validate", "boolean"),
                domainMethod("calculate", "double"),
                domainMethod("execute", "void") // 4 domain
                // Total: 6/10 = 60% (above 50%)
                );

        CodeUnit unit = domainClassWithMethods("TestEntity", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldNotWarn_atThresholdBoundary() {
        // Given: Exactly 50% boilerplate
        List<MethodDeclaration> methods = List.of(
                getter("getId", "java.lang.Long"),
                getter("getName", "java.lang.String"),
                setter("setName", "java.lang.String"),
                standardMethod("equals"),
                standardMethod("hashCode"), // 5 boilerplate
                domainMethod("process", "void"),
                domainMethod("validate", "boolean"),
                domainMethod("calculate", "double"),
                domainMethod("execute", "void"),
                domainMethod("apply", "void") // 5 domain
                );

        CodeUnit unit = domainClassWithMethods("TestEntity", methods);
        Codebase codebase = withUnits(unit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Exactly 50%, should not warn (threshold is >50, not >=)
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldHandleRecordType_withMinimalBoilerplate() {
        // Given: Record type with mostly domain logic
        // Records auto-generate constructor, getters, equals, hashCode, toString
        // but we only see the explicitly declared methods
        List<MethodDeclaration> methods = List.of(
                domainMethod("validate", "boolean"),
                domainMethod("calculate", "java.math.BigDecimal"),
                domainMethod("apply", "void"));

        CodeUnit recordUnit = domainRecordWithMethods("OrderId", methods);
        Codebase codebase = withUnits(recordUnit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: No boilerplate detected (record auto-generates it)
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldOnlyAnalyzeDomainLayer() {
        // Given: Mix of layers, but only domain should be analyzed
        List<MethodDeclaration> domainMethods = List.of(
                getter("getName", "java.lang.String"), // boilerplate
                domainMethod("process", "void") // domain logic
                );

        List<MethodDeclaration> infraMethods =
                List.of(getter("getConnection", "java.sql.Connection"), setter("setConnection", "java.sql.Connection"));

        CodeUnit domainUnit = domainClassWithMethods("Order", domainMethods);
        CodeUnit infraUnit = infraClassWithMethods("OrderAdapter", infraMethods);

        Codebase codebase = withUnits(domainUnit, infraUnit);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only domain analyzed - 1 boilerplate / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
    }

    @Test
    void shouldHandleEmptyCodebase() {
        // Given: Empty codebase
        Codebase codebase = new Codebase("test", "com.example", List.of(), java.util.Map.of());

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no domain types found");
    }

    // === Helper Methods ===

    /**
     * Creates a getter method.
     */
    private MethodDeclaration getter(String name, String returnType) {
        return new MethodDeclaration(name, returnType, List.of(), Set.of("public"), Set.of(), 1);
    }

    /**
     * Creates a setter method.
     */
    private MethodDeclaration setter(String name, String paramType) {
        return new MethodDeclaration(name, "void", List.of(paramType), Set.of("public"), Set.of(), 1);
    }

    /**
     * Creates a standard Object method (equals, hashCode, toString).
     */
    private MethodDeclaration standardMethod(String name) {
        String returnType =
                switch (name) {
                    case "equals" -> "boolean";
                    case "hashCode" -> "int";
                    case "toString" -> "java.lang.String";
                    default -> "void";
                };

        List<String> params = name.equals("equals") ? List.of("java.lang.Object") : List.of();

        return new MethodDeclaration(name, returnType, params, Set.of("public"), Set.of(), 1);
    }

    /**
     * Creates a constructor (null return type).
     */
    private MethodDeclaration constructor() {
        return new MethodDeclaration("<init>", null, List.of(), Set.of("public"), Set.of(), 1);
    }

    /**
     * Creates a builder pattern method.
     */
    private MethodDeclaration builderMethod(String name) {
        String returnType = name.equals("build") ? "com.example.domain.Person" : "com.example.domain.PersonBuilder";
        List<String> params = name.startsWith("with") ? List.of("java.lang.String") : List.of();
        return new MethodDeclaration(name, returnType, params, Set.of("public"), Set.of(), 1);
    }

    /**
     * Creates a domain logic method.
     */
    private MethodDeclaration domainMethod(String name, String returnType) {
        return new MethodDeclaration(name, returnType, List.of(), Set.of("public"), Set.of(), 1);
    }

    /**
     * Creates a domain class with specific methods.
     */
    private CodeUnit domainClassWithMethods(String simpleName, List<MethodDeclaration> methods) {
        String qualifiedName = "com.example.domain." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                methods,
                List.of(),
                new CodeMetrics(50, methods.size(), 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates a domain interface with specific methods.
     */
    private CodeUnit domainInterfaceWithMethods(String simpleName, List<MethodDeclaration> methods) {
        String qualifiedName = "com.example.domain." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.INTERFACE,
                LayerClassification.DOMAIN,
                RoleClassification.PORT,
                methods,
                List.of(),
                new CodeMetrics(50, methods.size(), 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates a domain record with specific methods.
     */
    private CodeUnit domainRecordWithMethods(String simpleName, List<MethodDeclaration> methods) {
        String qualifiedName = "com.example.domain." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.RECORD,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT,
                methods,
                List.of(),
                new CodeMetrics(50, methods.size(), 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates an infrastructure class with specific methods.
     */
    private CodeUnit infraClassWithMethods(String simpleName, List<MethodDeclaration> methods) {
        String qualifiedName = "com.example.infrastructure." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.INFRASTRUCTURE,
                RoleClassification.ADAPTER,
                methods,
                List.of(),
                new CodeMetrics(50, methods.size(), 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }
}
