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
import java.util.Map;
import java.util.Set;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RelationalCohesionMetricCalculator}.
 *
 * <p>Validates the Dowalil relational cohesion formula:
 * H(pkg) = (internal_deps + 1) / type_count, then averaged across packages.
 *
 * @since 5.1.0
 */
class RelationalCohesionMetricCalculatorTest {

    private RelationalCohesionMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RelationalCohesionMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.cohesion.relational");
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
    @DisplayName("When calculating cohesion")
    class WhenCalculatingCohesion {

        @Test
        @DisplayName("Should calculate H for single package with 3 types and 2 internal deps")
        void shouldCalculate_singlePackage_3types_2internalDeps() {
            // Package com.example: A→B, B→C (2 internal deps)
            // H = (2 + 1) / 3 = 1.0
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.A", Set.of("com.example.B"),
                            "com.example.B", Set.of("com.example.C"),
                            "com.example.C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(1.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should calculate H for single package with 3 types and 5 internal deps")
        void shouldCalculate_singlePackage_3types_5internalDeps() {
            // Package com.example: dense connections
            // A→B, A→C, B→A, B→C, C→A (5 internal deps)
            // H = (5 + 1) / 3 = 2.0
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.A", Set.of("com.example.B", "com.example.C"),
                            "com.example.B", Set.of("com.example.A", "com.example.C"),
                            "com.example.C", Set.of("com.example.A")));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(2.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should average H across multiple packages")
        void shouldAverage_multiplePackages() {
            // Package com.domain: A→B, B→C (2 internal deps, 3 types) → H = (2+1)/3 = 1.0
            // Package com.infra: X→Y (1 internal dep, 2 types) → H = (1+1)/2 = 1.0
            // Average H = (1.0 + 1.0) / 2 = 1.0
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("com.domain.A", Set.of("com.domain.B"));
            deps.put("com.domain.B", Set.of("com.domain.C"));
            deps.put("com.domain.C", Set.of());
            deps.put("com.infra.X", Set.of("com.infra.Y"));
            deps.put("com.infra.Y", Set.of());
            when(query.allTypeDependencies()).thenReturn(deps);

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(1.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should exclude cross-package dependencies from internal count")
        void shouldExclude_crossPackageDeps() {
            // A→B (internal), A→X (cross-package)
            // Package com.domain: 2 types, 1 internal dep → H = (1+1)/2 = 1.0
            // Package com.infra: 1 type → excluded (< 2 types)
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("com.domain.A", Set.of("com.domain.B", "com.infra.X"));
            deps.put("com.domain.B", Set.of());
            deps.put("com.infra.X", Set.of());
            when(query.allTypeDependencies()).thenReturn(deps);

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(1.0, Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should exclude single-type packages from average")
        void shouldExclude_singleTypePackages() {
            // 3 packages with 1 type each → no packages with >= 2 types
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.a.X", Set.of("com.b.Y"),
                            "com.b.Y", Set.of("com.c.Z"),
                            "com.c.Z", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("When checking threshold")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold when H is within range")
        void shouldNotExceedThreshold_whenWithinRange() {
            // H = 2.0 is within [1.5, 4.0]
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.A", Set.of("com.example.B", "com.example.C"),
                            "com.example.B", Set.of("com.example.A", "com.example.C"),
                            "com.example.C", Set.of("com.example.A")));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(2.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold when H is below range")
        void shouldExceedThreshold_whenBelowRange() {
            // H = 1.0 is below 1.5
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.allTypeDependencies())
                    .thenReturn(Map.of(
                            "com.example.A", Set.of("com.example.B"),
                            "com.example.B", Set.of("com.example.C"),
                            "com.example.C", Set.of()));

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isCloseTo(1.0, Offset.offset(0.01));
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
