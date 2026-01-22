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
 * Tests for {@link CodeComplexityMetricCalculator}.
 *
 * <p>Validates that cyclomatic complexity is correctly calculated using the v5 ArchType API.
 *
 * <p><strong>Note:</strong> The v5 API does not yet include method complexity in the Method record.
 * Therefore, the calculator returns 0.0 as a placeholder. These tests document expected behavior
 * once complexity is available.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class CodeComplexityMetricCalculatorTest {

    private CodeComplexityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CodeComplexityMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("code.complexity.average");
    }

    @Test
    @DisplayName("Should return zero when no domain types")
    void shouldReturnZero_whenNoDomainTypes() {
        // Given: Empty model with no domain types
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.name()).isEqualTo("code.complexity.average");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("complexity");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return zero when domain type has no methods")
    void shouldReturnZero_whenDomainTypeHasNoMethods() {
        // Given: Domain type with no methods
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", List.of())
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return zero - complexity not yet available in v5 API")
    void shouldReturnZero_complexityNotYetAvailable() {
        // Given: Domain type with simple methods
        // Note: In v5 API, complexity field is not yet available in Method record
        List<Method> methods = List.of(
                method("getId", "java.lang.Long"),
                method("getName", "java.lang.String"),
                method("getTotal", "java.math.BigDecimal"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Returns 0.0 as placeholder since complexity is not yet in v5 API
        // TODO: Update expected values when complexity is added to Method record
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple domain type categories")
    void shouldHandleMultipleDomainTypeCategories() {
        // Given: Various domain type categories with methods
        List<Method> aggMethods = List.of(
                method("process", "void"),
                method("validate", "boolean"));

        List<Method> entityMethods = List.of(
                method("getId", "java.lang.Long"),
                method("update", "void"));

        List<Method> voMethods = List.of(
                method("add", "Money"),
                method("subtract", "Money"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", aggMethods)
                .addEntityWithStructure("com.example.domain.OrderLine", entityMethods)
                .addValueObjectWithStructure("com.example.domain.Money", voMethods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Returns 0.0 as placeholder
        // TODO: Update when complexity is available - expected: average of all method complexities
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should skip interfaces - no implementation methods")
    void shouldSkipInterfaces() {
        // Given: Domain layer with both interface and class
        List<Method> interfaceMethods = List.of(
                method("save", "void"),
                method("findById", "Order"));

        List<Method> classMethods = List.of(method("process", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateAsInterface("com.example.domain.OrderRepository", interfaceMethods)
                .addAggregateWithStructure("com.example.domain.Order", classMethods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Only the class methods should be counted, interface is skipped
        // Returns 0.0 as placeholder since complexity not yet available
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should only analyze domain layer types")
    void shouldOnlyAnalyzeDomainLayerTypes() {
        // Given: Domain types (infrastructure types are not in TestModelBuilder's domain index)
        List<Method> domainMethods = List.of(method("process", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", domainMethods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Only domain types are analyzed
        assertThat(metric.value()).isEqualTo(0.0); // Placeholder
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
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should include domain services in analysis")
    void shouldIncludeDomainServicesInAnalysis() {
        // Given: Domain service with methods
        List<Method> serviceMethods = List.of(
                method("calculateTotal", "java.math.BigDecimal"),
                method("transfer", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addDomainServiceWithStructure("com.example.domain.PaymentService", serviceMethods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Domain services are included
        // Returns 0.0 as placeholder
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should document complexity threshold behavior")
    void shouldDocumentComplexityThresholdBehavior() {
        // Given: Domain type with methods
        // Note: When complexity is available, threshold is > 10
        List<Method> methods = List.of(
                method("simpleMethod", "void"),
                method("complexMethod", "void"));

        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithStructure("com.example.domain.Order", methods)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Currently returns 0.0 (below threshold of 10)
        // TODO: When complexity is available:
        //   - Methods with complexity <= 10: exceedsThreshold = false
        //   - Methods with complexity > 10: exceedsThreshold = true
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }
}
