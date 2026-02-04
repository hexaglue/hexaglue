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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.util.TestCodebaseBuilder;
import io.hexaglue.plugin.audit.util.TestModelBuilder;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortDirectionValidator}.
 *
 * <p>Validates that port direction checking works correctly using the v5 ArchType API.
 * DRIVEN ports should be used by application services, and DRIVING ports should be
 * implemented by application services.
 *
 * @since 5.0.0 Migrated to v5 ArchType API
 */
class PortDirectionValidatorTest {

    private static final String PORT_PACKAGE = "com.example.domain.port";
    private static final String APP_PACKAGE = "com.example.application";

    private PortDirectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PortDirectionValidator();
    }

    @Test
    @DisplayName("Should pass when DRIVEN port is used by application service")
    void shouldPass_whenDrivenPortIsUsedByApplicationService() {
        // Given: A repository port (DRIVEN) and an application service that uses it
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addApplicationService(APP_PACKAGE + ".OrderService")
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
    @DisplayName("Should fail when DRIVEN port is not used by any application service")
    void shouldFail_whenDrivenPortIsNotUsed() {
        // Given: A repository port (DRIVEN) with no application service using it
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addApplicationService(APP_PACKAGE + ".OrderService") // Exists but doesn't use the repository
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .addApplicationService(APP_PACKAGE + ".OrderServiceImpl")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".OrderServiceImpl", PORT_PACKAGE + ".OrderService")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail when DRIVING port is not implemented by any application service")
    void shouldFail_whenDrivingPortIsNotImplemented() {
        // Given: A service port (DRIVING) with an application service that doesn't implement it
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .addApplicationService(APP_PACKAGE + ".ProductManager") // Exists but doesn't implement OrderService
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
        // Given: Various DRIVEN port types with an app service that doesn't use any of them
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".PaymentGateway", DrivenPortType.GATEWAY)
                .addDrivenPort(PORT_PACKAGE + ".EmailClient", DrivenPortType.OTHER)
                .addDrivenPort(PORT_PACKAGE + ".EventPublisher", DrivenPortType.EVENT_PUBLISHER)
                .addDrivenPort(PORT_PACKAGE + ".ProductStore", DrivenPortType.REPOSITORY)
                .addApplicationService(APP_PACKAGE + ".SomeService") // Present but doesn't use any port
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: All should be flagged (no app service uses them)
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .extracting(v -> v.message())
                .allMatch(msg -> msg.contains("DRIVEN port") && msg.contains("not used by"));
    }

    @Test
    @DisplayName("Should validate multiple DRIVING ports correctly")
    void shouldValidateMultipleDrivingPorts() {
        // Given: Various DRIVING port types with an app service that doesn't implement any of them
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivingPort(PORT_PACKAGE + ".PlaceOrderUseCase")
                .addDrivingPort(PORT_PACKAGE + ".OrderFacade")
                .addDrivingPort(PORT_PACKAGE + ".PaymentHandler")
                .addApplicationService(APP_PACKAGE + ".SomeService") // Present but doesn't implement any port
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: All should be flagged (no app service implements them)
        assertThat(violations).hasSize(3);
        assertThat(violations)
                .extracting(v -> v.message())
                .allMatch(msg -> msg.contains("DRIVING port") && msg.contains("not implemented by"));
    }

    @Test
    @DisplayName("Should validate multiple ports correctly")
    void shouldValidateMultiplePorts() {
        // Given: Mix of valid and invalid ports
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY) // Not used - INVALID
                .addDrivingPort(PORT_PACKAGE + ".OrderService") // Not implemented - INVALID
                .addDrivenPort(PORT_PACKAGE + ".ProductRepository", DrivenPortType.REPOSITORY) // Used - VALID
                .addDrivingPort(PORT_PACKAGE + ".ProductService") // Implemented - VALID
                .addApplicationService(APP_PACKAGE + ".ProductManager")
                .build();
        Codebase codebase = new TestCodebaseBuilder()
                .addDependency(APP_PACKAGE + ".ProductManager", PORT_PACKAGE + ".ProductRepository")
                .addDependency(APP_PACKAGE + ".ProductManager", PORT_PACKAGE + ".ProductService")
                .build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: Two violations (OrderRepository and OrderService)
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .flatExtracting(v -> v.affectedTypes())
                .containsExactlyInAnyOrder(PORT_PACKAGE + ".OrderRepository", PORT_PACKAGE + ".OrderService");
    }

    @Test
    @DisplayName("Should provide dependency evidence")
    void shouldProvideDependencyEvidence() {
        // Given: A driven port with an app service that doesn't use it
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addApplicationService(APP_PACKAGE + ".SomeService") // Present but doesn't use the port
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

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
    @DisplayName("Should pass when model has no port index")
    void shouldPass_whenNoPortIndex() {
        // Given - empty model has no port index
        ArchitecturalModel model = TestModelBuilder.emptyModel();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should skip validation when no application services and no discovery mechanism")
    void shouldSkipValidation_whenNoApplicationServicesAndNoDiscoveryMechanism() {
        // Given: Ports exist but no application services, no query, no compositionIndex
        ArchitecturalModel model = new TestModelBuilder()
                .addDrivenPort(PORT_PACKAGE + ".OrderRepository", DrivenPortType.REPOSITORY)
                .addDrivenPort(PORT_PACKAGE + ".PaymentGateway", DrivenPortType.GATEWAY)
                .addDrivingPort(PORT_PACKAGE + ".OrderService")
                .build();
        Codebase codebase = new TestCodebaseBuilder().build();

        // When: query is null and no compositionIndex
        List<Violation> violations = validator.validate(model, codebase, null);

        // Then: No violations — cannot validate without any discovery mechanism
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

    @Nested
    @DisplayName("CompositionIndex strategy")
    class CompositionIndexStrategyTests {

        @Test
        @DisplayName("Should pass when driving port is found via CompositionIndex IMPLEMENTS")
        void shouldPass_whenDrivingPortFoundViaCompositionIndex() {
            // Given: App service implements driving port, discoverable via CompositionIndex
            String portQName = PORT_PACKAGE + ".OrderService";
            String implQName = APP_PACKAGE + ".OrderServiceImpl";
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort(portQName)
                    .addApplicationService(implQName)
                    .addImplements(implQName, portQName)
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            // When: No dependency in codebase, but IMPLEMENTS relationship exists
            List<Violation> violations = validator.validate(model, codebase, null);

            // Then: No violations — found via CompositionIndex
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass when driven port is found via CompositionIndex IMPLEMENTS")
        void shouldPass_whenDrivenPortFoundViaCompositionIndex() {
            // Given: Adapter implements driven port, discoverable via CompositionIndex
            String portQName = PORT_PACKAGE + ".OrderRepository";
            String implQName = "com.example.infrastructure.JpaOrderRepository";
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivenPort(portQName, DrivenPortType.REPOSITORY)
                    .addApplicationService(APP_PACKAGE + ".SomeService")
                    .addImplements(implQName, portQName)
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            // When
            List<Violation> violations = validator.validate(model, codebase, null);

            // Then: No violations — found via CompositionIndex
            assertThat(violations)
                    .filteredOn(v -> v.affectedTypes().contains(portQName))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("ArchitectureQuery strategy")
    class ArchitectureQueryStrategyTests {

        @Test
        @DisplayName("Should pass when driving port implementor found via ArchitectureQuery")
        void shouldPass_whenDrivingPortFoundViaQuery() {
            // Given: App service implements driving port, only discoverable via query
            String portQName = PORT_PACKAGE + ".OrderService";
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort(portQName)
                    .addApplicationService(APP_PACKAGE + ".SomeService")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors(portQName)).thenReturn(List.of(APP_PACKAGE + ".OrderServiceImpl"));

            // When
            List<Violation> violations = validator.validate(model, codebase, query);

            // Then: No violations — found via ArchitectureQuery
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass when driven port implementor found via ArchitectureQuery")
        void shouldPass_whenDrivenPortFoundViaQuery() {
            // Given: Adapter implements driven port, only discoverable via query
            String portQName = PORT_PACKAGE + ".OrderRepository";
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivenPort(portQName, DrivenPortType.REPOSITORY)
                    .addApplicationService(APP_PACKAGE + ".SomeService")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors(portQName))
                    .thenReturn(List.of("com.example.infrastructure.JpaOrderRepository"));

            // When
            List<Violation> violations = validator.validate(model, codebase, query);

            // Then: No violations — found via ArchitectureQuery
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail when query returns empty implementors")
        void shouldFail_whenQueryReturnsEmptyImplementors() {
            // Given: No app service implements the ports, query returns empty
            String portQName = PORT_PACKAGE + ".OrderService";
            ArchitecturalModel model = new TestModelBuilder()
                    .addDrivingPort(portQName)
                    .addApplicationService(APP_PACKAGE + ".SomeService")
                    .build();
            Codebase codebase = new TestCodebaseBuilder().build();

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors(portQName)).thenReturn(List.of());

            // When
            List<Violation> violations = validator.validate(model, codebase, query);

            // Then: Violation reported — no implementor found anywhere
            assertThat(violations).hasSize(1);
            assertThat(violations.get(0).message()).contains("DRIVING port").contains("not implemented");
        }

        @Test
        @DisplayName("Should validate ports when no app services but query is available")
        void shouldValidatePorts_whenNoAppServicesButQueryAvailable() {
            // Given: Ports exist, no application services, but query is available
            String drivingPortQName = PORT_PACKAGE + ".OrderService";
            ArchitecturalModel model =
                    new TestModelBuilder().addDrivingPort(drivingPortQName).build();
            Codebase codebase = new TestCodebaseBuilder().build();

            ArchitectureQuery query = mock(ArchitectureQuery.class);
            when(query.findImplementors(drivingPortQName)).thenReturn(List.of(APP_PACKAGE + ".OrderServiceImpl"));

            // When: No app services in registry, but query can find implementors
            List<Violation> violations = validator.validate(model, codebase, query);

            // Then: No violations — found via query despite no app services in registry
            assertThat(violations).isEmpty();
        }
    }
}
