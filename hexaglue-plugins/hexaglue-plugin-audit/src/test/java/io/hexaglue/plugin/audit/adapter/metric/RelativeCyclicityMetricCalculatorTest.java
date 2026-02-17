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
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RelativeCyclicityMetricCalculator}.
 *
 * <p>Validates the Sonargraph-style relative cyclicity formula:
 * {@code Σ(scc_size²) / N² × 100} using true Strongly Connected Components
 * from Tarjan's algorithm.
 *
 * @since 5.1.0
 */
class RelativeCyclicityMetricCalculatorTest {

    private RelativeCyclicityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RelativeCyclicityMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.cyclicity.relative");
    }

    @Nested
    @DisplayName("When no cycles exist")
    class WhenNoCycles {

        @Test
        @DisplayName("Should return zero when query is null")
        void shouldReturnZero_whenQueryIsNull() {
            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, null);

            assertThat(metric.value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return zero when no dependencies")
        void shouldReturnZero_whenNoDependencies() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(Map.of());

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return zero when no cycles detected")
        void shouldReturnZero_whenNoCyclesDetected() {
            // 10-node DAG: T0→T1→...→T9
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(linearGraph(10));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }
    }

    @Nested
    @DisplayName("When cycles exist")
    class WhenCyclesExist {

        @Test
        @DisplayName("Should calculate for single small cycle (2 types)")
        void shouldCalculate_forSingleSmallCycle() {
            // SCC {A,B} among 10 types: 2² / 10² × 100 = 4%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(graphWithCycle(10, "A", "B"));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(4.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should calculate for single large cycle (5 types)")
        void shouldCalculate_forSingleLargeCycle() {
            // SCC {A,B,C,D,E} among 10 types: 5² / 10² × 100 = 25%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(graphWithCycle(10, "A", "B", "C", "D", "E"));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(25.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isTrue();
        }

        @Test
        @DisplayName("Should calculate for two small cycles")
        void shouldCalculate_forTwoSmallCycles() {
            // SCC {A,B} + SCC {C,D} among 10 types: (2² + 2²) / 10² × 100 = 8%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            Map<String, Set<String>> graph = new HashMap<>();
            // Cycle 1: A ↔ B
            graph.put("A", Set.of("B"));
            graph.put("B", Set.of("A"));
            // Cycle 2: C ↔ D
            graph.put("C", Set.of("D"));
            graph.put("D", Set.of("C"));
            // Fill to 10 types
            for (int i = 0; i < 6; i++) {
                graph.put("T" + i, Set.of());
            }

            when(query.allTypeDependencies()).thenReturn(graph);

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(8.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isTrue();
        }

        @Test
        @DisplayName("Should merge overlapping cycles into single SCC")
        void shouldMergeOverlappingCycles_intoSingleScc() {
            // A→B→C→A and B→D→B share B → single SCC {A,B,C,D}
            // 4² / 10² × 100 = 16%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            Map<String, Set<String>> graph = new HashMap<>();
            graph.put("A", Set.of("B"));
            graph.put("B", Set.of("C", "D"));
            graph.put("C", Set.of("A"));
            graph.put("D", Set.of("B"));
            // Fill to 10 types
            for (int i = 0; i < 6; i++) {
                graph.put("T" + i, Set.of());
            }

            when(query.allTypeDependencies()).thenReturn(graph);

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(16.0, Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold at 4% (below 5%)")
        void shouldNotExceedThreshold_below5Percent() {
            // SCC of 2 among 10: 4/100 = 4%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(graphWithCycle(10, "A", "B"));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(4.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold above 5%")
        void shouldExceedThreshold_above5Percent() {
            // SCC of 3 among 10: 9/100 = 9%
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(graphWithCycle(10, "A", "B", "C"));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = new TestCodebaseBuilder().build();

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(9.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }

    // === Graph Builders ===

    /**
     * Creates a linear DAG T0→T1→...→T(n-1) with no cycles.
     */
    private Map<String, Set<String>> linearGraph(int size) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (int i = 0; i < size - 1; i++) {
            graph.put("T" + i, Set.of("T" + (i + 1)));
        }
        graph.put("T" + (size - 1), Set.of());
        return graph;
    }

    /**
     * Creates a graph of {@code totalSize} types where the given cycle members
     * form a directed cycle (e.g., A→B→C→A). Remaining types are isolated.
     */
    private Map<String, Set<String>> graphWithCycle(int totalSize, String... cycleMembers) {
        Map<String, Set<String>> graph = new HashMap<>();
        // Build directed cycle: each member points to the next, last points to first
        for (int i = 0; i < cycleMembers.length; i++) {
            String next = cycleMembers[(i + 1) % cycleMembers.length];
            graph.put(cycleMembers[i], Set.of(next));
        }
        // Fill remaining types (isolated)
        int remaining = totalSize - cycleMembers.length;
        for (int i = 0; i < remaining; i++) {
            graph.put("T" + i, Set.of());
        }
        return graph;
    }
}
