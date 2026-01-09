/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.validator.hexagonal;

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.applicationClass;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.domainClass;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.infraClass;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.DependencyEvidence;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DependencyDirectionValidator}.
 *
 * <p>Validates that dependencies flow in the correct direction (domain should not depend on infrastructure).
 */
class DependencyDirectionValidatorTest {

    private DependencyDirectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DependencyDirectionValidator();
    }

    @Test
    @DisplayName("Should pass when domain has no dependencies")
    void shouldPass_whenDomainHasNoDependencies() {
        // Given
        Codebase codebase = new TestCodebaseBuilder().addUnit(domainClass("Order")).build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when domain depends on domain")
    void shouldPass_whenDomainDependsOnDomain() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(domainClass("Customer"))
                .addDependency("com.example.domain.Order", "com.example.domain.Customer")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when domain depends on infrastructure")
    void shouldFail_whenDomainDependsOnInfrastructure() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(infraClass("OrderRepository"))
                .addDependency("com.example.domain.Order", "com.example.infrastructure.OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:dependency-direction");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.BLOCKER);
        assertThat(violations.get(0).message())
                .contains("DOMAIN")
                .contains("Order")
                .contains("Infrastructure")
                .contains("OrderRepository");
    }

    @Test
    @DisplayName("Should fail when application depends on infrastructure")
    void shouldFail_whenApplicationDependsOnInfrastructure() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .addUnit(infraClass("JpaOrderRepository"))
                .addDependency(
                        "com.example.application.OrderService", "com.example.infrastructure.JpaOrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:dependency-direction");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.BLOCKER);
        assertThat(violations.get(0).message()).contains("APPLICATION").contains("Infrastructure");
    }

    @Test
    @DisplayName("Should detect multiple violations")
    void shouldDetectMultipleViolations() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(domainClass("Customer"))
                .addUnit(infraClass("OrderRepo"))
                .addUnit(infraClass("CustomerRepo"))
                .addDependency("com.example.domain.Order", "com.example.infrastructure.OrderRepo")
                .addDependency("com.example.domain.Customer", "com.example.infrastructure.CustomerRepo")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("Should pass when infrastructure depends on domain")
    void shouldPass_whenInfrastructureDependsOnDomain() {
        // Given: Infrastructure -> Domain is allowed (correct direction)
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(infraClass("JpaOrderRepository"))
                .addDependency("com.example.infrastructure.JpaOrderRepository", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should provide dependency evidence")
    void shouldProvideDependencyEvidence() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(infraClass("OrderRepository"))
                .addDependency("com.example.domain.Order", "com.example.infrastructure.OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        // DependencyEvidence.of() returns RelationshipEvidence
        assertThat(violations.get(0).evidence().get(0))
                .extracting(e -> e.description())
                .asString()
                .contains("Illegal dependency")
                .contains("DOMAIN")
                .contains("Infrastructure");
    }

    @Test
    @DisplayName("Should return correct default severity")
    void shouldReturnCorrectDefaultSeverity() {
        // When/Then
        assertThat(validator.defaultSeverity()).isEqualTo(Severity.BLOCKER);
    }

    @Test
    @DisplayName("Should return correct constraint ID")
    void shouldReturnCorrectConstraintId() {
        // When/Then
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:dependency-direction");
    }
}
