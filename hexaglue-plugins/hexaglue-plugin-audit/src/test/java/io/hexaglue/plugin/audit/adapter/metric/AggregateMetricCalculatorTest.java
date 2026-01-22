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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AggregateMetricCalculator}.
 *
 * <p>Validates that aggregate size metrics are correctly calculated
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class AggregateMetricCalculatorTest {

    private AggregateMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AggregateMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.avgSize");
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
        assertThat(metric.name()).isEqualTo("aggregate.avgSize");
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("methods");
        assertThat(metric.description()).contains("no aggregates found");
    }

    @Test
    @DisplayName("Should calculate average size with single aggregate")
    void shouldCalculateAverageSize_withSingleAggregate() {
        // Given: Single aggregate with 5 methods
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.Order", 5)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(5.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // 5 < 20
    }

    @Test
    @DisplayName("Should calculate average size with multiple aggregates")
    void shouldCalculateAverageSize_withMultipleAggregates() {
        // Given: 3 aggregates with 10, 15, and 20 methods
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.Order", 10)
                .addAggregateWithMethods("com.example.domain.Customer", 15)
                .addAggregateWithMethods("com.example.domain.Product", 20)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average = (10 + 15 + 20) / 3 = 15.0
        assertThat(metric.value()).isEqualTo(15.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // 15 < 20
    }

    @Test
    @DisplayName("Should warn when average size exceeds threshold")
    void shouldWarn_whenAverageSizeExceedsThreshold() {
        // Given: 2 aggregates with 25 and 30 methods (average = 27.5)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.LargeAggregate1", 25)
                .addAggregateWithMethods("com.example.domain.LargeAggregate2", 30)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average exceeds threshold of 20
        assertThat(metric.value()).isEqualTo(27.5);
        assertThat(metric.exceedsThreshold()).isTrue();
        assertThat(metric.threshold()).isPresent();
    }

    @Test
    @DisplayName("Should handle zero method aggregates")
    void shouldHandleZeroMethodAggregates() {
        // Given: Aggregate with no methods
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.EmptyAggregate", 0)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should only count aggregate roots, not entities")
    void shouldOnlyCountAggregateRoots_notEntities() {
        // Given: Mix of aggregates and entities
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.Order", 10)
                .addEntity("com.example.domain.OrderLine") // Not counted
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Only aggregate counted, not entity
        assertThat(metric.value()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should calculate correctly with many aggregates")
    void shouldCalculateCorrectly_withManyAggregates() {
        // Given: 10 aggregates with varying sizes
        TestModelBuilder builder = new TestModelBuilder();
        for (int i = 1; i <= 10; i++) {
            builder.addAggregateWithMethods("com.example.domain.Aggregate" + i, i * 2);
        }
        ArchitecturalModel model = builder.build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Average = (2+4+6+8+10+12+14+16+18+20) / 10 = 11.0
        assertThat(metric.value()).isEqualTo(11.0);
    }

    @Test
    @DisplayName("Should not exceed threshold at boundary")
    void shouldNotExceedThreshold_atBoundary() {
        // Given: Aggregate with exactly 20 methods (boundary)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.BoundaryAggregate", 20)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Should not exceed (threshold is > 20, not >=)
        assertThat(metric.value()).isEqualTo(20.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should exceed threshold just above boundary")
    void shouldExceedThreshold_justAboveBoundary() {
        // Given: Aggregate with 21 methods
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateWithMethods("com.example.domain.SlightlyLargeAggregate", 21)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(21.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }
}
