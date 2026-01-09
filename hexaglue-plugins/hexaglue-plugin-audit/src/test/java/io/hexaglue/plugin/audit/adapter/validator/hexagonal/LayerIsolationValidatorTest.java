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

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LayerIsolationValidator}.
 *
 * <p>Validates that layers respect isolation rules (Domain -> nothing, Application -> Domain, Infrastructure -> all).
 */
class LayerIsolationValidatorTest {

    private LayerIsolationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LayerIsolationValidator();
    }

    @Test
    @DisplayName("Should pass when domain depends only on domain")
    void shouldPass_whenDomainDependsOnlyOnDomain() {
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
    @DisplayName("Should pass when application depends on domain")
    void shouldPass_whenApplicationDependsOnDomain() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .addUnit(domainClass("Order"))
                .addDependency("com.example.application.OrderService", "com.example.domain.Order")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when infrastructure depends on all layers")
    void shouldPass_whenInfrastructureDependsOnAllLayers() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(infraClass("JpaOrderRepository"))
                .addUnit(domainClass("Order"))
                .addUnit(applicationClass("OrderService"))
                .addDependency("com.example.infrastructure.JpaOrderRepository", "com.example.domain.Order")
                .addDependency("com.example.infrastructure.JpaOrderRepository", "com.example.application.OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when domain depends on application")
    void shouldFail_whenDomainDependsOnApplication() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(applicationClass("OrderService"))
                .addDependency("com.example.domain.Order", "com.example.application.OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:layer-isolation");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message())
                .contains("DOMAIN")
                .contains("Order")
                .contains("APPLICATION")
                .contains("not allowed");
    }

    @Test
    @DisplayName("Should fail when domain depends on infrastructure")
    void shouldFail_whenDomainDependsOnInfrastructure() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(infraClass("JpaOrderRepository"))
                .addDependency("com.example.domain.Order", "com.example.infrastructure.JpaOrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("DOMAIN").contains("INFRASTRUCTURE");
    }

    @Test
    @DisplayName("Should fail when application depends on infrastructure")
    void shouldFail_whenApplicationDependsOnInfrastructure() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .addUnit(infraClass("JpaRepository"))
                .addDependency("com.example.application.OrderService", "com.example.infrastructure.JpaRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("APPLICATION").contains("INFRASTRUCTURE");
    }

    @Test
    @DisplayName("Should fail when application depends on presentation")
    void shouldFail_whenApplicationDependsOnPresentation() {
        // Given: Application should not depend on Presentation
        CodeUnit appService = new CodeUnit(
                "com.example.application.OrderService",
                CodeUnitKind.CLASS,
                LayerClassification.APPLICATION,
                RoleClassification.USE_CASE,
                List.of(),
                List.of(),
                new CodeMetrics(50, 5, 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));

        CodeUnit controller = new CodeUnit(
                "com.example.presentation.OrderController",
                CodeUnitKind.CLASS,
                LayerClassification.PRESENTATION,
                RoleClassification.ADAPTER,
                List.of(),
                List.of(),
                new CodeMetrics(50, 5, 3, 2, 80.0),
                new DocumentationInfo(true, 100, List.of()));

        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(appService)
                .addUnit(controller)
                .addDependency("com.example.application.OrderService", "com.example.presentation.OrderController")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("APPLICATION");
    }

    @Test
    @DisplayName("Should detect multiple layer violations")
    void shouldDetectMultipleLayerViolations() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(applicationClass("Service1"))
                .addUnit(applicationClass("Service2"))
                .addUnit(infraClass("Infra"))
                .addDependency("com.example.domain.Order", "com.example.application.Service1")
                .addDependency("com.example.domain.Order", "com.example.infrastructure.Infra")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("Should pass when codebase has no dependencies")
    void shouldPass_whenNoDependencies() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(domainClass("Order"))
                .addUnit(applicationClass("Service"))
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
                .addUnit(applicationClass("OrderService"))
                .addDependency("com.example.domain.Order", "com.example.application.OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description()).contains("Layer DOMAIN can only depend on");
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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:layer-isolation");
    }
}
