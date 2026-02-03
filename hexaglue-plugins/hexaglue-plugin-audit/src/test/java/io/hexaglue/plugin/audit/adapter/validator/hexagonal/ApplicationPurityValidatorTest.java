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

package io.hexaglue.plugin.audit.adapter.validator.hexagonal;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ApplicationPurityValidator}.
 *
 * <p>Validates that application layer types are correctly checked for infrastructure
 * dependencies using both classified types and codebase package scanning.
 *
 * @since 5.0.0
 */
class ApplicationPurityValidatorTest {

    private ApplicationPurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ApplicationPurityValidator();
    }

    @Test
    @DisplayName("Should pass when application service has no dependencies")
    void shouldPass_whenNoDependencies() {
        // Given: Pure application service with no dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application service depends only on domain types")
    void shouldPass_whenOnlyDomainDependencies() {
        // Given: Application service depending on domain types
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application service depends on standard Java libraries")
    void shouldPass_whenStandardJavaDependencies() {
        // Given: Application service with standard Java dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "java.util.List")
                .addDependency("com.example.application.OrderService", "java.time.LocalDateTime")
                .addDependency("com.example.application.OrderService", "java.util.Optional")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when application service imports Spring Framework")
    void shouldFail_whenSpringDependencies() {
        // Given: Application service with Spring dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "org.springframework.stereotype.Service")
                .addDependency(
                        "com.example.application.OrderService",
                        "org.springframework.transaction.annotation.Transactional")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:application-purity");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message()).contains("OrderService").contains("2 forbidden infrastructure import");
        assertThat(violations.get(0).affectedTypes()).contains("com.example.application.OrderService");
        assertThat(violations.get(0).evidence()).hasSize(2);
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("Spring Framework"));
    }

    @Test
    @DisplayName("Should fail when application service imports JPA annotations")
    void shouldFail_whenJpaDependencies() {
        // Given: Application service with JPA dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "jakarta.persistence.EntityManager")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("JPA/Persistence")
                .contains("jakarta.persistence.EntityManager");
    }

    @Test
    @DisplayName("Should fail when application service imports Hibernate")
    void shouldFail_whenHibernateDependencies() {
        // Given: Application service with Hibernate dependency
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "org.hibernate.Session")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence().get(0).description()).contains("Hibernate ORM");
    }

    @Test
    @DisplayName("Should detect violations in unclassified types in application packages")
    void shouldDetectViolations_inUnclassifiedApplicationPackageTypes() {
        // Given: No classified application types, but codebase has a type in .application. package
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.PaymentUseCase", "org.springframework.stereotype.Service")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("PaymentUseCase");
        assertThat(violations.get(0).affectedTypes()).contains("com.example.application.PaymentUseCase");
        assertThat(violations.get(0).evidence())
                .extracting(e -> e.description())
                .anyMatch(desc -> desc.contains("Spring Framework"));
    }

    @Test
    @DisplayName("Should detect multiple violations across different application services")
    void shouldDetectMultipleViolations() {
        // Given: Multiple application services with forbidden dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .addApplicationService("com.example.application.PaymentService")
                .addApplicationService("com.example.application.ShippingService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "org.springframework.stereotype.Service")
                .addDependency("com.example.application.PaymentService", "jakarta.persistence.EntityManager")
                .addDependency("com.example.application.ShippingService", "com.fasterxml.jackson.databind.ObjectMapper")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Should find 3 violations, one per application service
        assertThat(violations).hasSize(3);
        assertThat(violations)
                .extracting(v -> v.affectedTypes().get(0))
                .containsExactlyInAnyOrder(
                        "com.example.application.OrderService",
                        "com.example.application.PaymentService",
                        "com.example.application.ShippingService");
    }

    @Test
    @DisplayName("Should handle mixed valid and forbidden dependencies")
    void shouldHandleMixedDependencies() {
        // Given: Application service with both valid and forbidden dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService("com.example.application.OrderService")
                .addAggregateRoot("com.example.domain.Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency("com.example.application.OrderService", "com.example.domain.Order") // Valid
                .addDependency("com.example.application.OrderService", "java.util.List") // Valid
                .addDependency(
                        "com.example.application.OrderService", "org.springframework.stereotype.Service") // Forbidden
                .addDependency(
                        "com.example.application.OrderService",
                        "org.springframework.transaction.annotation.Transactional") // Forbidden
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Should only report forbidden dependencies
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).hasSize(2);
        assertThat(violations.get(0).message()).contains("2 forbidden infrastructure import");
    }

    @Test
    @DisplayName("Should pass when no application types exist")
    void shouldPass_whenNoApplicationTypes() {
        // Given: Empty model with no application types
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:application-purity");
    }
}
