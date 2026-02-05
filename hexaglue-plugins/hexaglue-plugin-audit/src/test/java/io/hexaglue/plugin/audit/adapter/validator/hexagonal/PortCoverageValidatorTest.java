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
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortCoverageValidator}.
 *
 * <p>Validates that the port coverage validator correctly identifies ports
 * without adapter implementations using the v5 ArchType API.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class PortCoverageValidatorTest {

    private static final String PORT_PACKAGE = "com.example.domain.port";
    private static final String INFRA_PACKAGE = "com.example.infrastructure";
    private static final String APP_PACKAGE = "com.example.application";

    private PortCoverageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortCoverageValidator();
    }

    @Test
    @DisplayName("Should pass when port has adapter implementation")
    void shouldPass_whenPortHasAdapterImplementation() {
        // Given - Port with adapter that IMPLEMENTS it (not just depends)
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .addImplements(INFRA_PACKAGE + ".OrderServiceAdapter", PORT_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when port has no implementation")
    void shouldFail_whenPortHasNoImplementation() {
        // Given - Port without any adapter
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).constraintId().value()).isEqualTo("hexagonal:port-coverage");
        assertThat(violations.get(0).severity()).isEqualTo(Severity.MAJOR);
        assertThat(violations.get(0).message())
                .contains("OrderService")
                .contains("no adapter implementation")
                .startsWith("Driving");
        assertThat(violations.get(0).affectedTypes()).containsExactly(PORT_PACKAGE + ".OrderService");
    }

    @Test
    @DisplayName("Should check all ports")
    void shouldCheckAllPorts() {
        // Given - Multiple ports, some with IMPLEMENTS relationships, some without
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .addDrivingPort(PORT_PACKAGE + ".PaymentGateway")
                .addDrivingPort(PORT_PACKAGE + ".NotificationService")
                .addImplements(INFRA_PACKAGE + ".OrderServiceAdapter", PORT_PACKAGE + ".OrderService")
                .addImplements(INFRA_PACKAGE + ".NotificationServiceAdapter", PORT_PACKAGE + ".NotificationService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - Only PaymentGateway should have violation
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("PaymentGateway");
    }

    @Test
    @DisplayName("Should check driven ports as well")
    void shouldCheckDrivenPorts() {
        // Given - Driven port without implementation
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message())
                .contains("OrderRepository")
                .contains("no adapter implementation")
                .startsWith("Driven");
    }

    @Test
    @DisplayName("Should pass when no ports exist")
    void shouldPass_whenNoPorts() {
        // Given - Empty model
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass when port has multiple adapter implementations")
    void shouldPass_whenPortHasMultipleAdapterImplementations() {
        // Given - Port with multiple adapters via IMPLEMENTS relationships
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addImplements(INFRA_PACKAGE + ".JpaOrderRepositoryAdapter", PORT_PACKAGE + ".OrderRepository")
                .addImplements(INFRA_PACKAGE + ".MongoOrderRepositoryAdapter", PORT_PACKAGE + ".OrderRepository")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when no adapters exist at all")
    void shouldFail_whenNoAdaptersExist() {
        // Given - Ports without any adapters in the system
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".PaymentGateway", DrivenPortType.GATEWAY)
                .addApplicationService(APP_PACKAGE + ".SomeApplicationService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - Both ports should have violations
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(v -> v.message()).allMatch(msg -> msg.contains("no adapter implementation"));
    }

    @Test
    @DisplayName("Should differentiate driving and driven port violation messages")
    void shouldDifferentiateDrivingAndDrivenPortViolationMessages() {
        // Given - One driving port and one driven port, both without adapters
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then - Messages should clearly indicate port direction
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(Violation::message)
                .anySatisfy(msg -> assertThat(msg).startsWith("Driving port"))
                .anySatisfy(msg -> assertThat(msg).startsWith("Driven port"));
    }

    @Test
    @DisplayName("Should provide relationship evidence")
    void shouldProvideRelationshipEvidence() {
        // Given
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
    @DisplayName("Should pass when adapter implements port")
    void shouldPass_whenAdapterImplementsPort() {
        // Given - Realistic scenario with adapter IMPLEMENTING port (not just depending)
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".CustomerRepository", DrivenPortType.REPOSITORY)
                .addImplements(INFRA_PACKAGE + ".JpaCustomerRepository", PORT_PACKAGE + ".CustomerRepository")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty codebase")
    void shouldHandleEmptyCodebase() {
        // Given
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("Adapter detection via CompositionIndex")
    class AdapterDetectionViaCompositionIndex {

        @Test
        @DisplayName("Should pass when port has adapter via IMPLEMENTS relationship in graph")
        void shouldPass_whenPortHasAdapterViaImplementsRelationship() {
            // Given - Port with IMPLEMENTS relationship in CompositionIndex, empty Codebase
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                    .addImplements(INFRA_PACKAGE + ".JpaOrderRepository", PORT_PACKAGE + ".OrderRepository")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            // When
            List<Violation> violations = validator.validate(model, codebase, null);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass when port has adapter via IMPLEMENTS in graph")
        void shouldPass_whenPortHasAdapterViaImplementsInGraph() {
            // Given - Adapter implements port via IMPLEMENTS relationship
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort(PORT_PACKAGE + ".OrderService")
                    .addImplements(INFRA_PACKAGE + ".OrderRestController", PORT_PACKAGE + ".OrderService")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            // When
            List<Violation> violations = validator.validate(model, codebase, null);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail when graph has no IMPLEMENTS for port")
        void shouldFail_whenGraphHasNoImplementsForPort() {
            // Given - CompositionIndex exists but no IMPLEMENTS for this port
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort(PORT_PACKAGE + ".OrderService")
                    .addDrivenPort(PORT_PACKAGE + ".PaymentGateway", DrivenPortType.GATEWAY)
                    .addImplements(INFRA_PACKAGE + ".OrderRestController", PORT_PACKAGE + ".OrderService")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            // When
            List<Violation> violations = validator.validate(model, codebase, null);

            // Then - Only PaymentGateway should have violation (OrderService is covered via graph)
            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).message()).contains("PaymentGateway");
        }
    }
}
