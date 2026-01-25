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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DomainPurityValidator}.
 *
 * <p>Validates that domain types are correctly checked for infrastructure dependencies
 * using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
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
        // Given: Pure domain aggregate with no dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when domain type depends only on other domain types")
    void shouldPass_whenOnlyDomainDependencies() {
        // Given: Domain aggregate depending on another domain entity
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.OrderLine")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "com.example.domain.OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when domain type depends on standard Java libraries")
    void shouldPass_whenStandardJavaDependencies() {
        // Given: Domain aggregate with standard Java dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "java.util.List")
                .addDependency("com.example.domain.Order", "java.time.LocalDateTime")
                .addDependency("com.example.domain.Order", "java.math.BigDecimal")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when domain type imports JPA annotations")
    void shouldFail_whenJpaDependencies() {
        // Given: Domain aggregate with JPA dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "javax.persistence.Entity")
                .addDependency("com.example.domain.Order", "javax.persistence.Id")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("ddd:domain-purity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(violations.get(0).message()).contains("Order").contains("2 forbidden infrastructure import");
        assertThat(violations.get(0).affectedTypes()).contains("com.example.domain.Order");
        assertThat(violations.get(0).evidence()).hasSize(2);
    }

    @Test
    @DisplayName("Should fail when domain type imports Jakarta persistence")
    void shouldFail_whenJakartaPersistence() {
        // Given: Domain entity with Jakarta persistence dependency
        ArchitecturalModel model =
                new TestModelBuilder().addEntity("com.example.domain.Product").build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Product", "jakarta.persistence.Entity")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        // Given: Domain type with Spring dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.OrderService", "org.springframework.stereotype.Service")
                .addDependency(
                        "com.example.domain.OrderService", "org.springframework.beans.factory.annotation.Autowired")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        // Given: Domain aggregate with Hibernate dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Customer")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Customer", "org.hibernate.annotations.Cache")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Hibernate ORM");
    }

    @Test
    @DisplayName("Should fail when domain type imports Jackson JSON")
    void shouldFail_whenJacksonDependencies() {
        // Given: Domain value object with Jackson dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Address")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Address", "com.fasterxml.jackson.annotation.JsonProperty")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Jackson JSON");
    }

    @Test
    @DisplayName("Should fail when domain type imports JDBC")
    void shouldFail_whenJdbcDependencies() {
        // Given: Domain type with JDBC dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.DataMapper")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.DataMapper", "java.sql.Connection")
                .addDependency("com.example.domain.DataMapper", "javax.sql.DataSource")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("JDBC"));
    }

    @Test
    @DisplayName("Should fail when domain type imports AWS SDK")
    void shouldFail_whenAwsSdkDependencies() {
        // Given: Domain type with AWS SDK dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.DocumentStorage")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.DocumentStorage", "software.amazon.awssdk.services.s3.S3Client")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("AWS SDK");
    }

    @Test
    @DisplayName("Should fail when domain type imports Stripe API")
    void shouldFail_whenStripeDependencies() {
        // Given: Domain type with Stripe dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.PaymentProcessor")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.PaymentProcessor", "com.stripe.model.Charge")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Stripe API");
    }

    @Test
    @DisplayName("Should fail when domain type imports validation framework")
    void shouldFail_whenValidationFramework() {
        // Given: Domain value object with Bean Validation dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.Email")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Email", "javax.validation.constraints.NotNull")
                .addDependency("com.example.domain.Email", "jakarta.validation.constraints.Email")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("Validation Framework"));
    }

    @Test
    @DisplayName("Should fail when domain type imports messaging frameworks")
    void shouldFail_whenMessagingDependencies() {
        // Given: Domain type with Kafka dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.EventPublisher")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.EventPublisher", "org.apache.kafka.clients.producer.KafkaProducer")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Messaging");
    }

    @Test
    @DisplayName("Should fail when domain type imports web/servlet APIs")
    void shouldFail_whenWebDependencies() {
        // Given: Domain type with Servlet dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.RequestHandler")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.RequestHandler", "javax.servlet.http.HttpServletRequest")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Web/HTTP");
    }

    @Test
    @DisplayName("Should detect multiple violations across different domain types")
    void shouldDetectMultipleViolations() {
        // Given: Multiple domain types with forbidden dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .addEntity("com.example.domain.Product")
                .addValueObject("com.example.domain.Money")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "javax.persistence.Entity")
                .addDependency("com.example.domain.Product", "org.springframework.stereotype.Component")
                .addDependency("com.example.domain.Money", "com.fasterxml.jackson.annotation.JsonCreator")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        // Given: Domain aggregate with both valid and forbidden dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Customer")
                .addValueObject("com.example.domain.Address")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Customer", "com.example.domain.Address") // Valid
                .addDependency("com.example.domain.Customer", "java.util.Set") // Valid
                .addDependency("com.example.domain.Customer", "javax.persistence.Entity") // Forbidden
                .addDependency("com.example.domain.Customer", "org.springframework.stereotype.Component") // Forbidden
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Should only report forbidden dependencies for Customer
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).hasSize(2);
        assertThat(violations.get(0).message()).contains("2 forbidden infrastructure import");
    }

    @Test
    @DisplayName("Should pass when codebase has no domain types")
    void shouldPass_whenNoDomainTypes() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide dependency evidence for violations")
    void shouldProvideDependencyEvidence() {
        // Given: Domain aggregate with forbidden dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.Order", "javax.persistence.Entity")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.CRITICAL);
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
        ArchitecturalModel model = new TestModelBuilder()
                .addValueObject("com.example.domain.TestType1")
                .addValueObject("com.example.domain.TestType2")
                .addValueObject("com.example.domain.TestType3")
                .addValueObject("com.example.domain.TestType4")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.domain.TestType1", "javax.persistence.Entity")
                .addDependency("com.example.domain.TestType2", "org.springframework.stereotype.Component")
                .addDependency("com.example.domain.TestType3", "com.fasterxml.jackson.databind.ObjectMapper")
                .addDependency("com.example.domain.TestType4", "java.sql.ResultSet")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
