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
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainPurityMetricCalculator}.
 *
 * <p>Validates that domain purity is correctly calculated using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class DomainPurityMetricCalculatorTest {

    private DomainPurityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DomainPurityMetricCalculator();
    }

    @Test
    @DisplayName("Should have correct metric name")
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("domain.purity");
    }

    @Test
    @DisplayName("Should return 100% when no domain types")
    void shouldReturn100Percent_whenNoDomainTypes() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.name()).isEqualTo("domain.purity");
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.unit()).isEqualTo("%");
    }

    @Test
    @DisplayName("Should return 100% when all domain types are pure")
    void shouldReturn100Percent_whenAllDomainTypesArePure() {
        // Given: Domain types with no infrastructure dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.OrderLine")
                .addValueObject("com.example.domain.Money")
                .addDomainService("com.example.domain.OrderService")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                // Only domain dependencies (pure)
                .addDependency("com.example.domain.Order", "com.example.domain.Money")
                .addDependency("com.example.domain.OrderLine", "com.example.domain.Money")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should return 0% when all domain types have infrastructure dependencies")
    void shouldReturn0Percent_whenAllDomainTypesHaveInfrastructureDependencies() {
        // Given: Domain types all with JPA dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.OrderLine")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "jakarta.persistence.Entity")
                .addDependency("com.example.domain.OrderLine", "javax.persistence.Column")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should calculate mixed purity correctly")
    void shouldCalculateMixedPurityCorrectly() {
        // Given: 3 domain types, 2 pure, 1 impure
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.OrderLine")
                .addValueObject("com.example.domain.Money")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                // Order has JPA dependency (impure)
                .addDependency("com.example.domain.Order", "jakarta.persistence.Entity")
                // OrderLine and Money are pure (domain only)
                .addDependency("com.example.domain.OrderLine", "com.example.domain.Money")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 2/3 = 66.67%
        assertThat(metric.value()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should detect JPA dependencies as impure")
    void shouldDetectJpaDependencies_asImpure() {
        // Given: Domain type with JPA dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "jakarta.persistence.Entity")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect Spring dependencies as impure")
    void shouldDetectSpringDependencies_asImpure() {
        // Given: Domain type with Spring dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "org.springframework.stereotype.Component")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect Hibernate dependencies as impure")
    void shouldDetectHibernateDependencies_asImpure() {
        // Given: Domain type with Hibernate dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "org.hibernate.annotations.Type")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect Jackson dependencies as impure")
    void shouldDetectJacksonDependencies_asImpure() {
        // Given: Domain type with Jackson dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.fasterxml.jackson.annotation.JsonProperty")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect AWS SDK dependencies as impure")
    void shouldDetectAwsSdkDependencies_asImpure() {
        // Given: Domain type with AWS SDK dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainService("com.example.domain.StorageService")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.StorageService", "software.amazon.awssdk.services.s3.S3Client")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect Kafka dependencies as impure")
    void shouldDetectKafkaDependencies_asImpure() {
        // Given: Domain type with Kafka dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainService("com.example.domain.EventPublisher")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.EventPublisher", "org.apache.kafka.clients.producer.Producer")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect JDBC dependencies as impure")
    void shouldDetectJdbcDependencies_asImpure() {
        // Given: Domain type with JDBC dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addDomainService("com.example.domain.DataService")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.DataService", "java.sql.Connection")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should detect Jakarta Validation dependencies as impure")
    void shouldDetectJakartaValidationDependencies_asImpure() {
        // Given: Domain type with Jakarta Validation dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Email")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Email", "jakarta.validation.constraints.NotNull")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should allow Java standard library dependencies")
    void shouldAllowJavaStandardLibraryDependencies() {
        // Given: Domain type with standard library dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "java.util.List")
                .addDependency("com.example.domain.Order", "java.time.LocalDateTime")
                .addDependency("com.example.domain.Order", "java.math.BigDecimal")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Standard library is allowed (pure)
        assertThat(metric.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should count all domain type categories")
    void shouldCountAllDomainTypeCategories() {
        // Given: All domain type categories
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.OrderLine")
                .addValueObject("com.example.domain.Money")
                .addIdentifier("com.example.domain.OrderId")
                .addDomainEvent("com.example.domain.OrderPlacedEvent")
                .addDomainService("com.example.domain.OrderService")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                // Only one impure type
                .addDependency("com.example.domain.Order", "jakarta.persistence.Entity")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 5/6 = 83.33%
        assertThat(metric.value()).isCloseTo(83.33, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should handle domain type with multiple infrastructure dependencies")
    void shouldHandleDomainTypeWithMultipleInfrastructureDependencies() {
        // Given: Domain type with multiple infrastructure dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "jakarta.persistence.Entity")
                .addDependency("com.example.domain.Order", "org.springframework.stereotype.Component")
                .addDependency("com.example.domain.Order", "com.fasterxml.jackson.annotation.JsonProperty")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: Still impure (one violation is enough)
        assertThat(metric.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should not exceed threshold at 100%")
    void shouldNotExceedThreshold_at100Percent() {
        // Given: All pure domain types
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addValueObject("com.example.domain.Money")
                .build();

        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    @DisplayName("Should exceed threshold below 100%")
    void shouldExceedThreshold_below100Percent() {
        // Given: One impure domain type out of two
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addValueObject("com.example.domain.Money")
                .build();

        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "jakarta.persistence.Entity")
                .build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then: 50% < 100% threshold
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty codebase")
    void shouldHandleEmptyCodebase() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        Metric metric = calculator.calculate(model, codebase, null);

        // Then
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }
}
