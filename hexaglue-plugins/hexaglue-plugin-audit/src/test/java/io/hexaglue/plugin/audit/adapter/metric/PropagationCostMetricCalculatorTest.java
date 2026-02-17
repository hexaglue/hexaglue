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
import java.util.Map;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PropagationCostMetricCalculator}.
 *
 * @since 5.1.0
 */
class PropagationCostMetricCalculatorTest {

    private PropagationCostMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PropagationCostMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.propagation.cost");
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
        @DisplayName("Should return zero when no dependencies")
        void shouldReturnZero_whenNoDependencies() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(Map.of());

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When calculating propagation cost")
    class WhenCalculatingPropagationCost {

        @Test
        @DisplayName("Should calculate for isolated types (no edges)")
        void shouldCalculate_forIsolatedTypes() {
            // Given: 3 types with no dependencies between them
            // Each type can reach only itself → 3 reachable / 9 total = 33.3%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of(),
                            "B", Set.of(),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            // Each of 3 types reaches only itself → 3/9 = 33.3%
            assertThat(metric.value()).isCloseTo(33.33, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should calculate for linear chain A→B→C")
        void shouldCalculate_forLinearChain() {
            // Given: A→B→C
            // A reaches {A, B, C} = 3
            // B reaches {B, C} = 2
            // C reaches {C} = 1
            // Total = 6 / 9 = 66.7%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("C"),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(66.67, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should calculate for fully connected graph")
        void shouldCalculate_forFullyConnectedGraph() {
            // Given: A↔B↔C (all connected)
            // Every type reaches all 3 → 9/9 = 100%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("A", "C"),
                            "C", Set.of("B")));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(100.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should calculate for star topology (hub and spokes)")
        void shouldCalculate_forStarTopology() {
            // Given: Hub→{A, B, C}, no other edges
            // Hub reaches {Hub, A, B, C} = 4
            // A reaches {A} = 1
            // B reaches {B} = 1
            // C reaches {C} = 1
            // Total = 7 / 16 = 43.75%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "Hub", Set.of("A", "B", "C"),
                            "A", Set.of(),
                            "B", Set.of(),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(43.75, Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold for low propagation cost")
        void shouldNotExceedThreshold_forLowPropagationCost() {
            // Star topology: 43.75% > 35% → exceeds
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "Hub", Set.of("A", "B", "C"),
                            "A", Set.of(),
                            "B", Set.of(),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.exceedsThreshold()).isTrue();
        }

        @Test
        @DisplayName("Should not exceed threshold for isolated types")
        void shouldNotExceedThreshold_forIsolatedTypes() {
            // 3 isolated types: 33.3% < 35% → does not exceed
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of(),
                            "B", Set.of(),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.exceedsThreshold()).isFalse();
        }
    }
}
