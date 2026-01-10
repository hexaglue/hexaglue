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
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainPurityMetricCalculator}.
 */
class DomainPurityMetricCalculatorTest {

    private DomainPurityMetricCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DomainPurityMetricCalculator();
    }

    @Test
    void shouldHaveCorrectMetricName() {
        assertThat(calculator.metricName()).isEqualTo("domain.purity");
    }

    @Test
    void shouldReturn100Percent_whenNoDomainTypes() {
        // Given: Empty codebase with no domain types
        Codebase codebase = new Codebase("test", "com.example", List.of(), java.util.Map.of());

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Default to 100% when no domain types exist
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.unit()).isEqualTo("%");
        assertThat(metric.description()).contains("no domain types found");
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturn100Percent_whenAllDomainTypesPure() {
        // Given: Domain types with no infrastructure dependencies
        Codebase codebase = withUnits(
                aggregate("Order"),
                entity("OrderLine", true),
                valueObject("Money", false),
                domainClass("OrderService"));

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: All types are pure
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldReturn100Percent_whenDomainTypesHaveNoDependencies() {
        // Given: Domain types with empty dependencies
        var builder = new TestCodebaseBuilder();
        builder.addUnit(aggregate("Order"));
        builder.addUnit(valueObject("Money", false));
        // No dependencies added

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Types with no dependencies are pure
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldDetectImpurity_withJpaDependencies() {
        // Given: Domain type with JPA dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "jakarta.persistence.Entity");
        builder.addDependency(order.qualifiedName(), "jakarta.persistence.Id");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withSpringDependencies() {
        // Given: Domain type with Spring dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(entity("OrderLine", true));
        builder.addDependency(order.qualifiedName(), "org.springframework.stereotype.Component");
        builder.addDependency(order.qualifiedName(), "org.springframework.beans.factory.annotation.Autowired");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withHibernateDependencies() {
        // Given: Domain type with Hibernate dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "org.hibernate.annotations.Cache");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withJacksonDependencies() {
        // Given: Domain type with Jackson dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(entity("OrderLine", true));
        builder.addDependency(order.qualifiedName(), "com.fasterxml.jackson.annotation.JsonProperty");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withJdbcDependencies() {
        // Given: Domain type with JDBC dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "java.sql.Connection");
        builder.addDependency(order.qualifiedName(), "javax.sql.DataSource");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withAwsSdkDependencies() {
        // Given: Domain type with AWS SDK dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "software.amazon.awssdk.services.s3.S3Client");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withStripeDependencies() {
        // Given: Domain type with Stripe dependency
        var builder = new TestCodebaseBuilder();
        var payment = aggregate("Payment");
        builder.addUnit(payment);
        builder.addUnit(valueObject("Amount", false));
        builder.addDependency(payment.qualifiedName(), "com.stripe.model.Charge");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withAzureSdkDependencies() {
        // Given: Domain type with Azure SDK dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "com.azure.storage.blob.BlobClient");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withGoogleCloudDependencies() {
        // Given: Domain type with Google Cloud SDK dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "com.google.cloud.storage.Storage");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withKafkaDependencies() {
        // Given: Domain type with Kafka dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "org.apache.kafka.clients.producer.Producer");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withRabbitMqDependencies() {
        // Given: Domain type with RabbitMQ dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "com.rabbitmq.client.Channel");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withJmsDependencies() {
        // Given: Domain type with JMS dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "jakarta.jms.Queue");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withServletDependencies() {
        // Given: Domain type with Servlet dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "jakarta.servlet.http.HttpServletRequest");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withJaxRsDependencies() {
        // Given: Domain type with JAX-RS dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "jakarta.ws.rs.GET");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withValidationDependencies() {
        // Given: Domain type with validation framework dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "jakarta.validation.constraints.NotNull");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldDetectImpurity_withHibernateValidatorDependencies() {
        // Given: Domain type with Hibernate Validator dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addUnit(valueObject("Money", false));
        builder.addDependency(order.qualifiedName(), "org.hibernate.validator.constraints.Email");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 2 total = 50%
        assertThat(metric.value()).isEqualTo(50.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldCalculateCorrectly_withMixedPureAndImpureTypes() {
        // Given: 3 pure types, 2 impure types
        var builder = new TestCodebaseBuilder();

        // Pure types
        var customer = aggregate("Customer");
        var address = valueObject("Address", false);
        var email = valueObject("Email", false);

        // Impure types
        var order = aggregate("Order");
        var payment = entity("Payment", true);

        builder.addUnit(customer);
        builder.addUnit(address);
        builder.addUnit(email);
        builder.addUnit(order);
        builder.addUnit(payment);

        // Add infrastructure dependencies to impure types
        builder.addDependency(order.qualifiedName(), "jakarta.persistence.Entity");
        builder.addDependency(payment.qualifiedName(), "org.springframework.stereotype.Component");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 3 pure / 5 total = 60%
        assertThat(metric.value()).isEqualTo(60.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldReturn0Percent_whenAllDomainTypesImpure() {
        // Given: All domain types have infrastructure dependencies
        var builder = new TestCodebaseBuilder();

        var order = aggregate("Order");
        var customer = aggregate("Customer");
        var payment = entity("Payment", true);

        builder.addUnit(order);
        builder.addUnit(customer);
        builder.addUnit(payment);

        // All have infrastructure dependencies
        builder.addDependency(order.qualifiedName(), "jakarta.persistence.Entity");
        builder.addDependency(customer.qualifiedName(), "org.springframework.stereotype.Component");
        builder.addDependency(payment.qualifiedName(), "com.fasterxml.jackson.annotation.JsonProperty");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 0 pure / 3 total = 0%
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldIgnoreAllowedDependencies() {
        // Given: Domain types with allowed Java standard library dependencies
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        var money = valueObject("Money", false);

        builder.addUnit(order);
        builder.addUnit(money);

        // These are allowed dependencies (standard Java)
        builder.addDependency(order.qualifiedName(), "java.util.List");
        builder.addDependency(order.qualifiedName(), "java.time.LocalDateTime");
        builder.addDependency(money.qualifiedName(), "java.math.BigDecimal");
        builder.addDependency(money.qualifiedName(), "java.util.Currency");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Both types remain pure (standard Java is allowed)
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldHandleSingleDomainTypeWithDependency() {
        // Given: One domain type with infrastructure dependency
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);
        builder.addDependency(order.qualifiedName(), "jakarta.persistence.Entity");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 0 pure / 1 total = 0%
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldHandleSinglePureDomainType() {
        // Given: One domain type with no dependencies
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 1 pure / 1 total = 100%
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldCalculateCorrectly_withLargeCodebase() {
        // Given: 90 pure types, 10 impure types
        var builder = new TestCodebaseBuilder();

        // Add 90 pure types
        for (int i = 0; i < 90; i++) {
            builder.addUnit(domainClass("Pure" + i));
        }

        // Add 10 impure types
        for (int i = 0; i < 10; i++) {
            var impure = domainClass("Impure" + i);
            builder.addUnit(impure);
            builder.addDependency(impure.qualifiedName(), "jakarta.persistence.Entity");
        }

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: 90 pure / 100 total = 90%
        assertThat(metric.value()).isEqualTo(90.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }

    @Test
    void shouldOnlyConsiderDomainLayer() {
        // Given: Mixed codebase with infrastructure types (should be ignored)
        var builder = new TestCodebaseBuilder();

        // Domain types
        var order = aggregate("Order");
        builder.addUnit(order);

        // Infrastructure types (should not affect purity calculation)
        var adapter = infraClass("OrderAdapter");
        builder.addUnit(adapter);
        builder.addDependency(adapter.qualifiedName(), "jakarta.persistence.Entity");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Only domain types are considered, infrastructure types don't affect purity
        assertThat(metric.value()).isEqualTo(100.0);
        assertThat(metric.exceedsThreshold()).isFalse();
    }

    @Test
    void shouldDetectMultipleForbiddenDependenciesInSameType() {
        // Given: Domain type with multiple infrastructure dependencies
        var builder = new TestCodebaseBuilder();
        var order = aggregate("Order");
        builder.addUnit(order);

        // Multiple infrastructure dependencies
        builder.addDependency(order.qualifiedName(), "jakarta.persistence.Entity");
        builder.addDependency(order.qualifiedName(), "org.springframework.stereotype.Component");
        builder.addDependency(order.qualifiedName(), "com.fasterxml.jackson.annotation.JsonProperty");
        builder.addDependency(order.qualifiedName(), "java.sql.Connection");

        Codebase codebase = builder.build();

        // When
        Metric metric = calculator.calculate(codebase);

        // Then: Type is impure regardless of number of violations
        assertThat(metric.value()).isEqualTo(0.0);
        assertThat(metric.exceedsThreshold()).isTrue();
    }
}
