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

package io.hexaglue.arch.ports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ElementRef;
import io.hexaglue.arch.ElementRegistry;
import io.hexaglue.arch.domain.Aggregate;
import io.hexaglue.arch.domain.DomainEntity;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Port Types")
class PortTypesTest {

    private static final String PKG = "com.example.ports";

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("PortClassification")
    class PortClassificationTest {

        @Test
        @DisplayName("should identify driving classifications")
        void shouldIdentifyDriving() {
            assertThat(PortClassification.USE_CASE.isDriving()).isTrue();
            assertThat(PortClassification.COMMAND_HANDLER.isDriving()).isTrue();
            assertThat(PortClassification.QUERY_HANDLER.isDriving()).isTrue();
            assertThat(PortClassification.REPOSITORY.isDriving()).isFalse();
        }

        @Test
        @DisplayName("should identify driven classifications")
        void shouldIdentifyDriven() {
            assertThat(PortClassification.REPOSITORY.isDriven()).isTrue();
            assertThat(PortClassification.GATEWAY.isDriven()).isTrue();
            assertThat(PortClassification.EVENT_PUBLISHER.isDriven()).isTrue();
            assertThat(PortClassification.USE_CASE.isDriven()).isFalse();
        }
    }

    @Nested
    @DisplayName("PortOperation")
    class PortOperationTest {

