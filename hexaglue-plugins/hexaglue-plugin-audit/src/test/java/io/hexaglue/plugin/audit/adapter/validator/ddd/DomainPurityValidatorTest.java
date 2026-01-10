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

package io.hexaglue.plugin.audit.adapter.validator.ddd;

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.aggregate;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.domainClass;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.entity;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.infraClass;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.valueObject;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainPurityValidator}.
 *
 * <p>Validates that domain types are correctly checked for infrastructure dependencies.
 */
class DomainPurityValidatorTest {

    private DomainPurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DomainPurityValidator();
    }

    @Test
    @DisplayName("Should pass when domain type has no dependencies")
    void shouldPass_whenNoDependencies() {
        // Given: Pure domain entity with no dependencies
        Codebase codebase =
                new TestCodebaseBuilder().addUnit(aggregate("Order")).build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when domain type depends only on other domain types")
    void shouldPass_whenOnlyDomainDependencies() {
        // Given: Domain entity depending on another domain type
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addUnit(entity("OrderLine", true))
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when domain type depends on standard Java libraries")
    void shouldPass_whenStandardJavaDependencies() {
        // Given: Domain entity with standard Java dependencies
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addDependency("com.example.domain.Order", "java.util.List")
                .addDependency("com.example.domain.Order", "java.time.LocalDateTime")
                .addDependency("com.example.domain.Order", "java.math.BigDecimal")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when domain type imports JPA annotations")
    void shouldFail_whenJpaDependencies() {
        // Given: Domain entity with JPA dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addDependency("com.example.domain.Order", "javax.persistence.Entity")
                .addDependency("com.example.domain.Order", "javax.persistence.Id")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:domain-purity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message()).contains("Order").contains("2 forbidden infrastructure import");
        assertThat(violations.get(0).affectedTypes()).contains("com.example.domain.Order");
        assertThat(violations.get(0).evidence()).hasSize(2);
    }

    @Test
    @DisplayName("Should fail when domain type imports Jakarta persistence")
    void shouldFail_whenJakartaPersistence() {
        // Given: Domain entity with Jakarta persistence dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(entity("Product", true))
                .addDependency("com.example.domain.Product", "jakarta.persistence.Entity")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("JPA/Persistence")
                .contains("jakarta.persistence.Entity");
    }

    @Test
    @DisplayName("Should fail when domain type imports Spring Framework")
    void shouldFail_whenSpringDependencies() {
        // Given: Domain entity with Spring dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("OrderService"))
                .addDependency("com.example.domain.OrderService", "org.springframework.stereotype.Service")
                .addDependency(
                        "com.example.domain.OrderService", "org.springframework.beans.factory.annotation.Autowired")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("2 forbidden infrastructure import");
        assertThat(violations.get(0).evidence()).hasSize(2);
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("Spring Framework"));
    }

    @Test
    @DisplayName("Should fail when domain type imports Hibernate")
    void shouldFail_whenHibernateDependencies() {
        // Given: Domain entity with Hibernate dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Customer"))
                .addDependency("com.example.domain.Customer", "org.hibernate.annotations.Cache")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Hibernate ORM");
    }

    @Test
    @DisplayName("Should fail when domain type imports Jackson JSON")
    void shouldFail_whenJacksonDependencies() {
        // Given: Domain value object with Jackson dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(valueObject("Address", false))
                .addDependency("com.example.domain.Address", "com.fasterxml.jackson.annotation.JsonProperty")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Jackson JSON");
    }

    @Test
    @DisplayName("Should fail when domain type imports JDBC")
    void shouldFail_whenJdbcDependencies() {
        // Given: Domain entity with JDBC dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("DataMapper"))
                .addDependency("com.example.domain.DataMapper", "java.sql.Connection")
                .addDependency("com.example.domain.DataMapper", "javax.sql.DataSource")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("JDBC"));
    }

    @Test
    @DisplayName("Should fail when domain type imports AWS SDK")
    void shouldFail_whenAwsSdkDependencies() {
        // Given: Domain entity with AWS SDK dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("DocumentStorage"))
                .addDependency("com.example.domain.DocumentStorage", "software.amazon.awssdk.services.s3.S3Client")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("AWS SDK");
    }

    @Test
    @DisplayName("Should fail when domain type imports Stripe API")
    void shouldFail_whenStripeDependencies() {
        // Given: Domain entity with Stripe dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("PaymentProcessor"))
                .addDependency("com.example.domain.PaymentProcessor", "com.stripe.model.Charge")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Stripe API");
    }

    @Test
    @DisplayName("Should fail when domain type imports validation framework")
    void shouldFail_whenValidationFramework() {
        // Given: Domain entity with Bean Validation dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(valueObject("Email", false))
                .addDependency("com.example.domain.Email", "javax.validation.constraints.NotNull")
                .addDependency("com.example.domain.Email", "jakarta.validation.constraints.Email")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("Validation Framework"));
    }

    @Test
    @DisplayName("Should fail when domain type imports messaging frameworks")
    void shouldFail_whenMessagingDependencies() {
        // Given: Domain entity with Kafka dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("EventPublisher"))
                .addDependency("com.example.domain.EventPublisher", "org.apache.kafka.clients.producer.KafkaProducer")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Messaging");
    }

    @Test
    @DisplayName("Should fail when domain type imports web/servlet APIs")
    void shouldFail_whenWebDependencies() {
        // Given: Domain entity with Servlet dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("RequestHandler"))
                .addDependency("com.example.domain.RequestHandler", "javax.servlet.http.HttpServletRequest")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Web/HTTP");
    }

    @Test
    @DisplayName("Should pass when infrastructure layer has infrastructure dependencies")
    void shouldPass_whenInfrastructureLayerHasInfraDependencies() {
        // Given: Infrastructure class with Spring dependency (allowed)
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(infraClass("OrderRepositoryImpl"))
                .addDependency(
                        "com.example.infrastructure.OrderRepositoryImpl", "org.springframework.stereotype.Repository")
                .addDependency("com.example.infrastructure.OrderRepositoryImpl", "javax.persistence.EntityManager")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Infrastructure layer is allowed to have infrastructure dependencies
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should detect multiple violations across different domain types")
    void shouldDetectMultipleViolations() {
        // Given: Multiple domain types with forbidden dependencies
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addUnit(entity("Product", true))
                .addUnit(valueObject("Money", false))
                .addDependency("com.example.domain.Order", "javax.persistence.Entity")
                .addDependency("com.example.domain.Product", "org.springframework.stereotype.Component")
                .addDependency("com.example.domain.Money", "com.fasterxml.jackson.annotation.JsonCreator")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should find 3 violations, one per domain type
        assertThat(violations).hasSize(3);
        assertThat(violations)
                .extracting(v -> v.affectedTypes().get(0))
                .containsExactlyInAnyOrder(
                        "com.example.domain.Order", "com.example.domain.Product", "com.example.domain.Money");
    }

    @Test
    @DisplayName("Should handle mixed dependencies correctly")
    void shouldHandleMixedDependencies() {
        // Given: Domain entity with both valid and forbidden dependencies
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Customer"))
                .addUnit(valueObject("Address", false))
                .addDependency("com.example.domain.Customer", "com.example.domain.Address") // Valid
                .addDependency("com.example.domain.Customer", "java.util.Set") // Valid
                .addDependency("com.example.domain.Customer", "javax.persistence.Entity") // Forbidden
                .addDependency("com.example.domain.Customer", "org.springframework.stereotype.Component") // Forbidden
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should only report forbidden dependencies
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).hasSize(2);
        assertThat(violations.get(0).message()).contains("2 forbidden infrastructure import");
    }

    @Test
    @DisplayName("Should pass when domain layer is empty")
    void shouldPass_whenNoDomainTypes() {
        // Given: Codebase with only infrastructure types
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(infraClass("OrderRepositoryImpl"))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide dependency evidence for violations")
    void shouldProvideDependencyEvidence() {
        // Given: Domain entity with forbidden dependency
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(aggregate("Order"))
                .addDependency("com.example.domain.Order", "javax.persistence.Entity")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("Forbidden")
                .contains("JPA/Persistence")
                .contains("javax.persistence.Entity");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.MAJOR);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("ddd:domain-purity");
    }

    @Test
    @DisplayName("Should categorize different dependency types correctly")
    void shouldCategorizeDependenciesCorrectly() {
        // Given: Domain types with various forbidden dependencies
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("TestType1"))
                .addUnit(domainClass("TestType2"))
                .addUnit(domainClass("TestType3"))
                .addUnit(domainClass("TestType4"))
                .addDependency("com.example.domain.TestType1", "javax.persistence.Entity")
                .addDependency("com.example.domain.TestType2", "org.springframework.stereotype.Component")
                .addDependency("com.example.domain.TestType3", "com.fasterxml.jackson.databind.ObjectMapper")
                .addDependency("com.example.domain.TestType4", "java.sql.ResultSet")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should correctly categorize each type
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .flatExtracting(v -> v.evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("JPA/Persistence"))
                .anyMatch(desc -> desc.contains("Spring Framework"))
                .anyMatch(desc -> desc.contains("Jackson JSON"))
                .anyMatch(desc -> desc.contains("JDBC"));
    }
}
