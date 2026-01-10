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
import static io.hexaglue.plugin.audit.util.TestCodebaseBuilder.infraClass;
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
 * Tests for {@link PortCoverageValidator}.
 *
 * <p>Validates that the port coverage validator correctly identifies ports
 * without adapter implementations.
 */
class PortCoverageValidatorTest {

    private PortCoverageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortCoverageValidator();
    }

    @Test
    @DisplayName("Should pass when port has adapter implementation")
    void shouldPass_whenPortHasAdapterImplementation() {
        // Given - Port with adapter that depends on it
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .addUnit(infraClass("OrderServiceAdapter"))
                .addDependency("com.example.infrastructure.OrderServiceAdapter", "com.example.domain.port.OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when port has no implementation")
    void shouldFail_whenPortHasNoImplementation() {
        // Given - Port without any adapter
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:port-coverage");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message()).contains("OrderService").contains("no adapter implementation");
        assertThat(violations.get(0).affectedTypes()).containsExactly("com.example.domain.port.OrderService");
    }

    @Test
    @DisplayName("Should check all ports")
    void shouldCheckAllPorts() {
        // Given - Multiple ports, some with implementations, some without
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .addUnit(infraClass("OrderServiceAdapter"))
                .addUnit(port("PaymentGateway", CodeUnitKind.INTERFACE))
                .addUnit(port("NotificationService", CodeUnitKind.INTERFACE))
                .addUnit(infraClass("NotificationServiceAdapter"))
                .addDependency("com.example.infrastructure.OrderServiceAdapter", "com.example.domain.port.OrderService")
                .addDependency(
                        "com.example.infrastructure.NotificationServiceAdapter",
                        "com.example.domain.port.NotificationService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then - Only PaymentGateway should have violation
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("PaymentGateway");
    }

    @Test
    @DisplayName("Should fail when port is implemented in application layer instead of adapter layer")
    void shouldFail_whenPortImplementedInApplicationLayerOnly() {
        // Given - Port with application layer implementation (not valid adapter)
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .addUnit(applicationClass("OrderServiceImpl"))
                .addDependency("com.example.application.OrderServiceImpl", "com.example.domain.port.OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then - Should still fail because application layer implementation is not a valid adapter
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("OrderService").contains("no adapter implementation");
    }

    @Test
    @DisplayName("Should pass when no ports exist")
    void shouldPass_whenNoPorts() {
        // Given - Codebase without ports
        Codebase codebase =
                new TestCodebaseBuilder().addUnit(infraClass("SomeAdapter")).build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when port has multiple adapter implementations")
    void shouldPass_whenPortHasMultipleAdapterImplementations() {
        // Given - Port with multiple adapters
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderRepository", CodeUnitKind.INTERFACE))
                .addUnit(infraClass("JpaOrderRepositoryAdapter"))
                .addUnit(infraClass("MongoOrderRepositoryAdapter"))
                .addDependency(
                        "com.example.infrastructure.JpaOrderRepositoryAdapter",
                        "com.example.domain.port.OrderRepository")
                .addDependency(
                        "com.example.infrastructure.MongoOrderRepositoryAdapter",
                        "com.example.domain.port.OrderRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when no adapters exist at all")
    void shouldFail_whenNoAdaptersExist() {
        // Given - Ports without any adapters in the system
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .addUnit(port("PaymentGateway", CodeUnitKind.INTERFACE))
                .addUnit(applicationClass("SomeApplicationService"))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then - Both ports should have violations
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(v -> v.message()).allMatch(msg -> msg.contains("no adapter implementation"));
    }

    @Test
    @DisplayName("Should provide relationship evidence")
    void shouldProvideRelationshipEvidence() {
        // Given
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("OrderService", CodeUnitKind.INTERFACE))
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).evidence()).isNotEmpty();
        assertThat(violations.get(0).evidence().get(0).description())
                .contains("Ports must have at least one adapter implementation")
                .contains("infrastructure layer");
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
        assertThat(validator.constraintId().value()).isEqualTo("hexagonal:port-coverage");
    }

    @Test
    @DisplayName("Should pass when adapter exists even without direct dependency tracking")
    void shouldPass_whenAdapterExistsAndDependsOnPort() {
        // Given - Realistic scenario with adapter implementing port
        Codebase codebase = new TestCodebaseBuilder()
                .addUnit(port("CustomerRepository", CodeUnitKind.INTERFACE))
                .addUnit(infraClass("JpaCustomerRepository"))
                .addDependency(
                        "com.example.infrastructure.JpaCustomerRepository",
                        "com.example.domain.port.CustomerRepository")
                .build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty codebase")
    void shouldHandleEmptyCodebase() {
        // Given
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }
}
