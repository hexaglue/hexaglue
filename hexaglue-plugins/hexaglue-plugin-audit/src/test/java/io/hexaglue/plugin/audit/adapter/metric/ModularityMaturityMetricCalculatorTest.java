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
import io.hexaglue.arch.model.audit.CouplingMetrics;
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
 * Tests for {@link ModularityMaturityMetricCalculator}.
 *
 * <p>Validates the composite MMI formula:
 * MMI = 0.45 × modularity + 0.30 × hierarchy + 0.25 × pattern
 *
 * @since 5.1.0
 */
class ModularityMaturityMetricCalculatorTest {

    private ModularityMaturityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ModularityMaturityMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("architecture.mmi");
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
    }

    @Nested
    @DisplayName("When calculating MMI")
    class WhenCalculatingMMI {

        @Test
        @DisplayName("Should return high MMI when no cycles and no problematic packages")
        void shouldReturnHighMMI_whenNoCyclesAndNoProblematicPackages() {
            // No cycles, no problematic packages, ideal distance
            // modularity = 100 (D=0 for all), hierarchy = 100 (no cycles), pattern = 100 (no problematic)
            // MMI = 0.45*100 + 0.30*100 + 0.25*100 = 100
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            // Package on main sequence: D = |A + I - 1| = 0 when A=0.5, I=0.5
            CouplingMetrics idealPkg = new CouplingMetrics("com.example.domain", 5, 5, 0.5);
            when(query.analyzeAllPackageCoupling()).thenReturn(List.of(idealPkg));

            // No dependencies → no cycles
            when(query.allTypeDependencies()).thenReturn(Map.of());

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            // Modularity: (1 - 0.0) * 100 = 100
            // Hierarchy: 100 (no deps)
            // Pattern: 100% non-problematic
            assertThat(metric.value()).isCloseTo(100.0, Offset.offset(0.1));
        }

        @Test
        @DisplayName("Should return lower MMI when cycles and problematic packages exist")
        void shouldReturnLowerMMI_whenCyclesAndProblematicPackagesExist() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            // Package far from main sequence: D = |0.0 + 0.0 - 1| = 1.0
            // This is in Zone of Pain (I<0.3, A<0.3) → problematic
            CouplingMetrics painPkg = new CouplingMetrics("com.example.domain", 10, 0, 0.0);
            when(query.analyzeAllPackageCoupling()).thenReturn(List.of(painPkg));

            // Create cycles: A↔B among 10 types → RC = 4/100*100 = 4%
            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("A", Set.of("B"));
            deps.put("B", Set.of("A"));
            for (int i = 0; i < 8; i++) {
                deps.put("T" + i, Set.of());
            }
            when(query.allTypeDependencies()).thenReturn(deps);

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            // Modularity: (1 - 1.0) * 100 = 0 → axis = 0
            // Hierarchy: max(0, 100 - 4*5) = 80
            // Pattern: 0/1 non-problematic → 0
            // MMI = 0.45*0 + 0.30*80 + 0.25*0 = 24
            assertThat(metric.value()).isCloseTo(24.0, Offset.offset(1.0));
            assertThat(metric.value()).isLessThan(50.0);
        }

        @Test
        @DisplayName("Should handle mixed packages")
        void shouldHandle_mixedPackages() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);

            // One ideal package and one problematic
            CouplingMetrics idealPkg = new CouplingMetrics("com.example.domain", 5, 5, 0.5);
            CouplingMetrics painPkg = new CouplingMetrics("com.example.infra", 10, 0, 0.0);
            when(query.analyzeAllPackageCoupling()).thenReturn(List.of(idealPkg, painPkg));

            // No cycles
            when(query.allTypeDependencies()).thenReturn(Map.of());

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            // Modularity: ((1-0) + (1-1))/2 * 100 = 50
            // Hierarchy: 100 (no deps)
            // Pattern: 1/2 non-problematic = 50%
            // MMI = 0.45*50 + 0.30*100 + 0.25*50 = 22.5 + 30 + 12.5 = 65
            assertThat(metric.value()).isCloseTo(65.0, Offset.offset(1.0));
        }
    }

    @Nested
    @DisplayName("When checking threshold")
    class WhenCheckingThreshold {

        @Test
        @DisplayName("Should not exceed threshold when MMI is above 50")
        void shouldNotExceedThreshold_whenAbove50() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            CouplingMetrics idealPkg = new CouplingMetrics("com.example.domain", 5, 5, 0.5);
            when(query.analyzeAllPackageCoupling()).thenReturn(List.of(idealPkg));
            when(query.allTypeDependencies()).thenReturn(Map.of());

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            assertThat(metric.value()).isGreaterThan(50.0);
            assertThat(metric.exceedsThreshold()).isFalse();
        }

        @Test
        @DisplayName("Should exceed threshold when MMI is below 50")
        void shouldExceedThreshold_whenBelow50() {
            ArchitectureQuery query = mock(ArchitectureQuery.class);
            CouplingMetrics painPkg = new CouplingMetrics("com.example.domain", 10, 0, 0.0);
            when(query.analyzeAllPackageCoupling()).thenReturn(List.of(painPkg));

            // Large cycle to drive hierarchy down
            Map<String, Set<String>> deps = new HashMap<>();
            deps.put("A", Set.of("B"));
            deps.put("B", Set.of("C"));
            deps.put("C", Set.of("D"));
            deps.put("D", Set.of("A"));
            for (int i = 0; i < 6; i++) {
                deps.put("T" + i, Set.of());
            }
            when(query.allTypeDependencies()).thenReturn(deps);

            ArchitecturalModel model = TestModelBuilder.emptyModel();
            Codebase codebase = mock(Codebase.class);

            Metric metric = calculator.calculate(model, codebase, query);

            // Modularity: (1-1)*100 = 0
            // Hierarchy: max(0, 100 - 16*5) = max(0, 20) = 20
            // Pattern: 0% non-problematic
            // MMI = 0.45*0 + 0.30*20 + 0.25*0 = 6
            assertThat(metric.value()).isLessThan(50.0);
            assertThat(metric.exceedsThreshold()).isTrue();
        }
    }
}
