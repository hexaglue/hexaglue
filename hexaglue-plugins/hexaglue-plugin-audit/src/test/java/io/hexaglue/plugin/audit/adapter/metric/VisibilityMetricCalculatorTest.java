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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.syntax.Modifier;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VisibilityMetricCalculator}.
 *
 * @since 5.1.0
 */
class VisibilityMetricCalculatorTest {

    private VisibilityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new VisibilityMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.visibility.average");
    }

    @Nested
    @DisplayName("When registry not available")
    class WhenRegistryNotAvailable {

        @Test
        @DisplayName("Should return zero for empty model")
        void shouldReturnZero_forEmptyModel() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            // Empty model has a registry but no types
            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When calculating visibility")
    class WhenCalculatingVisibility {

        @Test
        @DisplayName("Should return 100% when all types are public")
        void shouldReturn100Percent_whenAllTypesPublic() {
            // All default types in TestModelBuilder are PUBLIC
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .addEntity("com.example.domain.OrderLine")
                    .addValueObject("com.example.domain.Money")
                    .build();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0% when no types are public")
        void shouldReturn0Percent_whenNoTypesPublic() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObjectWithModifiers("com.example.domain.Money", Set.of())
                    .addEntityWithModifiers("com.example.domain.OrderLine", Set.of())
                    .build();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate average across packages")
        void shouldCalculateAverage_acrossPackages() {
            // Package com.example.ports: 2 public out of 2 → RV = 100%
            // Package com.example.infra: 1 public out of 3 → RV = 33.3%
            // ARV = (100 + 33.3) / 2 = 66.67%
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.ports.OrderService")
                    .addDrivenPort(
                            "com.example.ports.OrderRepository", io.hexaglue.arch.model.DrivenPortType.REPOSITORY)
                    .addValueObjectWithModifiers("com.example.infra.JpaOrder", Set.of(Modifier.PUBLIC))
                    .addValueObjectWithModifiers("com.example.infra.JpaOrderMapper", Set.of())
                    .addValueObjectWithModifiers("com.example.infra.JpaOrderHelper", Set.of())
                    .build();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            // (100 + 33.33) / 2 = 66.67
            assertThat(metric.value()).isCloseTo(66.67, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should handle single package with mixed visibility")
        void shouldHandleSinglePackage_withMixedVisibility() {
            // 1 public out of 3 types → 33.3%
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addValueObjectWithModifiers("com.example.domain.InternalHelper", Set.of())
                    .addValueObjectWithModifiers("com.example.domain.PackagePrivate", Set.of())
                    .build();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isCloseTo(33.33, Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold for low visibility")
        void shouldNotExceedThreshold_forLowVisibility() {
            // 50% visibility → below 70% threshold
            ArchitecturalModel model = new TestModelBuilder()
                    .addValueObject("com.example.domain.Money")
                    .addValueObjectWithModifiers("com.example.domain.Internal", Set.of())
                    .build();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(50.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold for high visibility")
        void shouldExceedThreshold_forHighVisibility() {
            // All public → 100% > 70%
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.domain.Order")
                    .addEntity("com.example.domain.OrderLine")
                    .addValueObject("com.example.domain.Money")
                    .build();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(100.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
