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
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainEventCoverageMetricCalculator}.
 *
 * @since 5.1.0
 */
class DomainEventCoverageMetricCalculatorTest {

    private DomainEventCoverageMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DomainEventCoverageMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("ddd.event.coverage");
    }

    @Nested
    @DisplayName("When no aggregates exist")
    class WhenNoAggregates {

        @Test
        @DisplayName("Should return zero for empty model")
        void shouldReturnZero_forEmptyModel() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.description()).contains("no aggregates found");
        }
    }

    @Nested
    @DisplayName("When checking event coverage")
    class WhenCheckingEventCoverage {

        @Test
        @DisplayName("Should return 100% when all aggregates emit events")
        void shouldReturn100Percent_whenAllAggregatesEmitEvents() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRootWithEvents(
                            "com.example.domain.Order", List.of("com.example.domain.OrderCreatedEvent"))
                    .addAggregateRootWithEvents(
                            "com.example.domain.Customer", List.of("com.example.domain.CustomerRegisteredEvent"))
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(100.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should return 0% when no aggregates emit events")
        void shouldReturn0Percent_whenNoAggregatesEmitEvents() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .addAggregateRoot("com.example.domain.Customer")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }

        @Test
        @DisplayName("Should calculate 50% with mixed coverage")
        void shouldCalculate50Percent_withMixedCoverage() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRootWithEvents(
                            "com.example.domain.Order", List.of("com.example.domain.OrderCreatedEvent"))
                    .addAggregateRoot("com.example.domain.Customer")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(50.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should calculate 66.7% with 2 of 3 aggregates emitting events")
        void shouldCalculate66Percent_with2of3() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRootWithEvents(
                            "com.example.domain.Order", List.of("com.example.domain.OrderCreatedEvent"))
                    .addAggregateRootWithEvents(
                            "com.example.domain.Customer", List.of("com.example.domain.CustomerRegisteredEvent"))
                    .addAggregateRoot("com.example.domain.Product")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isCloseTo(66.67, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should count aggregate with multiple events as one")
        void shouldCountAggregateWithMultipleEvents_asOne() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRootWithEvents(
                            "com.example.domain.Order",
                            List.of(
                                    "com.example.domain.OrderCreatedEvent",
                                    "com.example.domain.OrderCancelledEvent",
                                    "com.example.domain.OrderShippedEvent"))
                    .addAggregateRoot("com.example.domain.Customer")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            // 1 out of 2 aggregates emit events = 50%
            assertThat(metric.value()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold at 50%")
        void shouldNotExceedThreshold_at50Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRootWithEvents(
                            "com.example.domain.Order", List.of("com.example.domain.OrderCreatedEvent"))
                    .addAggregateRoot("com.example.domain.Customer")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(50.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold below 50%")
        void shouldExceedThreshold_below50Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRootWithEvents(
                            "com.example.domain.Order", List.of("com.example.domain.OrderCreatedEvent"))
                    .addAggregateRoot("com.example.domain.Customer")
                    .addAggregateRoot("com.example.domain.Product")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            // 1 / 3 = 33.3%
            assertThat(metric.value()).isCloseTo(33.33, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
