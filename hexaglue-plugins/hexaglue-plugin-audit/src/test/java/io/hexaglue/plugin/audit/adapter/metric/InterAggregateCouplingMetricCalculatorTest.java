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
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InterAggregateCouplingMetricCalculator}.
 *
 * <p>Validates that cross-aggregate dependencies are counted correctly
 * and averaged per aggregate.
 *
 * @since 5.1.0
 */
class InterAggregateCouplingMetricCalculatorTest {

    private InterAggregateCouplingMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new InterAggregateCouplingMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("ddd.aggregate.coupling");
    }

    @Nested
    @DisplayName("When no aggregates")
    class WhenNoAggregates {

        @Test
        @DisplayName("Should return zero for empty model")
        void shouldReturnZero_forEmptyModel() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return zero for single aggregate")
        void shouldReturnZero_forSingleAggregate() {
            ArchitecturalModel model =
                    new TestModelBuilder().addAggregateRoot("com.example.Order").build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When calculating coupling")
    class WhenCalculatingCoupling {

        @Test
        @DisplayName("Should return zero for isolated aggregates")
        void shouldReturnZero_forIsolatedAggregates() {
            // Two aggregates with no cross-dependencies
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.Order")
                    .addAggregateRoot("com.example.Customer")
                    .build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.Order", Set.of(),
                            "com.example.Customer", Set.of()));

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(0.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should count entity-to-root cross-aggregate dep")
        void shouldCount_entityToRootCrossAggregateDep() {
            // Order aggregate has entity OrderLine
            // OrderLine depends on Customer (another aggregate root)
            // → 1 cross-dep / 2 aggregates = 0.5
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.Order", List.of("com.example.OrderLine"), List.of())
                    .addEntity("com.example.OrderLine", "com.example.Order")
                    .addAggregateRoot("com.example.Customer")
                    .build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("com.example.Order", Set.of());
            deps.put("com.example.OrderLine", Set.of("com.example.Customer"));
            deps.put("com.example.Customer", Set.of());
            when(query.allTypeDependencies()).thenReturn(deps);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(0.5, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should not count intra-aggregate dependencies")
        void shouldNotCount_intraAggregateDeps() {
            // Order → OrderLine (same aggregate) should not count
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.Order", List.of("com.example.OrderLine"), List.of())
                    .addEntity("com.example.OrderLine", "com.example.Order")
                    .addAggregateRoot("com.example.Customer")
                    .build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("com.example.Order", Set.of("com.example.OrderLine"));
            deps.put("com.example.OrderLine", Set.of());
            deps.put("com.example.Customer", Set.of());
            when(query.allTypeDependencies()).thenReturn(deps);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(0.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should count deps from value objects in aggregate")
        void shouldCount_valueObjectCrossDeps() {
            // Order aggregate has VO Money, Money depends on Customer
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.Order", List.of(), List.of("com.example.Money"))
                    .addValueObject("com.example.Money")
                    .addAggregateRoot("com.example.Customer")
                    .build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("com.example.Order", Set.of());
            deps.put("com.example.Money", Set.of("com.example.Customer"));
            deps.put("com.example.Customer", Set.of());
            when(query.allTypeDependencies()).thenReturn(deps);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(0.5, Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("When checking threshold")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold when coupling is zero")
        void shouldNotExceedThreshold_whenZero() {
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot("com.example.Order")
                    .addAggregateRoot("com.example.Customer")
                    .build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.Order", Set.of(),
                            "com.example.Customer", Set.of()));

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold when coupling is high")
        void shouldExceedThreshold_whenHigh() {
            // Order aggregate has 4 members (root + 3 entities) all depending on Customer
            // Customer depends on all 4 Order members
            // Order agg: 4 cross-deps, Customer agg: 4 cross-deps → 8 / 2 = 4.0 > 3.0
            ArchitecturalModel model = new TestModelBuilder()
                    .addAggregateRoot(
                            "com.example.Order",
                            List.of("com.example.OrderLine", "com.example.OrderItem", "com.example.OrderStatus"),
                            List.of())
                    .addEntity("com.example.OrderLine", "com.example.Order")
                    .addEntity("com.example.OrderItem", "com.example.Order")
                    .addEntity("com.example.OrderStatus", "com.example.Order")
                    .addAggregateRoot("com.example.Customer")
                    .build();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("com.example.Order", Set.of("com.example.Customer"));
            deps.put("com.example.OrderLine", Set.of("com.example.Customer"));
            deps.put("com.example.OrderItem", Set.of("com.example.Customer"));
            deps.put("com.example.OrderStatus", Set.of("com.example.Customer"));
            deps.put(
                    "com.example.Customer",
                    Set.of(
                            "com.example.Order",
                            "com.example.OrderLine",
                            "com.example.OrderItem",
                            "com.example.OrderStatus"));
            when(query.allTypeDependencies()).thenReturn(deps);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(4.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
