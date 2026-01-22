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
 * Tests for {@link BoilerplateMetricCalculator}.
 *
 * <p>Validates that boilerplate code ratio is correctly calculated
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class BoilerplateMetricCalculatorTest {

    private BoilerplateMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BoilerplateMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("code.boilerplate.ratio");
    }

    @Test
    @DisplayName("Should return zero when no domain types")
    void shouldReturnZero_whenNoDomainTypes() {
        // Given: Model with no domain types
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("%");
    }

    @Test
    @DisplayName("Should return zero when domain types have no methods")
    void shouldReturnZero_whenDomainTypesHaveNoMethods() {
        // Given: Domain types with no methods
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", List.of())
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no methods found");
    }

    @Test
    @DisplayName("Should return 100% when all methods are boilerplate")
    void shouldReturn100Percent_whenAllMethodsAreBoilerplate() {
        // Given: Class with only boilerplate methods
        List<Method> methods = List.of(
                getter("getName", "java.lang.String"),
                getter("getAge", "int"),
                setter("setName"),
                setter("setAge"),
                objectMethod("equals", "boolean"),
                objectMethod("hashCode", "int"),
                objectMethod("toString", "java.lang.String"),
                factoryMethod("create", "com.example.domain.Person"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Person", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 8 boilerplate / 8 total = 100%
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should return 0% when no boilerplate")
    void shouldReturn0Percent_whenNoBoilerplate() {
        // Given: Class with only domain logic methods
        List<Method> methods = List.of(
                businessMethod("placeOrder", "void"),
                businessMethod("calculateTotal", "java.math.BigDecimal"),
                businessMethod("validateInventory", "boolean"),
                businessMethod("applyDiscount", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.OrderService", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 0 boilerplate / 4 total = 0%
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should calculate mixed ratio")
    void shouldCalculateMixedRatio() {
        // Given: Class with 6 boilerplate and 4 domain methods
        List<Method> methods = List.of(
                // Boilerplate (6)
                getter("getId", "java.lang.Long"),
                getter("getName", "java.lang.String"),
                setter("setName"),
                objectMethod("equals", "boolean"),
                objectMethod("hashCode", "int"),
                factoryMethod("create", "com.example.domain.Order"),
                // Domain logic (4)
                businessMethod("placeOrder", "void"),
                businessMethod("cancel", "void"),
                businessMethod("calculateTotal", "java.math.BigDecimal"),
                businessMethod("validateRules", "boolean"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 6 boilerplate / 10 total = 60%
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should recognize getter variants")
    void shouldRecognizeGetterVariants() {
        // Given: Different getter patterns
        List<Method> methods = List.of(
                getter("getName", "java.lang.String"),
                getter("isActive", "boolean"),
                getter("getTotal", "java.math.BigDecimal"),
                businessMethod("processOrder", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Product", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 3 boilerplate / 4 total = 75%
        assertThat(metric.value()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Should recognize builder patterns")
    void shouldRecognizeBuilderPatterns() {
        // Given: Factory/builder pattern methods
        List<Method> methods = List.of(
                factoryMethod("builder", "com.example.domain.PersonBuilder"),
                factoryMethod("build", "com.example.domain.Person"),
                factoryMethod("withName", "com.example.domain.PersonBuilder"),
                factoryMethod("withAge", "com.example.domain.PersonBuilder"),
                businessMethod("validate", "boolean"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.PersonBuilder", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 4 factory methods / 5 total = 80%
        assertThat(metric.value()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Should skip interface types")
    void shouldNotCountInterfaceMethods() {
        // Given: Interface with method declarations
        List<Method> methods = List.of(getter("getName", "java.lang.String"), getter("getAge", "int"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateAsInterface("com.example.domain.Person", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Interfaces are skipped, no methods counted
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.description()).contains("no methods found");
    }

    @Test
    @DisplayName("Should average across multiple domain types")
    void shouldAverageAcrossMultipleDomainTypes() {
        // Given: Multiple domain types with different ratios
        // Class 1: 4 boilerplate / 5 total = 80%
        List<Method> methods1 = List.of(
                getter("getName", "java.lang.String"),
                setter("setName"),
                objectMethod("equals", "boolean"),
                objectMethod("hashCode", "int"),
                businessMethod("validate", "boolean"));

        // Class 2: 2 boilerplate / 6 total = 33.33%
        List<Method> methods2 = List.of(
                getter("getId", "java.lang.Long"),
                factoryMethod("create", "com.example.domain.Entity2"),
                businessMethod("process", "void"),
                businessMethod("calculate", "double"),
                businessMethod("validate", "boolean"),
                businessMethod("execute", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Entity1", methods1)
                .addEntityWithStructure("com.example.domain.Entity2", methods2)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Total = (4 + 2) boilerplate / (5 + 6) total = 6/11 = 54.54%
        assertThat(metric.value()).isCloseTo(54.54, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should warn when ratio exceeds threshold")
    void shouldWarn_whenRatioExceedsThreshold() {
        // Given: 60% boilerplate
        List<Method> methods = List.of(
                getter("getId", "java.lang.Long"),
                getter("getName", "java.lang.String"),
                setter("setName"),
                objectMethod("equals", "boolean"),
                objectMethod("hashCode", "int"),
                factoryMethod("create", "com.example.domain.TestEntity"),
                businessMethod("process", "void"),
                businessMethod("validate", "boolean"),
                businessMethod("calculate", "double"),
                businessMethod("execute", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.TestEntity", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 6/10 = 60%, exceeds 50% threshold
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should not warn at threshold boundary")
    void shouldNotWarn_atThresholdBoundary() {
        // Given: Exactly 50% boilerplate
        List<Method> methods = List.of(
                getter("getId", "java.lang.Long"),
                getter("getName", "java.lang.String"),
                setter("setName"),
                objectMethod("equals", "boolean"),
                objectMethod("hashCode", "int"),
                businessMethod("process", "void"),
                businessMethod("validate", "boolean"),
                businessMethod("calculate", "double"),
                businessMethod("execute", "void"),
                businessMethod("apply", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.TestEntity", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Exactly 50%, should not warn (threshold is >50, not >=)
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should handle record type with minimal boilerplate")
    void shouldHandleRecordType_withMinimalBoilerplate() {
        // Given: Record type with mostly domain logic
        List<Method> methods = List.of(
                businessMethod("validate", "boolean"),
                businessMethod("calculate", "java.math.BigDecimal"),
                businessMethod("apply", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateAsRecord("com.example.domain.OrderId", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Records are skipped (no boilerplate detected)
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should analyze all domain type kinds")
    void shouldAnalyzeAllDomainTypeKinds() {
        // Given: Mix of domain types with methods
        // Aggregate: 2 boilerplate, 1 business
        List<Method> aggMethods =
                List.of(getter("getName", "java.lang.String"), setter("setName"), businessMethod("process", "void"));

        // Entity: 1 boilerplate, 2 business
        List<Method> entityMethods = List.of(
                getter("getId", "java.lang.Long"),
                businessMethod("update", "void"),
                businessMethod("validate", "boolean"));

        // Value object: 1 boilerplate, 1 business
        List<Method> voMethods = List.of(getter("getValue", "int"), businessMethod("add", "com.example.domain.Money"));

        // Domain service: 0 boilerplate, 2 business
        List<Method> serviceMethods =
                List.of(businessMethod("calculate", "java.math.BigDecimal"), businessMethod("transfer", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", aggMethods)
                .addEntityWithStructure("com.example.domain.OrderLine", entityMethods)
                .addValueObjectWithStructure("com.example.domain.Money", voMethods)
                .addDomainServiceWithStructure("com.example.domain.PaymentService", serviceMethods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Total = 4 boilerplate / 10 methods = 40%
        assertThat(metric.value()).isEqualTo(40.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should recognize lifecycle methods as boilerplate")
    void shouldRecognizeLifecycleMethods_asBoilerplate() {
        // Given: Class with lifecycle callback methods
        List<Method> methods =
                List.of(lifecycleMethod("init"), lifecycleMethod("destroy"), businessMethod("process", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Service", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 2 lifecycle / 3 total = 66.67%
        assertThat(metric.value()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should handle empty codebase")
    void shouldHandleEmptyCodebase() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }
}