        @Test
        @DisplayName("should create operation with return type")
        void shouldCreateWithReturnType() {
            // when
            PortOperation op = new PortOperation(
                    "findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of(PKG + ".OrderId")), null);

            // then
            assertThat(op.name()).isEqualTo("findById");
            assertThat(op.isVoid()).isFalse();
            assertThat(op.parameterTypes()).hasSize(1);
        }

        @Test
        @DisplayName("should identify void operation")
        void shouldIdentifyVoid() {
            // when
            PortOperation op = PortOperation.of("save");

            // then
            assertThat(op.isVoid()).isTrue();
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new PortOperation("  ", null, List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("DrivingPort")
    class DrivingPortTest {

        @Test
        @DisplayName("should create use case port")
        void shouldCreateUseCase() {
            // when
            DrivingPort port = new DrivingPort(
                    ElementId.of(PKG + ".PlaceOrderUseCase"),
                    PortClassification.USE_CASE,
                    List.of(PortOperation.of("execute")),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            // then
            assertThat(port.kind()).isEqualTo(ElementKind.DRIVING_PORT);
            assertThat(port.isUseCase()).isTrue();
            assertThat(port.isCommandHandler()).isFalse();
            assertThat(port.operations()).hasSize(1);
        }

        @Test
        @DisplayName("should create command handler port")
        void shouldCreateCommandHandler() {
            // when
            DrivingPort port = new DrivingPort(
                    ElementId.of(PKG + ".CreateOrderCommandHandler"),
                    PortClassification.COMMAND_HANDLER,
                    List.of(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            // then
            assertThat(port.isCommandHandler()).isTrue();
            assertThat(port.isQueryHandler()).isFalse();
        }

        @Test
        @DisplayName("should use factory method")
        void shouldUseFactory() {
            // when
            DrivingPort port = DrivingPort.of(PKG + ".TestUseCase", highConfidence(ElementKind.DRIVING_PORT));

            // then
            assertThat(port.classification()).isEqualTo(PortClassification.USE_CASE);
            assertThat(port.operations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DrivenPort")
    class DrivenPortTest {

        @Test
        @DisplayName("should create repository port")
        void shouldCreateRepository() {
            // when
            DrivenPort port = new DrivenPort(
                    ElementId.of(PKG + ".OrderRepository"),
                    PortClassification.REPOSITORY,
                    List.of(PortOperation.of("findById"), PortOperation.of("save")),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_PORT));

            // then
            assertThat(port.kind()).isEqualTo(ElementKind.DRIVEN_PORT);
            assertThat(port.isRepository()).isTrue();
            assertThat(port.isGateway()).isFalse();
            assertThat(port.operations()).hasSize(2);
        }

        @Test
        @DisplayName("should create gateway port")
        void shouldCreateGateway() {
            // when
            DrivenPort port = new DrivenPort(
                    ElementId.of(PKG + ".PaymentGateway"),
                    PortClassification.GATEWAY,
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_PORT));

            // then
            assertThat(port.isGateway()).isTrue();
            assertThat(port.isRepository()).isFalse();
        }

        @Test
        @DisplayName("should resolve primary aggregate")
        void shouldResolvePrimaryAggregate() {
            // given
            DomainEntity root =
                    DomainEntity.aggregateRoot("com.example.Order", highConfidence(ElementKind.AGGREGATE_ROOT));
            Aggregate agg = Aggregate.of(
                    "com.example.OrderAggregate",
                    ElementRef.of(root.id(), DomainEntity.class),
                    highConfidence(ElementKind.AGGREGATE));
            ElementRef<Aggregate> aggRef = ElementRef.of(agg.id(), Aggregate.class);

            DrivenPort port =
                    DrivenPort.repository(PKG + ".OrderRepository", aggRef, highConfidence(ElementKind.DRIVEN_PORT));

            ElementRegistry registry =
                    ElementRegistry.builder().add(root).add(agg).add(port).build();

            // when
            Optional<Aggregate> resolved = port.resolvePrimaryAggregate(registry);

            // then
            assertThat(resolved).isPresent();
            assertThat(resolved.get()).isEqualTo(agg);
        }

        @Test
        @DisplayName("should use factory method")
        void shouldUseFactory() {
            // when
            DrivenPort port = DrivenPort.of(PKG + ".TestRepository", highConfidence(ElementKind.DRIVEN_PORT));

            // then
            assertThat(port.classification()).isEqualTo(PortClassification.REPOSITORY);
            assertThat(port.primaryManagedType()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ApplicationService")
    class ApplicationServiceTest {

        @Test
        @DisplayName("should create application service")
        void shouldCreate() {
            // when
            ApplicationService service = new ApplicationService(
                    ElementId.of(PKG + ".OrderApplicationService"),
                    List.of(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.APPLICATION_SERVICE));

            // then
            assertThat(service.kind()).isEqualTo(ElementKind.APPLICATION_SERVICE);
            assertThat(service.dependencyCount()).isZero();
            assertThat(service.implementsDrivingPorts()).isFalse();
        }

        @Test
        @DisplayName("should track dependencies")
        void shouldTrackDependencies() {
            // given
            ElementRef<DrivenPort> repo = ElementRef.of(ElementId.of(PKG + ".OrderRepository"), DrivenPort.class);
            ElementRef<DrivenPort> gateway = ElementRef.of(ElementId.of(PKG + ".PaymentGateway"), DrivenPort.class);

            // when
            ApplicationService service = new ApplicationService(
                    ElementId.of(PKG + ".OrderApplicationService"),
                    List.of(),
                    List.of(repo, gateway),
                    null,
                    highConfidence(ElementKind.APPLICATION_SERVICE));

            // then
            assertThat(service.dependencyCount()).isEqualTo(2);
            assertThat(service.drivenPortDependencies()).hasSize(2);
        }

        @Test
        @DisplayName("should track implemented ports")
        void shouldTrackImplementedPorts() {
            // given
            ElementRef<DrivingPort> useCase =
                    ElementRef.of(ElementId.of(PKG + ".PlaceOrderUseCase"), DrivingPort.class);

            // when
            ApplicationService service = new ApplicationService(
                    ElementId.of(PKG + ".OrderApplicationService"),
                    List.of(useCase),
                    List.of(),
                    null,
                    highConfidence(ElementKind.APPLICATION_SERVICE));

            // then
            assertThat(service.implementsDrivingPorts()).isTrue();
            assertThat(service.implementedPorts()).hasSize(1);
        }

        @Test
        @DisplayName("should use factory method")
        void shouldUseFactory() {
            // when
            ApplicationService service =
                    ApplicationService.of(PKG + ".TestService", highConfidence(ElementKind.APPLICATION_SERVICE));

            // then
            assertThat(service.implementedPorts()).isEmpty();
            assertThat(service.drivenPortDependencies()).isEmpty();
        }
    }
}
