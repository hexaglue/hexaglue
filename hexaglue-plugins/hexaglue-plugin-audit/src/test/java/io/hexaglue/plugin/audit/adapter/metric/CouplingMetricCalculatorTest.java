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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CouplingMetrics;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CouplingMetricCalculator}.
 */
class CouplingMetricCalculatorTest {

    private CouplingMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CouplingMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.coupling.efferent");
    }

    @Test
    void shouldReturnZero_whenNoAggregates() {
        // Given: Codebase with no aggregates
        Codebase codebase = withUnits(domainClass("SomeService"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.name()).isEqualTo("aggregate.coupling.efferent");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("dependencies");
    }

    @Test
    void shouldReturnZero_whenNoAggregateInterconnections() {
        // Given: 3 isolated aggregates with no dependencies
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");
        CodeUnit product = aggregate("Product");

        Codebase codebase = withUnits(order, customer, product);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateCoupling_withSingleDependency() {
        // Given: Order depends on Customer
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(customer)
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average coupling = (1 + 0) / 2 = 0.5
        assertThat(metric.value()).isEqualTo(0.5);
    }

    @Test
    void shouldCalculateCoupling_withMultipleDependencies() {
        // Given: Order → Customer, Order → Product
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");
        CodeUnit product = aggregate("Product");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(customer)
                .addUnit(product)
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Order", "com.example.domain.Product")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (2 + 0 + 0) / 3 = 0.67 (approx)
        assertThat(metric.value()).isCloseTo(0.67, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void shouldIgnoreNonAggregateDependencies() {
        // Given: Order depends on entity and aggregate
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");
        CodeUnit orderLine = entity("OrderLine", true);

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(customer)
                .addUnit(orderLine)
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only aggregate dependency counted, entity ignored
        // Average = (1 + 0) / 2 = 0.5
        assertThat(metric.value()).isEqualTo(0.5);
    }

    @Test
    void shouldIgnoreSelfDependencies() {
        // Given: Aggregate with self-reference (shouldn't happen, but defensive)
        CodeUnit order = aggregate("Order");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addDependency("com.example.domain.Order", "com.example.domain.Order")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Self-dependency ignored
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    void shouldWarn_whenCouplingExceedsThreshold() {
        // Given: Aggregate with 4 aggregate dependencies (exceeds threshold of 3)
        CodeUnit mainAggregate = aggregate("MainAggregate");
        CodeUnit agg1 = aggregate("Aggregate1");
        CodeUnit agg2 = aggregate("Aggregate2");
        CodeUnit agg3 = aggregate("Aggregate3");
        CodeUnit agg4 = aggregate("Aggregate4");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(mainAggregate)
                .addUnit(agg1)
                .addUnit(agg2)
                .addUnit(agg3)
                .addUnit(agg4)
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate1")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate2")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate3")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate4")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (4 + 0 + 0 + 0 + 0) / 5 = 0.8
        // But mainAggregate alone has 4, which exceeds threshold
        assertThat(metric.value()).isEqualTo(0.8);
    }

    @Test
    void shouldCalculateCorrectly_withBidirectionalDependencies() {
        // Given: Order ↔ Customer (both depend on each other)
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(customer)
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Customer", "com.example.domain.Order")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Both have efferent coupling of 1, average = (1 + 1) / 2 = 1.0
        assertThat(metric.value()).isEqualTo(1.0);
    }

    @Test
    void shouldNotExceedThreshold_atBoundary() {
        // Given: Aggregate with exactly 3 dependencies (boundary)
        CodeUnit mainAggregate = aggregate("MainAggregate");
        CodeUnit agg1 = aggregate("Aggregate1");
        CodeUnit agg2 = aggregate("Aggregate2");
        CodeUnit agg3 = aggregate("Aggregate3");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(mainAggregate)
                .addUnit(agg1)
                .addUnit(agg2)
                .addUnit(agg3)
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate1")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate2")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate3")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (3 + 0 + 0 + 0) / 4 = 0.75, doesn't exceed threshold
        assertThat(metric.value()).isEqualTo(0.75);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldExceedThreshold_whenAverageAboveThree() {
        // Given: All aggregates highly coupled
        // A→B,C,D,E (4 deps), B→A,C (2 deps), C→A (1 dep), D→A (1 dep), E→empty (0 deps)
        CodeUnit aggA = aggregate("AggregateA");
        CodeUnit aggB = aggregate("AggregateB");
        CodeUnit aggC = aggregate("AggregateC");
        CodeUnit aggD = aggregate("AggregateD");
        CodeUnit aggE = aggregate("AggregateE");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggA)
                .addUnit(aggB)
                .addUnit(aggC)
                .addUnit(aggD)
                .addUnit(aggE)
                .addDependency("com.example.domain.AggregateA", "com.example.domain.AggregateB")
                .addDependency("com.example.domain.AggregateA", "com.example.domain.AggregateC")
                .addDependency("com.example.domain.AggregateA", "com.example.domain.AggregateD")
                .addDependency("com.example.domain.AggregateA", "com.example.domain.AggregateE")
                .addDependency("com.example.domain.AggregateB", "com.example.domain.AggregateA")
                .addDependency("com.example.domain.AggregateB", "com.example.domain.AggregateC")
                .addDependency("com.example.domain.AggregateC", "com.example.domain.AggregateA")
                .addDependency("com.example.domain.AggregateD", "com.example.domain.AggregateA")
                .build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Average = (4 + 2 + 1 + 1 + 0) / 5 = 1.6, still under 3
        assertThat(metric.value()).isEqualTo(1.6);
    }

    // === Tests with ArchitectureQuery (v3 refactored) ===

    @Test
    void shouldCalculateWithArchitectureQuery_whenQueryProvided() {
        // Given: Mock ArchitectureQuery returning coupling metrics
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);

        CouplingMetrics pkg1Metrics = new CouplingMetrics("com.example.pkg1", 2, 3, 0.4);
        CouplingMetrics pkg2Metrics = new CouplingMetrics("com.example.pkg2", 3, 2, 0.6);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(pkg1Metrics, pkg2Metrics));

        // When
        Metric metric = calculator.calculate(mockCodebase, mockQuery);

        // Then: Uses Core's metrics, average instability = (0.6 + 0.4) / 2 = 0.5
        assertThat(metric.name()).isEqualTo("aggregate.coupling.efferent");
        assertThat(metric.value()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.unit()).isEqualTo("ratio");
    }

    @Test
    void shouldReturnZeroWithArchitectureQuery_whenNoPackages() {
        // Given: Mock ArchitectureQuery returning empty list
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of());

        // When
        Metric metric = calculator.calculate(mockCodebase, mockQuery);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    void shouldCountProblematicPackagesWithArchitectureQuery() {
        // Given: Some problematic packages
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);

        // Problematic: A=0, I=0 -> ZONE_OF_PAIN
        CouplingMetrics problematic = new CouplingMetrics("com.example.pain", 5, 0, 0.0);
        // Healthy: A=0.5, I=0.5 -> IDEAL
        CouplingMetrics healthy = new CouplingMetrics("com.example.ideal", 1, 1, 0.5);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(problematic, healthy));

        // When
        Metric metric = calculator.calculate(mockCodebase, mockQuery);

        // Then: Description should mention problematic packages
        assertThat(metric.description()).contains("1");
        assertThat(metric.description()).containsIgnoringCase("problematic");
    }

    @Test
    void shouldFallbackToLegacy_whenArchitectureQueryIsNull() {
        // Given: Codebase with aggregates but no ArchitectureQuery
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(order)
                .addUnit(customer)
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .build();

        // When
        Metric metric = calculator.calculate(codebase, null);

        // Then: Falls back to legacy calculation
        assertThat(metric.name()).isEqualTo("aggregate.coupling.efferent");
        assertThat(metric.value()).isEqualTo(0.5); // Same as legacy test
        assertThat(metric.unit()).isEqualTo("dependencies");
    }
}
