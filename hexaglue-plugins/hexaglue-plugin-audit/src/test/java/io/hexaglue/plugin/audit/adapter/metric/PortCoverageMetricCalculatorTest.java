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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortCoverageMetricCalculator}.
 *
 * <p>Validates that port coverage percentage is correctly calculated
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class PortCoverageMetricCalculatorTest {

    private PortCoverageMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PortCoverageMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("aggregate.repository.coverage");
    }

    @Test
    @DisplayName("Should return 100% when no aggregates")
    void shouldReturn100Percent_whenNoAggregates() {
        // Given: Model with domain service but no aggregates
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainService("com.example.domain.SomeService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Return 100% when no aggregates (vacuous truth)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no aggregates found");
    }

    @Test
    @DisplayName("Should return 100% when all aggregates have repositories")
    void shouldReturn100Percent_whenAllAggregatesHaveRepositories() {
        // Given: 3 aggregates, each with repository
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addAggregateRoot("com.example.domain.Product")
                .addDrivenPort(
                        "com.example.domain.port.OrderRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.CustomerRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Customer")
                .addDrivenPort(
                        "com.example.domain.port.ProductRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Product")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return 50% when half aggregates have repositories")
    void shouldReturn50Percent_whenHalfAggregatesHaveRepositories() {
        // Given: 2 aggregates, only 1 with repository
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addDrivenPort(
                        "com.example.domain.port.OrderRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 1/2 = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // < 100%
    }

    @Test
    @DisplayName("Should return 0% when no repositories")
    void shouldReturn0Percent_whenNoRepositories() {
        // Given: Aggregates but no repositories
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should match repository by managed aggregate")
    void shouldMatchRepositoryByManagedAggregate() {
        // Given: Repository explicitly managing the aggregate
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.OrderRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should not match when repository manages different aggregate")
    void shouldNotMatch_whenRepositoryManagesDifferentAggregate() {
        // Given: Repository managing a different aggregate
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.CustomerRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: No match - Order aggregate doesn't have a repository
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate correctly with multiple aggregates and repositories")
    void shouldCalculateCorrectly_withMultipleAggregatesAndRepositories() {
        // Given: 4 aggregates, 3 with repositories
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Aggregate1")
                .addAggregateRoot("com.example.domain.Aggregate2")
                .addAggregateRoot("com.example.domain.Aggregate3")
                .addAggregateRoot("com.example.domain.Aggregate4")
                .addDrivenPort(
                        "com.example.domain.port.Aggregate1Repository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Aggregate1")
                .addDrivenPort(
                        "com.example.domain.port.Aggregate2Repository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Aggregate2")
                .addDrivenPort(
                        "com.example.domain.port.Aggregate3Repository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Aggregate3")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 3/4 = 75%
        assertThat(metric.value()).isEqualTo(75.0);
        assertThat(metric.exceedsThreshold()).isTrue(); // < 100%
    }

    @Test
    @DisplayName("Should warn when only one aggregate missing repository")
    void shouldWarn_whenOnlyOneAggregateMissingRepository() {
        // Given: 5 aggregates, 4 with repositories (80%)
        TestModelBuilder builder = new TestModelBuilder();

        for (int i = 1; i <= 5; i++) {
            builder.addAggregateRoot("com.example.domain.Aggregate" + i);
        }

        // Add repositories for first 4 only
        for (int i = 1; i <= 4; i++) {
            builder.addDrivenPort(
                    "com.example.domain.port.Aggregate" + i + "Repository",
                    DrivenPortType.REPOSITORY,
                    "com.example.domain.Aggregate" + i);
        }

        ArchitecturalModel model = builder.build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 4/5 = 80%
        assertThat(metric.value()).isEqualTo(80.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should not exceed threshold at boundary")
    void shouldNotExceedThreshold_atBoundary() {
        // Given: All aggregates have repositories (100%)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.OrderRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Exactly 100%, no warning
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should exceed threshold just below boundary")
    void shouldExceedThreshold_justBelowBoundary() {
        // Given: 99% coverage (99 of 100 aggregates have repositories)
        TestModelBuilder builder = new TestModelBuilder();

        for (int i = 1; i <= 100; i++) {
            builder.addAggregateRoot("com.example.domain.Aggregate" + i);
        }

        // Add repositories for first 99 only
        for (int i = 1; i <= 99; i++) {
            builder.addDrivenPort(
                    "com.example.domain.port.Aggregate" + i + "Repository",
                    DrivenPortType.REPOSITORY,
                    "com.example.domain.Aggregate" + i);
        }

        ArchitecturalModel model = builder.build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 99/100 = 99%, still warns
        assertThat(metric.value()).isEqualTo(99.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should ignore extra repositories")
    void shouldIgnoreExtraRepositories() {
        // Given: More repositories than aggregates (orphaned repositories)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.OrderRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.CustomerRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 1/1 = 100%, extra repository ignored
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should handle single aggregate with repository")
    void shouldHandleSingleAggregate_withRepository() {
        // Given: Single aggregate with repository
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDrivenPort(
                        "com.example.domain.port.OrderRepository",
                        DrivenPortType.REPOSITORY,
                        "com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should handle single aggregate without repository")
    void shouldHandleSingleAggregate_withoutRepository() {
        // Given: Single aggregate without repository
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }
}
