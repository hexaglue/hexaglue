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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValueObjectRatioMetricCalculator}.
 *
 * @since 5.1.0
 */
class ValueObjectRatioMetricCalculatorTest {

    private ValueObjectRatioMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ValueObjectRatioMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("ddd.value.object.ratio");
    }

    @Nested
    @DisplayName("When no domain types exist")
    class WhenNoDomainTypes {

        @Test
        @DisplayName("Should return zero for empty model")
        void shouldReturnZero_forEmptyModel() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.unit()).isEqualTo("%");
            assertThat(metric.description()).contains("no entities or value objects found");
        }

        @Test
        @DisplayName("Should return zero when only aggregates")
        void shouldReturnZero_whenOnlyAggregates() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When calculating ratio")
    class WhenCalculatingRatio {

        @Test
        @DisplayName("Should return 100% when all value objects")
        void shouldReturn100Percent_whenAllValueObjects() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addValueObject("com.example.domain.Address")
                    .addValueObject("com.example.domain.Email")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(100.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should return 0% when all entities")
        void shouldReturn0Percent_whenAllEntities() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addEntity("com.example.domain.OrderLine")
                    .addEntity("com.example.domain.Product")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }

        @Test
        @DisplayName("Should calculate 60% with 3 VO and 2 entities")
        void shouldCalculate60Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addValueObject("com.example.domain.Address")
                    .addValueObject("com.example.domain.Email")
                    .addEntity("com.example.domain.OrderLine")
                    .addEntity("com.example.domain.Product")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(60.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should calculate 20% with 1 VO and 4 entities")
        void shouldCalculate20Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addEntity("com.example.domain.OrderLine")
                    .addEntity("com.example.domain.Product")
                    .addEntity("com.example.domain.Category")
                    .addEntity("com.example.domain.Customer")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(20.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold at 40%")
        void shouldNotExceedThreshold_at40Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addValueObject("com.example.domain.Address")
                    .addEntity("com.example.domain.OrderLine")
                    .addEntity("com.example.domain.Product")
                    .addEntity("com.example.domain.Category")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(40.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold below 40%")
        void shouldExceedThreshold_below40Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addEntity("com.example.domain.OrderLine")
                    .addEntity("com.example.domain.Product")
                    .addEntity("com.example.domain.Category")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(25.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }

    @Nested
    @DisplayName("When ignoring non-VO/entity types")
    class WhenIgnoringOtherTypes {

        @Test
        @DisplayName("Should not count aggregate roots in the ratio")
        void shouldNotCountAggregateRoots() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .addValueObject("com.example.domain.Money")
                    .addEntity("com.example.domain.OrderLine")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            // 1 VO / (1 Entity + 1 VO) = 50%
            assertThat(metric.value()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should not count domain services or ports")
        void shouldNotCountServicesOrPorts() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addEntity("com.example.domain.OrderLine")
                    .addDomainService("com.example.domain.PricingService")
                    .addDrivingPort("com.example.port.OrderUseCase")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            // 1 VO / (1 Entity + 1 VO) = 50%
            assertThat(metric.value()).isEqualTo(50.0);
        }
    }
}
