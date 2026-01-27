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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.CouplingMetrics;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CouplingMetricCalculator}.
 *
 * <p>Validates that aggregate coupling is correctly calculated using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class CouplingMetricCalculatorTest {

    private CouplingMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CouplingMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.coupling.efferent");
    }

    @Test
    @DisplayName("Should return zero when no aggregates")
    void shouldReturnZero_whenNoAggregates() {
        // Given: Model with no aggregates
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainService("com.example.domain.SomeService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.name()).isEqualTo("aggregate.coupling.efferent");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("dependencies");
    }

    @Test
    @DisplayName("Should return zero when no aggregate interconnections")
    void shouldReturnZero_whenNoAggregateInterconnections() {
        // Given: 3 isolated aggregates with no dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addAggregateRoot("com.example.domain.Product")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should calculate coupling with single dependency")
    void shouldCalculateCoupling_withSingleDependency() {
        // Given: Order depends on Customer
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average coupling = (1 + 0) / 2 = 0.5
        assertThat(metric.value()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should calculate coupling with multiple dependencies")
    void shouldCalculateCoupling_withMultipleDependencies() {
        // Given: Order → Customer, Order → Product
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addAggregateRoot("com.example.domain.Product")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Order", "com.example.domain.Product")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average = (2 + 0 + 0) / 3 = 0.67 (approx)
        assertThat(metric.value()).isCloseTo(0.67, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should ignore non-aggregate dependencies")
    void shouldIgnoreNonAggregateDependencies() {
        // Given: Order depends on entity and aggregate
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addEntity("com.example.domain.OrderLine")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Only aggregate dependency counted, entity ignored
        // Average = (1 + 0) / 2 = 0.5
        assertThat(metric.value()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should ignore self-dependencies")
    void shouldIgnoreSelfDependencies() {
        // Given: Aggregate with self-reference (shouldn't happen, but defensive)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Order")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Self-dependency ignored
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle high coupling scenario")
    void shouldHandleHighCouplingScenario() {
        // Given: Aggregate with 4 aggregate dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.MainAggregate")
                .addAggregateRoot("com.example.domain.Aggregate1")
                .addAggregateRoot("com.example.domain.Aggregate2")
                .addAggregateRoot("com.example.domain.Aggregate3")
                .addAggregateRoot("com.example.domain.Aggregate4")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate1")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate2")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate3")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate4")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average = (4 + 0 + 0 + 0 + 0) / 5 = 0.8
        assertThat(metric.value()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("Should calculate correctly with bidirectional dependencies")
    void shouldCalculateCorrectly_withBidirectionalDependencies() {
        // Given: Order ↔ Customer (both depend on each other)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .addDependency("com.example.domain.Customer", "com.example.domain.Order")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Both have efferent coupling of 1, average = (1 + 1) / 2 = 1.0
        assertThat(metric.value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should not exceed threshold at boundary")
    void shouldNotExceedThreshold_atBoundary() {
        // Given: Aggregate with exactly 3 dependencies (boundary)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.MainAggregate")
                .addAggregateRoot("com.example.domain.Aggregate1")
                .addAggregateRoot("com.example.domain.Aggregate2")
                .addAggregateRoot("com.example.domain.Aggregate3")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate1")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate2")
                .addDependency("com.example.domain.MainAggregate", "com.example.domain.Aggregate3")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average = (3 + 0 + 0 + 0) / 4 = 0.75, doesn't exceed threshold
        assertThat(metric.value()).isEqualTo(0.75);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should calculate correctly with complex dependency graph")
    void shouldCalculateCorrectly_withComplexDependencyGraph() {
        // Given: Complex interconnected aggregates
        // A→B,C,D,E (4 deps), B→A,C (2 deps), C→A (1 dep), D→A (1 dep), E→empty (0 deps)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.AggregateA")
                .addAggregateRoot("com.example.domain.AggregateB")
                .addAggregateRoot("com.example.domain.AggregateC")
                .addAggregateRoot("com.example.domain.AggregateD")
                .addAggregateRoot("com.example.domain.AggregateE")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
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
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average = (4 + 2 + 1 + 1 + 0) / 5 = 1.6
        assertThat(metric.value()).isEqualTo(1.6);
    }

    // === Tests with ArchitectureQuery ===

    @Test
    @DisplayName("Should calculate with ArchitectureQuery when query provided")
    void shouldCalculateWithArchitectureQuery_whenQueryProvided() {
        // Given: Mock ArchitectureQuery returning coupling metrics
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);
        ArchitecturalModel model = TestModelBuilder.emptyModel();

        CouplingMetrics pkg1Metrics = new CouplingMetrics("com.example.pkg1", 2, 3, 0.4);
        CouplingMetrics pkg2Metrics = new CouplingMetrics("com.example.pkg2", 3, 2, 0.6);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(pkg1Metrics, pkg2Metrics));

        // When
        Metric metric = calculator.calculate(model, mockCodebase, mockQuery);

        // Then: Uses Core's metrics, average instability = (0.6 + 0.4) / 2 = 0.5
        assertThat(metric.name()).isEqualTo("aggregate.coupling.efferent");
        assertThat(metric.value()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.unit()).isEqualTo("ratio");
    }

    @Test
    @DisplayName("Should return zero with ArchitectureQuery when no packages")
    void shouldReturnZeroWithArchitectureQuery_whenNoPackages() {
        // Given: Mock ArchitectureQuery returning empty list
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);
        ArchitecturalModel model = TestModelBuilder.emptyModel();

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of());

        // When
        Metric metric = calculator.calculate(model, mockCodebase, mockQuery);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should count problematic packages with ArchitectureQuery")
    void shouldCountProblematicPackagesWithArchitectureQuery() {
        // Given: Some problematic packages
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);
        ArchitecturalModel model = TestModelBuilder.emptyModel();

        // Problematic: A=0, I=0 -> ZONE_OF_PAIN
        CouplingMetrics problematic = new CouplingMetrics("com.example.pain", 5, 0, 0.0);
        // Healthy: A=0.5, I=0.5 -> IDEAL
        CouplingMetrics healthy = new CouplingMetrics("com.example.ideal", 1, 1, 0.5);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(problematic, healthy));

        // When
        Metric metric = calculator.calculate(model, mockCodebase, mockQuery);

        // Then: Description should mention problematic packages
        assertThat(metric.description()).contains("1");
        assertThat(metric.description()).containsIgnoringCase("problematic");
    }

    @Test
    @DisplayName("Should NOT exceed threshold when low instability with ArchitectureQuery")
    void shouldNotExceedThreshold_whenLowInstabilityWithArchitectureQuery() {
        // Given: Mock ArchitectureQuery with LOW instability (good coupling)
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);
        ArchitecturalModel model = TestModelBuilder.emptyModel();

        // CouplingMetrics(packageName, Ca, Ce, abstractness)
        // Instability I = Ce / (Ca + Ce)
        // pkg1: Ca=4, Ce=1 → I = 1/5 = 0.2
        // pkg2: Ca=3, Ce=1 → I = 1/4 = 0.25
        CouplingMetrics pkg1Metrics = new CouplingMetrics("com.example.pkg1", 4, 1, 0.5);
        CouplingMetrics pkg2Metrics = new CouplingMetrics("com.example.pkg2", 3, 1, 0.5);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(pkg1Metrics, pkg2Metrics));

        // When
        Metric metric = calculator.calculate(model, mockCodebase, mockQuery);

        // Then: Average instability = (0.2 + 0.25) / 2 = 0.225, well below threshold of 0.7
        assertThat(metric.value()).isCloseTo(0.225, org.assertj.core.data.Offset.offset(0.01));
        // BUG H3: This should be FALSE (low instability is good), but bug causes TRUE
        assertThat(metric.exceedsThreshold())
                .as("Low instability (0.225) should NOT exceed threshold (0.7)")
                .isFalse();
    }

    @Test
    @DisplayName("Should exceed threshold when HIGH instability with ArchitectureQuery")
    void shouldExceedThreshold_whenHighInstabilityWithArchitectureQuery() {
        // Given: Mock ArchitectureQuery with HIGH instability (bad coupling)
        ArchitectureQuery mockQuery = mock(ArchitectureQuery.class);
        Codebase mockCodebase = mock(Codebase.class);
        ArchitecturalModel model = TestModelBuilder.emptyModel();

        // CouplingMetrics(packageName, Ca, Ce, abstractness)
        // Instability I = Ce / (Ca + Ce)
        // pkg1: Ca=1, Ce=4 → I = 4/5 = 0.8
        // pkg2: Ca=1, Ce=9 → I = 9/10 = 0.9
        CouplingMetrics pkg1Metrics = new CouplingMetrics("com.example.pkg1", 1, 4, 0.5);
        CouplingMetrics pkg2Metrics = new CouplingMetrics("com.example.pkg2", 1, 9, 0.5);

        when(mockQuery.analyzeAllPackageCoupling()).thenReturn(List.of(pkg1Metrics, pkg2Metrics));

        // When
        Metric metric = calculator.calculate(model, mockCodebase, mockQuery);

        // Then: Average instability = (0.8 + 0.9) / 2 = 0.85, above threshold of 0.7
        assertThat(metric.value()).isCloseTo(0.85, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold())
                .as("High instability (0.85) SHOULD exceed threshold (0.7)")
                .isTrue();
    }

    @Test
    @DisplayName("Should fallback to legacy when ArchitectureQuery is null")
    void shouldFallbackToLegacy_whenArchitectureQueryIsNull() {
        // Given: Model with aggregates but no ArchitectureQuery
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Falls back to legacy calculation
        assertThat(metric.name()).isEqualTo("aggregate.coupling.efferent");
        assertThat(metric.value()).isEqualTo(0.5); // Same as legacy test
        assertThat(metric.unit()).isEqualTo("dependencies");
    }
}
