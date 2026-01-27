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
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.LayerClassification;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainCoverageMetricCalculator}.
 *
 * <p>Validates that domain coverage percentage is correctly calculated
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class DomainCoverageMetricCalculatorTest {

    private DomainCoverageMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DomainCoverageMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("domain.coverage");
    }

    @Test
    @DisplayName("Should return zero when no types")
    void shouldReturnZero_whenNoTypes() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no types found");
    }

    @Test
    @DisplayName("Should return 100% when all types are domain types")
    void shouldReturn100Percent_whenAllTypesDomain() {
        // Given: Only domain types
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.OrderLine")
                .addValueObject("com.example.domain.Money")
                .addDomainService("com.example.domain.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Order")
                .addType("com.example.domain.OrderLine")
                .addType("com.example.domain.Money")
                .addType("com.example.domain.OrderService")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should calculate coverage with mixed layers")
    void shouldCalculateCoverage_withMixedLayers() {
        // Given: 6 domain types, 4 other types (60% domain)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addAggregateRoot("com.example.domain.Customer")
                .addEntity("com.example.domain.OrderLine")
                .addValueObject("com.example.domain.Money")
                .addDomainService("com.example.domain.Service1")
                .addDomainService("com.example.domain.Service2")
                .addUnclassifiedType("com.example.infrastructure.OrderAdapter", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.CustomerAdapter", UnclassifiedCategory.TECHNICAL)
                .addApplicationService("com.example.application.OrderUseCase")
                .addApplicationService("com.example.application.CustomerUseCase")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Order")
                .addType("com.example.domain.Customer")
                .addType("com.example.domain.OrderLine")
                .addType("com.example.domain.Money")
                .addType("com.example.domain.Service1")
                .addType("com.example.domain.Service2")
                .addType("com.example.infrastructure.OrderAdapter", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.CustomerAdapter", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.application.OrderUseCase", LayerClassification.APPLICATION)
                .addType("com.example.application.CustomerUseCase", LayerClassification.APPLICATION)
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 6 domain / 10 total = 60%
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should warn when domain coverage below threshold")
    void shouldWarn_whenDomainCoverageBelowThreshold() {
        // Given: 2 domain types, 10 other types (16.67% domain)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDomainService("com.example.domain.Service")
                .addUnclassifiedType("com.example.infrastructure.Adapter1", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Adapter2", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Adapter3", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Adapter4", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Adapter5", UnclassifiedCategory.TECHNICAL)
                .addApplicationService("com.example.application.UseCase1")
                .addApplicationService("com.example.application.UseCase2")
                .addApplicationService("com.example.application.UseCase3")
                .addApplicationService("com.example.application.UseCase4")
                .addApplicationService("com.example.application.UseCase5")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Order")
                .addType("com.example.domain.Service")
                .addType("com.example.infrastructure.Adapter1", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Adapter2", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Adapter3", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Adapter4", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Adapter5", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.application.UseCase1", LayerClassification.APPLICATION)
                .addType("com.example.application.UseCase2", LayerClassification.APPLICATION)
                .addType("com.example.application.UseCase3", LayerClassification.APPLICATION)
                .addType("com.example.application.UseCase4", LayerClassification.APPLICATION)
                .addType("com.example.application.UseCase5", LayerClassification.APPLICATION)
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 2 / 12 = 16.67%, below 30% threshold
        assertThat(metric.value()).isCloseTo(16.67, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should calculate correctly at threshold boundary")
    void shouldCalculateCorrectly_atThresholdBoundary() {
        // Given: Exactly 30% domain coverage
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Agg1")
                .addAggregateRoot("com.example.domain.Agg2")
                .addAggregateRoot("com.example.domain.Agg3") // 3 domain
                .addUnclassifiedType("com.example.infrastructure.Infra1", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra2", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra3", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra4", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra5", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra6", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra7", UnclassifiedCategory.TECHNICAL)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Agg1")
                .addType("com.example.domain.Agg2")
                .addType("com.example.domain.Agg3")
                .addType("com.example.infrastructure.Infra1", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra2", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra3", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra4", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra5", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra6", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra7", LayerClassification.INFRASTRUCTURE)
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 3 / 10 = 30%, exactly at threshold
        assertThat(metric.value()).isEqualTo(30.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should warn just below threshold")
    void shouldWarn_justBelowThreshold() {
        // Given: ~28.6% domain coverage (just below 30%)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Agg1")
                .addAggregateRoot("com.example.domain.Agg2") // 2 domain
                .addUnclassifiedType("com.example.infrastructure.Infra1", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra2", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra3", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra4", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Infra5", UnclassifiedCategory.TECHNICAL)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Agg1")
                .addType("com.example.domain.Agg2")
                .addType("com.example.infrastructure.Infra1", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra2", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra3", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra4", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Infra5", LayerClassification.INFRASTRUCTURE)
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 2/7 = 28.57%
        assertThat(metric.value()).isCloseTo(28.57, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should count only domain layer, ignoring application layer")
    void shouldCountOnlyDomainLayer_ignoringApplicationLayer() {
        // Given: Application layer types should not count as domain
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDomainService("com.example.domain.Service") // 2 domain
                .addApplicationService("com.example.application.UseCase1")
                .addApplicationService("com.example.application.UseCase2") // 2 application
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Order")
                .addType("com.example.domain.Service")
                .addType("com.example.application.UseCase1", LayerClassification.APPLICATION)
                .addType("com.example.application.UseCase2", LayerClassification.APPLICATION)
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 2 domain / 4 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should handle zero domain types")
    void shouldHandleZeroDomainTypes() {
        // Given: No domain types at all (bad architecture!)
        ArchitecturalModel model = new TestModelBuilder()
                .addUnclassifiedType("com.example.infrastructure.Adapter1", UnclassifiedCategory.TECHNICAL)
                .addUnclassifiedType("com.example.infrastructure.Adapter2", UnclassifiedCategory.TECHNICAL)
                .addApplicationService("com.example.application.UseCase1")
                .addApplicationService("com.example.application.UseCase2")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.infrastructure.Adapter1", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.infrastructure.Adapter2", LayerClassification.INFRASTRUCTURE)
                .addType("com.example.application.UseCase1", LayerClassification.APPLICATION)
                .addType("com.example.application.UseCase2", LayerClassification.APPLICATION)
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 0%
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should count identifiers as domain types")
    void shouldCountIdentifiers_asDomainTypes() {
        // Given: Identifier + aggregate
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addIdentifier("com.example.domain.OrderId")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Order")
                .addType("com.example.domain.OrderId")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 2 domain / 2 total = 100%
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should count domain events as domain types")
    void shouldCountDomainEvents_asDomainTypes() {
        // Given: Domain event + aggregate
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addDomainEvent("com.example.domain.OrderPlacedEvent")
                .addDomainEvent("com.example.domain.OrderCancelledEvent")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addType("com.example.domain.Order")
                .addType("com.example.domain.OrderPlacedEvent")
                .addType("com.example.domain.OrderCancelledEvent")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 3 domain / 3 total = 100%
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should calculate correctly with large mixed codebase")
    void shouldCalculateCorrectly_withLargeMixedCodebase() {
        // Given: 50 domain, 150 other = 25% coverage
        TestModelBuilder modelBuilder = new TestModelBuilder();
        TestCodebaseBuilder codebaseBuilder = new TestCodebaseBuilder();

        for (int i = 0; i < 50; i++) {
            String name = "com.example.domain.Aggregate" + i;
            modelBuilder.addAggregateRoot(name);
            codebaseBuilder.addType(name);
        }
        for (int i = 0; i < 150; i++) {
            String name = "com.example.infrastructure.Infra" + i;
            modelBuilder.addUnclassifiedType(name, UnclassifiedCategory.TECHNICAL);
            codebaseBuilder.addType(name, LayerClassification.INFRASTRUCTURE);
        }

        ArchitecturalModel model = modelBuilder.build();
        Codebase codebase = codebaseBuilder.build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 50 / 200 = 25%
        assertThat(metric.value()).isEqualTo(25.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }
}
