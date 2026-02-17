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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DependencyDepthMetricCalculator}.
 *
 * @since 5.1.0
 */
class DependencyDepthMetricCalculatorTest {

    private DependencyDepthMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DependencyDepthMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.dependency.depth");
    }

    @Nested
    @DisplayName("When query not available")
    class WhenQueryNotAvailable {

        @Test
        @DisplayName("Should return zero when query is null")
        void shouldReturnZero_whenQueryIsNull() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.unit()).isEqualTo("levels");
        }

        @Test
        @DisplayName("Should return zero when no dependencies")
        void shouldReturnZero_whenNoDependencies() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(Map.of());

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When calculating depth on DAG")
    class WhenCalculatingDepth {

        @Test
        @DisplayName("Should return 0 for isolated types")
        void shouldReturn0_forIsolatedTypes() {
            // Given: 3 types with no dependencies
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of(),
                            "B", Set.of(),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 1 for single edge A→B")
        void shouldReturn1_forSingleEdge() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 2 for linear chain A→B→C")
        void shouldReturn2_forLinearChain() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("C"),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should return 4 for hexagonal-style layering")
        void shouldReturn4_forHexagonalStyleLayering() {
            // adapter → appService → domainService → entity → valueObject
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "Adapter", Set.of("AppService"),
                            "AppService", Set.of("DomainService"),
                            "DomainService", Set.of("Entity"),
                            "Entity", Set.of("ValueObject"),
                            "ValueObject", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(4.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should pick longest path in tree structure")
        void shouldPickLongestPath_inTreeStructure() {
            // A→{B, C}, B→D, C→{E, F}, E→G
            // Longest: A→C→E→G = 3
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B", "C"),
                            "B", Set.of("D"),
                            "C", Set.of("E", "F"),
                            "D", Set.of(),
                            "E", Set.of("G"),
                            "F", Set.of(),
                            "G", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("When cycles exist")
    class WhenCyclesExist {

        @Test
        @DisplayName("Should handle cycle by contracting SCC")
        void shouldHandleCycle_byContractingSCC() {
            // A→B→A (cycle), A→C
            // After SCC contraction: {A,B}→C, depth = 1
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B", "C"),
                            "B", Set.of("A"),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle fully connected cycle")
        void shouldHandleFullyConnectedCycle() {
            // A↔B↔C (all cycle) → single SCC, depth = 0
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("C"),
                            "C", Set.of("A")));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            // All contracted into one SCC node → depth 0
            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold at depth 7")
        void shouldNotExceedThreshold_atDepth7() {
            // 7 levels: A→B→C→D→E→F→G→H
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("C"),
                            "C", Set.of("D"),
                            "D", Set.of("E"),
                            "E", Set.of("F"),
                            "F", Set.of("G"),
                            "G", Set.of("H"),
                            "H", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(7.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold above depth 7")
        void shouldExceedThreshold_aboveDepth7() {
            // 8 levels
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("C"),
                            "C", Set.of("D"),
                            "D", Set.of("E"),
                            "E", Set.of("F"),
                            "F", Set.of("G"),
                            "G", Set.of("H"),
                            "H", Set.of("I")));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(8.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
