/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.metric;

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.MethodDeclaration;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateMetricCalculator}.
 */
class AggregateMetricCalculatorTest {

    private AggregateMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AggregateMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.avgSize");
    }

    @Test
    void shouldReturnZero_whenNoAggregates() {
        // Given: Codebase with no aggregates
        Codebase codebase = withUnits(domainClass("SomeService"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.name()).isEqualTo("aggregate.avgSize");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("methods");
        assertThat(metric.description()).contains("no aggregates found");
    }

    @Test
    void shouldCalculateAverageSize_withSingleAggregate() {
        // Given: Single aggregate with 5 methods
        CodeUnit aggregate = aggregateWithMethods("Order", 5);
        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(5.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // 5 < 20
    }

    @Test
    void shouldCalculateAverageSize_withMultipleAggregates() {
        // Given: 3 aggregates with 10, 15, and 20 methods
        CodeUnit agg1 = aggregateWithMethods("Order", 10);
        CodeUnit agg2 = aggregateWithMethods("Customer", 15);
        CodeUnit agg3 = aggregateWithMethods("Product", 20);

        Codebase codebase = withUnits(agg1, agg2, agg3);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (10 + 15 + 20) / 3 = 15.0
        assertThat(metric.value()).isEqualTo(15.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // 15 < 20
    }

    @Test
    void shouldWarn_whenAverageSizeExceedsThreshold() {
        // Given: 2 aggregates with 25 and 30 methods (average = 27.5)
        CodeUnit agg1 = aggregateWithMethods("LargeAggregate1", 25);
        CodeUnit agg2 = aggregateWithMethods("LargeAggregate2", 30);

        Codebase codebase = withUnits(agg1, agg2);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average exceeds threshold of 20
        assertThat(metric.value()).isEqualTo(27.5);
        assertThat(metric.exceedsThreshold()).isTrue();
        assertThat(metric.threshold()).isPresent();
    }

    @Test
    void shouldHandleZeroMethodAggregates() {
        // Given: Aggregate with no methods
        CodeUnit aggregate = aggregateWithMethods("EmptyAggregate", 0);
        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    void shouldOnlyCountAggregateRoots_notEntities() {
        // Given: Mix of aggregates and entities
        CodeUnit aggregate = aggregateWithMethods("Order", 10);
        CodeUnit entity = entity("OrderLine", true);

        Codebase codebase = withUnits(aggregate, entity);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only aggregate counted, not entity
        assertThat(metric.value()).isEqualTo(10.0);
    }

    @Test
    void shouldCalculateCorrectly_withManyAggregates() {
        // Given: 10 aggregates with varying sizes
        TestCodebaseBuilder builder = new TestCodebaseBuilder();
        for (int i = 1; i <= 10; i++) {
            builder.addUnit(aggregateWithMethods("Aggregate" + i, i * 2));
        }
        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (2+4+6+8+10+12+14+16+18+20) / 10 = 11.0
        assertThat(metric.value()).isEqualTo(11.0);
    }

    @Test
    void shouldNotExceedThreshold_atBoundary() {
        // Given: Aggregate with exactly 20 methods (boundary)
        CodeUnit aggregate = aggregateWithMethods("BoundaryAggregate", 20);
        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Should not exceed (threshold is > 20, not >=)
        assertThat(metric.value()).isEqualTo(20.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldExceedThreshold_justAboveBoundary() {
        // Given: Aggregate with 21 methods
        CodeUnit aggregate = aggregateWithMethods("SlightlyLargeAggregate", 21);
        Codebase codebase = withUnits(aggregate);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(21.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    // === Helper Methods ===

    /**
     * Creates an aggregate with a specific number of methods.
     */
    private CodeUnit aggregateWithMethods(String simpleName, int methodCount) {
        String qualifiedName = "com.example.domain." + simpleName;
        List<MethodDeclaration> methods = new ArrayList<>();

        for (int i = 0; i < methodCount; i++) {
            methods.add(new MethodDeclaration(
                    "method" + i, "void", List.of(), Set.of("public"), Set.of(), 1));
        }

        return new CodeUnit(
                qualifiedName,
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                methods,
                List.of(),
                new CodeMetrics(50, methodCount, 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));
    }
}
