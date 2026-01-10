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

import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.applicationClass;
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.port;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortDirectionValidator}.
 *
 * <p>Validates that port direction checking works correctly based on naming
 * conventions and usage patterns.
 */
class PortDirectionValidatorTest {

    private PortDirectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortDirectionValidator();
    }

    @Test
    @DisplayName("Should pass when DRIVEN port is used by application service")
    void shouldPass_whenDrivenPortIsUsedByApplicationService() {
        // Given: A repository port (DRIVEN) and an application service that uses it
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepository", CodeUnitKind.INTERFACE))
                .addUnit(applicationClass("OrderService"))
                .addDependency("com.example.application.OrderService", "com.example.domain.port.OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when DRIVEN port is not used by any application service")
    void shouldFail_whenDrivenPortIsNotUsed() {
        // Given: A repository port (DRIVEN) with no application service using it
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepository", CodeUnitKind.INTERFACE))
                .addUnit(applicationClass("OrderService")) // Exists but doesn't use the repository
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:port-direction");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message())
                .contains("OrderRepository")
                .contains("DRIVEN port")
                .contains("not used by any application service");
    }

    @Test
    @DisplayName("Should pass when DRIVING port is referenced by application service")
    void shouldPass_whenDrivingPortIsReferencedByApplicationService() {
        // Given: A service port (DRIVING) and an application service that references it
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .addUnit(applicationClass("OrderServiceImpl"))
                .addDependency("com.example.application.OrderServiceImpl", "com.example.domain.port.OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when DRIVING port is not implemented by any application service")
    void shouldFail_whenDrivingPortIsNotImplemented() {
        // Given: A service port (DRIVING) with no application service implementing it
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:port-direction");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message())
                .contains("OrderService")
                .contains("DRIVING port")
                .contains("not implemented by any application service");
    }

    @Test
    @DisplayName("Should recognize common DRIVEN port suffixes")
    void shouldRecognizeDrivenPortSuffixes() {
        // Given: Various DRIVEN port types
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("PaymentGateway", CodeUnitKind.INTERFACE))
                .addUnit(port("EmailClient", CodeUnitKind.INTERFACE))
                .addUnit(port("EventPublisher", CodeUnitKind.INTERFACE))
                .addUnit(port("ProductStore", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: All should be detected as DRIVEN and flagged (no app service uses them)
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .extracting(v -> v.message())
                .allMatch(msg -> msg.contains("DRIVEN port") && msg.contains("not used by"));
    }

    @Test
    @DisplayName("Should recognize common DRIVING port suffixes")
    void shouldRecognizeDrivingPortSuffixes() {
        // Given: Various DRIVING port types
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("PlaceOrderUseCase", CodeUnitKind.INTERFACE))
                .addUnit(port("OrderFacade", CodeUnitKind.INTERFACE))
                .addUnit(port("PaymentHandler", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: All should be detected as DRIVING and flagged (no app service implements them)
        assertThat(violations).hasSize(3);
        assertThat(violations)
                .extracting(v -> v.message())
                .allMatch(msg -> msg.contains("DRIVING port") && msg.contains("not implemented by"));
    }

    @Test
    @DisplayName("Should skip ports with unknown direction")
    void shouldSkipPortsWithUnknownDirection() {
        // Given: A port with non-standard naming
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderManager", CodeUnitKind.INTERFACE)) // No recognizable suffix
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should not produce violations (cannot infer direction)
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate multiple ports correctly")
    void shouldValidateMultiplePorts() {
        // Given: Mix of valid and invalid ports
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepository", CodeUnitKind.INTERFACE)) // DRIVEN, not used - INVALID
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE)) // DRIVING, not impl - INVALID
                .addUnit(port("ProductRepository", CodeUnitKind.INTERFACE)) // DRIVEN, used - VALID
                .addUnit(port("ProductService", CodeUnitKind.INTERFACE)) // DRIVING, impl - VALID
                .addUnit(applicationClass("ProductManager"))
                .addDependency("com.example.application.ProductManager", "com.example.domain.port.ProductRepository")
                .addDependency("com.example.application.ProductManager", "com.example.domain.port.ProductService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Two violations (OrderRepository and OrderService)
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .flatExtracting(v -> v.affectedTypes())
                .containsExactlyInAnyOrder(
                        "com.example.domain.port.OrderRepository", "com.example.domain.port.OrderService");
    }

    @Test
    @DisplayName("Should provide dependency evidence")
    void shouldProvideDependencyEvidence() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepository", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("DRIVEN ports")
                .contains("application services");
    }

    @Test
    @DisplayName("Should pass when codebase has no ports")
    void shouldPass_whenNoPorts() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(applicationClass("OrderService"))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:port-direction");
    }

    @Test
    @DisplayName("Should handle edge case: port with Repository and Service suffix")
    void shouldHandleConflictingSuffixes() {
        // Given: Port with conflicting name (should match first found - Repository)
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepositoryService", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then: Should be treated as DRIVING (Service suffix at end)
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("DRIVING port");
    }
}
