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
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortCoverageMetricCalculator}.
 */
class PortCoverageMetricCalculatorTest {

    private PortCoverageMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PortCoverageMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("port.coverage");
    }

    @Test
    void shouldReturn100Percent_whenNoAggregates() {
        // Given: Codebase with no aggregates
        Codebase codebase = withUnits(domainClass("SomeService"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Return 100% when no aggregates (vacuous truth)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no aggregates found");
    }

    @Test
    void shouldReturn100Percent_whenAllAggregatesHaveRepositories() {
        // Given: 3 aggregates, each with repository
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");
        CodeUnit product = aggregate("Product");
        CodeUnit orderRepo = repository("Order");
        CodeUnit customerRepo = repository("Customer");
        CodeUnit productRepo = repository("Product");

        Codebase codebase = withUnits(order, customer, product, orderRepo, customerRepo, productRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturn50Percent_whenHalfAggregatesHaveRepositories() {
        // Given: 2 aggregates, only 1 with repository
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");
        CodeUnit orderRepo = repository("Order");

        Codebase codebase = withUnits(order, customer, orderRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1/2 = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // < 100%
    }

    @Test
    void shouldReturn0Percent_whenNoRepositories() {
        // Given: Aggregates but no repositories
        CodeUnit order = aggregate("Order");
        CodeUnit customer = aggregate("Customer");

        Codebase codebase = withUnits(order, customer);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldMatchRepositoryByName() {
        // Given: Repository name matches aggregate name + "Repository"
        CodeUnit order = aggregate("Order");
        CodeUnit orderRepo = repository("Order"); // Creates "OrderRepository"

        Codebase codebase = withUnits(order, orderRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    void shouldNotMatch_whenRepositoryNameIncorrect() {
        // Given: Repository with wrong name
        CodeUnit order = aggregate("Order");
        CodeUnit wrongRepo = repository("Customer"); // "CustomerRepository", not "OrderRepository"

        Codebase codebase = withUnits(order, wrongRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: No match
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    void shouldCalculateCorrectly_withMultipleAggregatesAndRepositories() {
        // Given: 4 aggregates, 3 with repositories
        CodeUnit agg1 = aggregate("Aggregate1");
        CodeUnit agg2 = aggregate("Aggregate2");
        CodeUnit agg3 = aggregate("Aggregate3");
        CodeUnit agg4 = aggregate("Aggregate4");

        CodeUnit repo1 = repository("Aggregate1");
        CodeUnit repo2 = repository("Aggregate2");
        CodeUnit repo3 = repository("Aggregate3");

        Codebase codebase = withUnits(agg1, agg2, agg3, agg4, repo1, repo2, repo3);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 3/4 = 75%
        assertThat(metric.value()).isEqualTo(75.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // < 100%
    }

    @Test
    void shouldWarn_whenOnlyOneAggregateMissingRepository() {
        // Given: 5 aggregates, 4 with repositories (80%)
        var builder = new io.hexaglue.plugin.audit.util.TestCodebaseBuilder();

        for (int i = 1; i <= 5; i++) {
            builder.addUnit(aggregate("Aggregate" + i));
        }

        // Add repositories for first 4 only
        for (int i = 1; i <= 4; i++) {
            builder.addUnit(repository("Aggregate" + i));
        }

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 4/5 = 80%
        assertThat(metric.value()).isEqualTo(80.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldNotExceedThreshold_atBoundary() {
        // Given: All aggregates have repositories (100%)
        CodeUnit order = aggregate("Order");
        CodeUnit orderRepo = repository("Order");

        Codebase codebase = withUnits(order, orderRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Exactly 100%, no warning
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldExceedThreshold_justBelowBoundary() {
        // Given: 99% coverage (99 of 100 aggregates have repositories)
        var builder = new io.hexaglue.plugin.audit.util.TestCodebaseBuilder();

        for (int i = 1; i <= 100; i++) {
            builder.addUnit(aggregate("Aggregate" + i));
        }

        // Add repositories for first 99 only
        for (int i = 1; i <= 99; i++) {
            builder.addUnit(repository("Aggregate" + i));
        }

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 99/100 = 99%, still warns
        assertThat(metric.value()).isEqualTo(99.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldIgnoreExtraRepositories() {
        // Given: More repositories than aggregates (orphaned repositories)
        CodeUnit order = aggregate("Order");
        CodeUnit orderRepo = repository("Order");
        CodeUnit customerRepo = repository("Customer"); // No matching aggregate

        Codebase codebase = withUnits(order, orderRepo, customerRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1/1 = 100%, extra repository ignored
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    void shouldHandleSingleAggregate_withRepository() {
        // Given: Single aggregate with repository
        CodeUnit order = aggregate("Order");
        CodeUnit orderRepo = repository("Order");

        Codebase codebase = withUnits(order, orderRepo);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    void shouldHandleSingleAggregate_withoutRepository() {
        // Given: Single aggregate without repository
        CodeUnit order = aggregate("Order");

        Codebase codebase = withUnits(order);

        // When
        Metric metric = calculator.calculate(codebase);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }
}
