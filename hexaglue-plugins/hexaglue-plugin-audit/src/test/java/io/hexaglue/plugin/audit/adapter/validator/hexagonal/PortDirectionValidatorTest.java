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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.ir.PortDirection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortDirectionValidator}.
 *
 * <p>Validates that port direction checking works correctly using the port direction
 * information provided by the core's ArchitectureQuery.
 */
class PortDirectionValidatorTest {

    private PortDirectionValidator validator;
    private ArchitectureQuery mockQuery;

    @BeforeEach
    void setUp() {
        validator = new PortDirectionValidator();
        mockQuery = mock(ArchitectureQuery.class);
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

        when(mockQuery.findPortDirection("com.example.domain.port.OrderRepository"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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

        when(mockQuery.findPortDirection("com.example.domain.port.OrderRepository"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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

        when(mockQuery.findPortDirection("com.example.domain.port.OrderService"))
                .thenReturn(Optional.of(PortDirection.DRIVING));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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

        when(mockQuery.findPortDirection("com.example.domain.port.OrderService"))
                .thenReturn(Optional.of(PortDirection.DRIVING));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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
    @DisplayName("Should validate multiple DRIVEN ports correctly")
    void shouldValidateMultipleDrivenPorts() {
        // Given: Various DRIVEN port types
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("PaymentGateway", CodeUnitKind.INTERFACE))
                .addUnit(port("EmailClient", CodeUnitKind.INTERFACE))
                .addUnit(port("EventPublisher", CodeUnitKind.INTERFACE))
                .addUnit(port("ProductStore", CodeUnitKind.INTERFACE))
                .build();

        when(mockQuery.findPortDirection("com.example.domain.port.PaymentGateway"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));
        when(mockQuery.findPortDirection("com.example.domain.port.EmailClient"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));
        when(mockQuery.findPortDirection("com.example.domain.port.EventPublisher"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));
        when(mockQuery.findPortDirection("com.example.domain.port.ProductStore"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then: All should be flagged (no app service uses them)
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .extracting(v -> v.message())
                .allMatch(msg -> msg.contains("DRIVEN port") && msg.contains("not used by"));
    }

    @Test
    @DisplayName("Should validate multiple DRIVING ports correctly")
    void shouldValidateMultipleDrivingPorts() {
        // Given: Various DRIVING port types
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("PlaceOrderUseCase", CodeUnitKind.INTERFACE))
                .addUnit(port("OrderFacade", CodeUnitKind.INTERFACE))
                .addUnit(port("PaymentHandler", CodeUnitKind.INTERFACE))
                .build();

        when(mockQuery.findPortDirection("com.example.domain.port.PlaceOrderUseCase"))
                .thenReturn(Optional.of(PortDirection.DRIVING));
        when(mockQuery.findPortDirection("com.example.domain.port.OrderFacade"))
                .thenReturn(Optional.of(PortDirection.DRIVING));
        when(mockQuery.findPortDirection("com.example.domain.port.PaymentHandler"))
                .thenReturn(Optional.of(PortDirection.DRIVING));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then: All should be flagged (no app service implements them)
        assertThat(violations).hasSize(3);
        assertThat(violations)
                .extracting(v -> v.message())
                .allMatch(msg -> msg.contains("DRIVING port") && msg.contains("not implemented by"));
    }

    @Test
    @DisplayName("Should skip ports with unknown direction")
    void shouldSkipPortsWithUnknownDirection() {
        // Given: A port without direction in core
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderManager", CodeUnitKind.INTERFACE))
                .build();

        when(mockQuery.findPortDirection("com.example.domain.port.OrderManager"))
                .thenReturn(Optional.empty());

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then: Should not produce violations
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

        when(mockQuery.findPortDirection("com.example.domain.port.OrderRepository"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));
        when(mockQuery.findPortDirection("com.example.domain.port.OrderService"))
                .thenReturn(Optional.of(PortDirection.DRIVING));
        when(mockQuery.findPortDirection("com.example.domain.port.ProductRepository"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));
        when(mockQuery.findPortDirection("com.example.domain.port.ProductService"))
                .thenReturn(Optional.of(PortDirection.DRIVING));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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

        when(mockQuery.findPortDirection("com.example.domain.port.OrderRepository"))
                .thenReturn(Optional.of(PortDirection.DRIVEN));

        // When
        List<Violation> violations = validator.validate(codebase, mockQuery);

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
        List<Violation> violations = validator.validate(codebase, mockQuery);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when query is null")
    void shouldReturnEmptyList_whenQueryIsNull() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepository", CodeUnitKind.INTERFACE))
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
}
