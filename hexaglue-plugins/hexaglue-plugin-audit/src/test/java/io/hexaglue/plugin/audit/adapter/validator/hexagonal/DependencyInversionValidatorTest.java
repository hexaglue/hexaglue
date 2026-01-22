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
import io.hexaglue.arch.model.DrivenPortType;
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
 * Tests for {@link DependencyInversionValidator}.
 *
 * <p>Validates that the Dependency Inversion Principle is correctly enforced
 * using the v5 ArchType API. Application components should depend on
 * abstractions (interfaces/ports) rather than concrete implementations.
 *
 * <p><strong>Note:</strong> In v5 API, infrastructure types are not part of the
 * classified model. The validator checks if application layer types depend on
 * concrete (non-interface) types that are NOT ports. Since infrastructure types
 * aren't in the model, this validator primarily ensures application depends
 * on ports rather than concrete implementations.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class DependencyInversionValidatorTest {

    private static final String DOMAIN_PACKAGE = "com.example.domain";
    private static final String APP_PACKAGE = "com.example.application";
    private static final String PORT_PACKAGE = "com.example.domain.port";

    private DependencyInversionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DependencyInversionValidator();
    }

    @Test
    @DisplayName("Should pass when application depends on port interface")
    void shouldPass_whenApplicationDependsOnPortInterface() {
        // Given: Application service depends on port interface (correct)
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application has no dependencies")
    void shouldPass_whenApplicationHasNoDependencies() {
        // Given: Application service with no dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application depends on domain")
    void shouldPass_whenApplicationDependsOnDomain() {
        // Given: Application depends on domain (allowed)
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", DOMAIN_PACKAGE + ".Order")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when application depends on multiple ports")
    void shouldPass_whenApplicationDependsOnMultiplePorts() {
        // Given: Application depends on multiple port interfaces
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addDrivenPort(PORT_PACKAGE + ".PaymentGateway", DrivenPortType.GATEWAY)
                .addDrivingPort(PORT_PACKAGE + ".OrderUseCase")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderRepository")
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".PaymentGateway")
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderUseCase")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when mixing interface and domain dependencies correctly")
    void shouldPass_whenMixingInterfaceAndDomainDependenciesCorrectly() {
        // Given: Application depends on port interface + domain concrete (both allowed)
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderRepository")
                .addDependency(APP_PACKAGE + ".OrderService", DOMAIN_PACKAGE + ".Order")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should ignore domain layer dependencies")
    void shouldIgnore_domainLayerDependencies() {
        // Given: Domain has dependencies (not checked by this validator)
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addEntity(DOMAIN_PACKAGE + ".OrderLine")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(DOMAIN_PACKAGE + ".Order", DOMAIN_PACKAGE + ".OrderLine")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should ignore external dependencies")
    void shouldIgnore_externalDependencies() {
        // Given: Application depends on external library (not in model)
        ArchitecturalModel model = new TestModelBuilder()
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderService", "java.util.List")
                .addDependency(APP_PACKAGE + ".OrderService", "org.slf4j.Logger")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - external deps are not in the model so ignored
        assertThat(violations).isEmpty();
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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:dependency-inversion");
    }

    @Test
    @DisplayName("Should pass when no application layer units exist")
    void shouldPass_whenNoApplicationLayerUnitsExist() {
        // Given: Only domain, no application
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty codebase gracefully")
    void shouldHandleEmptyCodebaseGracefully() {
        // Given: Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle complex valid architecture")
    void shouldHandleComplexValidArchitecture() {
        // Given: Realistic architecture with proper dependencies
        ArchitecturalModel model = new TestModelBuilder()
                .addAggregateRoot(DOMAIN_PACKAGE + ".Order")
                .addEntity(DOMAIN_PACKAGE + ".OrderLine")
                .addValueObject(DOMAIN_PACKAGE + ".Money")
                .addApplicationService(APP_PACKAGE + ".OrderService")
                .addApplicationService(APP_PACKAGE + ".PaymentService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addDrivenPort(PORT_PACKAGE + ".PaymentGateway", DrivenPortType.GATEWAY)
                .addDrivingPort(PORT_PACKAGE + ".PlaceOrderUseCase")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                // Domain internal dependencies
                .addDependency(DOMAIN_PACKAGE + ".Order", DOMAIN_PACKAGE + ".OrderLine")
                .addDependency(DOMAIN_PACKAGE + ".Order", DOMAIN_PACKAGE + ".Money")
                // Application -> Domain (valid)
                .addDependency(APP_PACKAGE + ".OrderService", DOMAIN_PACKAGE + ".Order")
                // Application -> Ports (valid, DIP compliant)
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".OrderRepository")
                .addDependency(APP_PACKAGE + ".PaymentService", PORT_PACKAGE + ".PaymentGateway")
                .addDependency(APP_PACKAGE + ".OrderService", PORT_PACKAGE + ".PlaceOrderUseCase")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - all dependencies are valid
        assertThat(violations).isEmpty();
    }
}
