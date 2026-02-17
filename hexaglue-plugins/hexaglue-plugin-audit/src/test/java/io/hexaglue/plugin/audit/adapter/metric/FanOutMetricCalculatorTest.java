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
 * Tests for {@link FanOutMetricCalculator}.
 *
 * @since 5.1.0
 */
class FanOutMetricCalculatorTest {

    private FanOutMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FanOutMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.fan.out.max");
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
    @DisplayName("When calculating fan-out")
    class WhenCalculatingFanOut {

        @Test
        @DisplayName("Should return zero for isolated types")
        void shouldReturnZero_forIsolatedTypes() {
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
        @DisplayName("Should detect hub type with high fan-out")
        void shouldDetectHub_withHighFanOut() {
            // Hub depends on A, B, C, D, E → fan-out = 5
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.Hub", Set.of("A", "B", "C", "D", "E"),
                            "A", Set.of(),
                            "B", Set.of(),
                            "C", Set.of(),
                            "D", Set.of(),
                            "E", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(5.0);
            assertThat(metric.description()).contains("Hub");
        }

        @Test
        @DisplayName("Should return one for linear chain")
        void shouldReturnOne_forLinearChain() {
            // A→B→C: max fan-out = 1
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of("C"),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should also report fan-in in description")
        void shouldReportFanIn_inDescription() {
            // A→C, B→C: max fan-in for C = 2
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("C"),
                            "B", Set.of("C"),
                            "C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.description()).contains("fan-in: 2");
        }
    }

    @Nested
    @DisplayName("When checking threshold behavior")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold for low fan-out")
        void shouldNotExceedThreshold_forLowFanOut() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "A", Set.of("B"),
                            "B", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold for high fan-out")
        void shouldExceedThreshold_forHighFanOut() {
            // Type with 25 dependencies → exceeds threshold of 20
            Set<String> manyDeps = Set.of(
                    "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "D11", "D12", "D13", "D14", "D15",
                    "D16", "D17", "D18", "D19", "D20", "D21", "D22", "D23", "D24", "D25");

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies()).thenReturn(Map.of("GodClass", manyDeps));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(25.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
