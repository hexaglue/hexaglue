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
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AdapterIndependenceMetricCalculator}.
 *
 * @since 5.1.0
 */
class AdapterIndependenceMetricCalculatorTest {

    private AdapterIndependenceMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AdapterIndependenceMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("hexagonal.adapter.independence");
    }

    @Nested
    @DisplayName("When architecture query not available")
    class WhenQueryNotAvailable {

        @Test
        @DisplayName("Should return zero when query is null")
        void shouldReturnZero_whenQueryIsNull() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.description()).contains("architecture query not available");
        }

        @Test
        @DisplayName("Should return 100% when no ports exist")
        void shouldReturn100Percent_whenNoPortsExist() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(100.0);
            assertThat(metric.description()).contains("no adapters found");
        }
    }

    @Nested
    @DisplayName("When calculating independence")
    class WhenCalculatingIndependence {

        @Test
        @DisplayName("Should return 100% when adapters have no mutual dependencies")
        void shouldReturn100Percent_whenAdaptersAreIndependent() {
            // 2 ports, each with 1 implementor, no dependencies between adapters
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .build();
            Codebase codebase = mock(Codebase.class);

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors("com.example.port.OrderService"))
                    .thenReturn(List.of("com.example.adapter.rest.OrderController"));
            when(query.findImplementors("com.example.port.OrderRepository"))
                    .thenReturn(List.of("com.example.adapter.jpa.JpaOrderRepository"));
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.adapter.rest.OrderController",
                                    Set.of("com.example.port.OrderService", "com.example.domain.Order"),
                            "com.example.adapter.jpa.JpaOrderRepository",
                                    Set.of("com.example.port.OrderRepository", "com.example.domain.Order")));

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should detect inter-adapter dependency")
        void shouldDetectInterAdapterDependency() {
            // Adapter A depends on Adapter B
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .addDrivenPort("com.example.port.OrderRepository", DrivenPortType.REPOSITORY)
                    .build();
            Codebase codebase = mock(Codebase.class);

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors("com.example.port.OrderService"))
                    .thenReturn(List.of("com.example.adapter.rest.OrderController"));
            when(query.findImplementors("com.example.port.OrderRepository"))
                    .thenReturn(List.of("com.example.adapter.jpa.JpaOrderRepository"));
            // Controller depends on JpaOrderRepository (violation!)
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.adapter.rest.OrderController",
                                    Set.of(
                                            "com.example.port.OrderService",
                                            "com.example.adapter.jpa.JpaOrderRepository"),
                            "com.example.adapter.jpa.JpaOrderRepository", Set.of("com.example.port.OrderRepository")));

            Metric metric = calculator.calculate(model, codebase, query);

            // Controller has 2 deps (excl self): OrderService, JpaOrderRepository
            // JpaOrderRepository has 1 dep: OrderRepository
            // Total = 3, inter-adapter = 1 (Controller→JpaOrderRepository)
            // Independence = (1 - 1/3) * 100 = 66.67%
            assertThat(metric.value()).isCloseTo(66.67, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should return 100% when adapters have no external dependencies")
        void shouldReturn100Percent_whenAdaptersHaveNoDependencies() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .build();
            Codebase codebase = mock(Codebase.class);

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors("com.example.port.OrderService"))
                    .thenReturn(List.of("com.example.adapter.rest.OrderController"));
            when(query.allTypeDependencies()).thenReturn(Map.of());

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should handle driving and driven adapters independently")
        void shouldHandleDrivingAndDrivenAdapters_independently() {
            // Multiple ports with independent adapters
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderUseCase")
                    .addDrivingPort("com.example.port.CustomerUseCase")
                    .addDrivenPort("com.example.port.OrderRepo", DrivenPortType.REPOSITORY)
                    .build();
            Codebase codebase = mock(Codebase.class);

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors("com.example.port.OrderUseCase"))
                    .thenReturn(List.of("com.example.adapter.rest.OrderApi"));
            when(query.findImplementors("com.example.port.CustomerUseCase"))
                    .thenReturn(List.of("com.example.adapter.rest.CustomerApi"));
            when(query.findImplementors("com.example.port.OrderRepo"))
                    .thenReturn(List.of("com.example.adapter.jpa.JpaOrderRepo"));
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.adapter.rest.OrderApi", Set.of("com.example.domain.Order"),
                            "com.example.adapter.rest.CustomerApi", Set.of("com.example.domain.Customer"),
                            "com.example.adapter.jpa.JpaOrderRepo", Set.of("com.example.domain.Order")));

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold at 100%")
        void shouldNotExceedThreshold_at100Percent() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.OrderService")
                    .build();
            Codebase codebase = mock(Codebase.class);

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors("com.example.port.OrderService"))
                    .thenReturn(List.of("com.example.adapter.OrderImpl"));
            when(query.allTypeDependencies())
                    .thenReturn(Map.of("com.example.adapter.OrderImpl", Set.of("com.example.domain.Order")));

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(100.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold when independence is below 80%")
        void shouldExceedThreshold_whenBelowLimit() {
            // 2 adapters where one depends entirely on the other
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort("com.example.port.A")
                    .addDrivenPort("com.example.port.B", DrivenPortType.REPOSITORY)
                    .build();
            Codebase codebase = mock(Codebase.class);

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors("com.example.port.A")).thenReturn(List.of("com.example.ImplA"));
            when(query.findImplementors("com.example.port.B")).thenReturn(List.of("com.example.ImplB"));
            // ImplA depends on ImplB only (inter-adapter = 1, total = 1 → independence = 0%)
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.ImplA", Set.of("com.example.ImplB"),
                            "com.example.ImplB", Set.of()));

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
