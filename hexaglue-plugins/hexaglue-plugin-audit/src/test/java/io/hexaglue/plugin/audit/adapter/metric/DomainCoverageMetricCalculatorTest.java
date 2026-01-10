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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainCoverageMetricCalculator}.
 */
class DomainCoverageMetricCalculatorTest {

    private DomainCoverageMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DomainCoverageMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("domain.coverage");
    }

    @Test
    void shouldReturnZero_whenNoTypes() {
        // Given: Empty codebase
        Codebase codebase = new Codebase("test", "com.example", List.of(), java.util.Map.of());

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no types found");
    }

    @Test
    void shouldReturn100Percent_whenAllTypesDomain() {
        // Given: Only domain types
        Codebase codebase = withUnits(
                aggregate("Order"),
                entity("OrderLine", true),
                valueObject("Money", false),
                domainClass("OrderService"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse(); // 100% > 30%, so no warning
    }

    @Test
    void shouldCalculateCoverage_withMixedLayers() {
        // Given: 6 domain types, 4 infrastructure types (60% domain)
        Codebase codebase = withUnits(
                aggregate("Order"),
                aggregate("Customer"),
                entity("OrderLine", true),
                valueObject("Money", false),
                domainClass("Service1"),
                domainClass("Service2"),
                infraClass("OrderAdapter"),
                infraClass("CustomerAdapter"),
                applicationClass("OrderUseCase"),
                applicationClass("CustomerUseCase"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 6 domain / 10 total = 60%
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldWarn_whenDomainCoverageBelowThreshold() {
        // Given: 2 domain types, 10 infrastructure types (16.67% domain)
        Codebase codebase = withUnits(
                aggregate("Order"),
                domainClass("Service"),
                infraClass("Adapter1"),
                infraClass("Adapter2"),
                infraClass("Adapter3"),
                infraClass("Adapter4"),
                infraClass("Adapter5"),
                applicationClass("UseCase1"),
                applicationClass("UseCase2"),
                applicationClass("UseCase3"),
                applicationClass("UseCase4"),
                applicationClass("UseCase5"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 2 / 12 = 16.67%, below 30% threshold
        assertThat(metric.value()).isCloseTo(16.67, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldCalculateCorrectly_atThresholdBoundary() {
        // Given: Exactly 30% domain coverage
        Codebase codebase = withUnits(
                aggregate("Agg1"),
                aggregate("Agg2"),
                aggregate("Agg3"), // 3 domain
                infraClass("Infra1"),
                infraClass("Infra2"),
                infraClass("Infra3"),
                infraClass("Infra4"),
                infraClass("Infra5"),
                infraClass("Infra6"),
                infraClass("Infra7") // 7 infrastructure
                );

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 3 / 10 = 30%, exactly at threshold (should not warn, as threshold is <30)
        assertThat(metric.value()).isEqualTo(30.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldWarn_justBelowThreshold() {
        // Given: 29% domain coverage (just below 30%)
        Codebase codebase = withUnits(
                aggregate("Agg1"),
                aggregate("Agg2"), // 2 domain
                infraClass("Infra1"),
                infraClass("Infra2"),
                infraClass("Infra3"),
                infraClass("Infra4"),
                infraClass("Infra5") // 5 infrastructure
                // Total: 7 types, 2/7 = 28.57%
                );

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isCloseTo(28.57, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldCountOnlyDomainLayer_ignoringApplicationLayer() {
        // Given: Application layer types should not count as domain
        Codebase codebase = withUnits(
                aggregate("Order"),
                domainClass("Service"), // 2 domain
                applicationClass("UseCase1"),
                applicationClass("UseCase2") // 2 application
                );

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 2 domain / 4 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
    }

    @Test
    void shouldHandleZeroDomainTypes() {
        // Given: No domain types at all (bad architecture!)
        Codebase codebase = withUnits(
                infraClass("Adapter1"),
                infraClass("Adapter2"),
                applicationClass("UseCase1"),
                applicationClass("UseCase2"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 0%
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldCalculateCorrectly_withLargeMixedCodebase() {
        // Given: 50 domain, 150 other = 20% coverage
        var builder = new io.hexaglue.plugin.audit.util.TestCodebaseBuilder();

        for (int i = 0; i < 50; i++) {
            builder.addUnit(domainClass("Domain" + i));
        }
        for (int i = 0; i < 150; i++) {
            builder.addUnit(infraClass("Infra" + i));
        }

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 50 / 200 = 25%
        assertThat(metric.value()).isEqualTo(25.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }
}
