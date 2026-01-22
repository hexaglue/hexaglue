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

import static io.hexaglue.plugin.audit.util.TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Method;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CohesionMetricCalculator}.
 *
 * <p>Validates that LCOM4 (Lack of Cohesion of Methods) is correctly calculated
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class CohesionMetricCalculatorTest {

    private CohesionMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CohesionMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.cohesion.lcom4");
    }

    @Test
    @DisplayName("Should return zero when no aggregates")
    void shouldReturnZero_whenNoAggregates() {
        // Given: Model with no aggregates
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.name()).isEqualTo("aggregate.cohesion.lcom4");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("components");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return one for cohesive class")
    void shouldReturnOne_forCohesiveClass() {
        // Given: Aggregate with all methods accessing the same non-common type field
        List<Field> fields = List.of(field("customer", "com.example.domain.Customer"));

        List<Method> methods = List.of(
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("setCustomer", "void", List.of("com.example.domain.Customer")),
                methodWithParams("updateCustomer", "void", List.of("com.example.domain.Customer")));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: All methods share the customer field through type matching, LCOM4 = 1 (cohesive)
        assertThat(metric.value()).isEqualTo(1.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return two for class with two groups")
    void shouldReturnTwo_forClassWithTwoGroups() {
        // Given: Aggregate with two separate groups of methods using non-common types
        List<Field> fields = List.of(
                field("customer", "com.example.domain.Customer"), field("product", "com.example.domain.Product"));

        List<Method> methods = List.of(
                // Group 1: Methods working with customer
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("setCustomer", "void", List.of("com.example.domain.Customer")),
                // Group 2: Methods working with product
                methodWithParams("getProduct", "com.example.domain.Product", List.of()),
                methodWithParams("setProduct", "void", List.of("com.example.domain.Product")));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Two separate groups, LCOM4 = 2 (low cohesion)
        assertThat(metric.value()).isEqualTo(2.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // Exactly at threshold
    }

    @Test
    @DisplayName("Should return three for class with three groups")
    void shouldReturnThree_forClassWithThreeGroups() {
        // Given: Aggregate with three separate groups of methods using domain types
        List<Field> fields = List.of(
                field("customer", "com.example.domain.Customer"),
                field("product", "com.example.domain.Product"),
                field("payment", "com.example.domain.Payment"));

        List<Method> methods = List.of(
                // Group 1: customer methods
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("setCustomer", "void", List.of("com.example.domain.Customer")),
                // Group 2: product methods
                methodWithParams("getProduct", "com.example.domain.Product", List.of()),
                methodWithParams("setProduct", "void", List.of("com.example.domain.Product")),
                // Group 3: payment methods
                methodWithParams("getPayment", "com.example.domain.Payment", List.of()),
                methodWithParams("setPayment", "void", List.of("com.example.domain.Payment")));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Three separate groups, LCOM4 = 3 (very low cohesion)
        assertThat(metric.value()).isEqualTo(3.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should handle aggregate with no methods")
    void shouldHandleAggregateWithNoMethods() {
        // Given: Aggregate with fields but no methods
        List<Field> fields = List.of(field("id", "java.lang.Long"));
        List<Method> methods = List.of();

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Default to 1 (cohesive) when no methods
        assertThat(metric.value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle aggregate with no fields")
    void shouldHandleAggregateWithNoFields() {
        // Given: Aggregate with methods but no fields
        List<Field> fields = List.of();
        List<Method> methods = List.of(
                methodWithParams("process", "void", List.of()), methodWithParams("validate", "boolean", List.of()));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Default to 1 (cohesive) when no fields
        assertThat(metric.value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate average for multiple aggregates")
    void shouldCalculateAverage_forMultipleAggregates() {
        // Given: Multiple aggregates with varying cohesion
        // Cohesive aggregate: all methods share customer field
        List<Field> fields1 = List.of(field("customer", "com.example.domain.Customer"));
        List<Method> methods1 = List.of(
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("setCustomer", "void", List.of("com.example.domain.Customer")));

        // Low cohesion aggregate: 3 separate groups
        List<Field> fields2 = List.of(
                field("customer", "com.example.domain.Customer"),
                field("product", "com.example.domain.Product"),
                field("payment", "com.example.domain.Payment"));
        List<Method> methods2 = List.of(
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("setCustomer", "void", List.of("com.example.domain.Customer")),
                methodWithParams("getProduct", "com.example.domain.Product", List.of()),
                methodWithParams("setProduct", "void", List.of("com.example.domain.Product")),
                methodWithParams("getPayment", "com.example.domain.Payment", List.of()),
                methodWithParams("setPayment", "void", List.of("com.example.domain.Payment")));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields1, methods1)
                .addAggregateWithFieldsAndMethods("com.example.domain.Invoice", fields2, methods2)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average of LCOM4 values (1 + 3) / 2 = 2.0
        assertThat(metric.value()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should exceed threshold when average above two")
    void shouldExceedThreshold_whenAverageAboveTwo() {
        // Given: Multiple aggregates with average LCOM4 > 2
        // Aggregate with 3 groups
        List<Field> fields1 = List.of(
                field("id", "java.lang.Long"),
                field("status", "java.lang.String"),
                field("total", "java.math.BigDecimal"));
        List<Method> methods1 = List.of(
                methodWithParams("getId", "java.lang.Long", List.of()),
                methodWithParams("getStatus", "java.lang.String", List.of()),
                methodWithParams("getTotal", "java.math.BigDecimal", List.of()));

        // Aggregate with 2 groups
        List<Field> fields2 = List.of(field("id", "java.lang.Long"), field("name", "java.lang.String"));
        List<Method> methods2 = List.of(
                methodWithParams("getId", "java.lang.Long", List.of()),
                methodWithParams("getName", "java.lang.String", List.of()));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields1, methods1)
                .addAggregateWithFieldsAndMethods("com.example.domain.Customer", fields2, methods2)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average LCOM4 = (3 + 2) / 2 = 2.5, exceeds threshold
        assertThat(metric.value()).isEqualTo(2.5);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty codebase")
    void shouldHandleEmptyCodebase() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Default to 0
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should connect methods by shared fields")
    void shouldConnectMethodsBySharedFields() {
        // Given: Methods that share fields through type matching with domain types
        List<Field> fields = List.of(
                field("customer", "com.example.domain.Customer"), field("product", "com.example.domain.Product"));

        List<Method> methods = List.of(
                // These methods share customer field by type
                methodWithParams("updateCustomer", "void", List.of("com.example.domain.Customer")),
                methodWithParams("validateCustomer", "boolean", List.of("com.example.domain.Customer")),
                // This method uses product
                methodWithParams("getProduct", "com.example.domain.Product", List.of()));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Two groups (customer methods connected, product method separate)
        assertThat(metric.value()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should handle getter/setter pattern")
    void shouldHandleGetterSetterPattern() {
        // Given: Standard getter/setter methods with non-common types
        List<Field> fields = List.of(
                field("customer", "com.example.domain.Customer"), field("product", "com.example.domain.Product"));

        List<Method> methods = List.of(
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("setCustomer", "void", List.of("com.example.domain.Customer")),
                methodWithParams("getProduct", "com.example.domain.Product", List.of()),
                methodWithParams("setProduct", "void", List.of("com.example.domain.Product")));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Two groups (customer methods and product methods)
        assertThat(metric.value()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should treat constructor as cohesive")
    void shouldTreatConstructorAsCohesive() {
        // Given: Aggregate with constructor (empty return type signifies constructor)
        List<Field> fields = List.of(
                field("customer", "com.example.domain.Customer"), field("product", "com.example.domain.Product"));

        List<Method> methods = List.of(
                // Constructor (empty return type, multiple params)
                constructor("Order", List.of("com.example.domain.Customer", "com.example.domain.Product")),
                methodWithParams("getCustomer", "com.example.domain.Customer", List.of()),
                methodWithParams("getProduct", "com.example.domain.Product", List.of()));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithFieldsAndMethods("com.example.domain.Order", fields, methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Constructor connects all methods, LCOM4 = 1
        assertThat(metric.value()).isEqualTo(1.0);
    }
}
