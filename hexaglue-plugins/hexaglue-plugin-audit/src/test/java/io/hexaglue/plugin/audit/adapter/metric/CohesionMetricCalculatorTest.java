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
import io.hexaglue.spi.audit.FieldDeclaration;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.MethodDeclaration;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CohesionMetricCalculator}.
 */
class CohesionMetricCalculatorTest {

    private CohesionMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CohesionMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.cohesion.lcom4");
    }

    @Test
    void shouldReturnZero_whenNoAggregates() {
        // Given: Codebase with no aggregates
        Codebase codebase = withUnits();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.name()).isEqualTo("aggregate.cohesion.lcom4");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("components");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturnOne_forCohesiveClass() {
        // Given: Aggregate with all methods accessing the same non-common type field
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(field("customer", "com.example.domain.Customer")),
                List.of(
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("setCustomer", "void", List.of("com.example.domain.Customer")),
                        method("updateCustomer", "void", List.of("com.example.domain.Customer"))));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: All methods share the customer field through getter/setter and type matching, LCOM4 = 1 (cohesive)
        assertThat(metric.value()).isEqualTo(1.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturnTwo_forClassWithTwoGroups() {
        // Given: Aggregate with two separate groups of methods using non-common types
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(
                        field("customer", "com.example.domain.Customer"),
                        field("product", "com.example.domain.Product")),
                List.of(
                        // Group 1: Methods working with customer
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("setCustomer", "void", List.of("com.example.domain.Customer")),
                        // Group 2: Methods working with product
                        method("getProduct", "com.example.domain.Product", List.of()),
                        method("setProduct", "void", List.of("com.example.domain.Product"))));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Two separate groups, LCOM4 = 2 (low cohesion)
        assertThat(metric.value()).isEqualTo(2.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // Exactly at threshold
    }

    @Test
    void shouldReturnThree_forClassWithThreeGroups() {
        // Given: Aggregate with three separate groups of methods using domain types
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(
                        field("customer", "com.example.domain.Customer"),
                        field("product", "com.example.domain.Product"),
                        field("payment", "com.example.domain.Payment")),
                List.of(
                        // Group 1: customer methods
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("setCustomer", "void", List.of("com.example.domain.Customer")),
                        // Group 2: product methods
                        method("getProduct", "com.example.domain.Product", List.of()),
                        method("setProduct", "void", List.of("com.example.domain.Product")),
                        // Group 3: payment methods
                        method("getPayment", "com.example.domain.Payment", List.of()),
                        method("setPayment", "void", List.of("com.example.domain.Payment"))));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Three separate groups, LCOM4 = 3 (very low cohesion)
        assertThat(metric.value()).isEqualTo(3.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldHandleAggregateWithNoMethods() {
        // Given: Aggregate with fields but no methods
        CodeUnit aggregate = createAggregateWithMethods("Order", List.of(field("id", "java.lang.Long")), List.of());

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Default to 1 (cohesive) when no methods
        assertThat(metric.value()).isEqualTo(1.0);
    }

    @Test
    void shouldHandleAggregateWithNoFields() {
        // Given: Aggregate with methods but no fields
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(),
                List.of(method("process", "void", List.of()), method("validate", "boolean", List.of())));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Default to 1 (cohesive) when no fields
        assertThat(metric.value()).isEqualTo(1.0);
    }

    @Test
    void shouldCalculateAverage_forMultipleAggregates() {
        // Given: Multiple aggregates with varying cohesion
        CodeUnit cohesiveAggregate = createAggregateWithMethods(
                "Order",
                List.of(field("customer", "com.example.domain.Customer")),
                List.of(
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("setCustomer", "void", List.of("com.example.domain.Customer"))));

        CodeUnit lowCohesionAggregate = createAggregateWithMethods(
                "Invoice",
                List.of(
                        field("customer", "com.example.domain.Customer"),
                        field("product", "com.example.domain.Product"),
                        field("payment", "com.example.domain.Payment")),
                List.of(
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("setCustomer", "void", List.of("com.example.domain.Customer")),
                        method("getProduct", "com.example.domain.Product", List.of()),
                        method("setProduct", "void", List.of("com.example.domain.Product")),
                        method("getPayment", "com.example.domain.Payment", List.of()),
                        method("setPayment", "void", List.of("com.example.domain.Payment"))));

        Codebase codebase = withUnits(cohesiveAggregate, lowCohesionAggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average of LCOM4 values (1 + 3) / 2 = 2.0
        assertThat(metric.value()).isEqualTo(2.0);
    }

    @Test
    void shouldExceedThreshold_whenAverageAboveTwo() {
        // Given: Multiple aggregates with average LCOM4 > 2
        CodeUnit agg1 = createAggregateWithMethods(
                "Order",
                List.of(
                        field("id", "java.lang.Long"),
                        field("status", "java.lang.String"),
                        field("total", "java.math.BigDecimal")),
                List.of(
                        method("getId", "java.lang.Long", List.of()),
                        method("getStatus", "java.lang.String", List.of()),
                        method("getTotal", "java.math.BigDecimal", List.of())));

        CodeUnit agg2 = createAggregateWithMethods(
                "Customer",
                List.of(field("id", "java.lang.Long"), field("name", "java.lang.String")),
                List.of(
                        method("getId", "java.lang.Long", List.of()),
                        method("getName", "java.lang.String", List.of())));

        Codebase codebase = withUnits(agg1, agg2);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average LCOM4 = (3 + 2) / 2 = 2.5, exceeds threshold
        assertThat(metric.value()).isEqualTo(2.5);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldHandleEmptyCodebase() {
        // Given: Empty codebase
        Codebase codebase = withUnits();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Default to 0
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldConnectMethodsBySharedFields() {
        // Given: Methods that share fields through type matching with domain types
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(
                        field("customer", "com.example.domain.Customer"),
                        field("product", "com.example.domain.Product")),
                List.of(
                        // These methods share customer field by type
                        method("updateCustomer", "void", List.of("com.example.domain.Customer")),
                        method("validateCustomer", "boolean", List.of("com.example.domain.Customer")),
                        // This method uses product
                        method("getProduct", "com.example.domain.Product", List.of())));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Two groups (customer methods connected, product method separate)
        assertThat(metric.value()).isEqualTo(2.0);
    }

    @Test
    void shouldHandleGetterSetterPattern() {
        // Given: Standard getter/setter methods with non-common types
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(
                        field("customer", "com.example.domain.Customer"),
                        field("product", "com.example.domain.Product")),
                List.of(
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("setCustomer", "void", List.of("com.example.domain.Customer")),
                        method("getProduct", "com.example.domain.Product", List.of()),
                        method("setProduct", "void", List.of("com.example.domain.Product"))));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Two groups (customer methods and product methods)
        assertThat(metric.value()).isEqualTo(2.0);
    }

    @Test
    void shouldTreatConstructorAsCohesive() {
        // Given: Aggregate with constructor (null return type signifies constructor)
        CodeUnit aggregate = createAggregateWithMethods(
                "Order",
                List.of(
                        field("customer", "com.example.domain.Customer"),
                        field("product", "com.example.domain.Product")),
                List.of(
                        // Constructor (null return type, multiple params)
                        new MethodDeclaration(
                                "Order",
                                null,
                                List.of("com.example.domain.Customer", "com.example.domain.Product"),
                                Set.of("public"),
                                Set.of(),
                                1),
                        method("getCustomer", "com.example.domain.Customer", List.of()),
                        method("getProduct", "com.example.domain.Product", List.of())));

        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Constructor connects all methods, LCOM4 = 1
        assertThat(metric.value()).isEqualTo(1.0);
    }

    // === Helper Methods ===

    /**
     * Creates an aggregate with specified methods and fields.
     *
     * @param simpleName the simple name of the aggregate
     * @param fields     the field declarations
     * @param methods    the method declarations
     * @return a CodeUnit representing the aggregate
     */
    private CodeUnit createAggregateWithMethods(
            String simpleName, List<FieldDeclaration> fields, List<MethodDeclaration> methods) {

        String qualifiedName = "com.example.domain." + simpleName;
        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                methods,
                fields,
                new CodeMetrics(100, 5, methods.size(), fields.size(), 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }

    /**
     * Creates a field declaration.
     *
     * @param name the field name
     * @param type the field type
     * @return a FieldDeclaration
     */
    private FieldDeclaration field(String name, String type) {
        return new FieldDeclaration(name, type, Set.of("private"), Set.of());
    }

    /**
     * Creates a method declaration.
     *
     * @param name       the method name
     * @param returnType the return type
     * @param paramTypes the parameter types
     * @return a MethodDeclaration
     */
    private MethodDeclaration method(String name, String returnType, List<String> paramTypes) {
        return new MethodDeclaration(name, returnType, paramTypes, Set.of("public"), Set.of(), 1);
    }
}
