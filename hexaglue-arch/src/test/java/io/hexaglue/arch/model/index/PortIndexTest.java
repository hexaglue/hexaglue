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

package io.hexaglue.arch.model.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortIndex}.
 *
 * @since 4.1.0
 */
@DisplayName("PortIndex")
class PortIndexTest {

    private static final TypeId ORDER_REPOSITORY_ID = TypeId.of("com.example.OrderRepository");
    private static final TypeId PAYMENT_GATEWAY_ID = TypeId.of("com.example.PaymentGateway");
    private static final TypeId ORDER_USE_CASE_ID = TypeId.of("com.example.PlaceOrderUseCase");
    private static final TypeId ORDER_ID = TypeId.of("com.example.Order");
    private static final TypeId ORDER_SERVICE_ID = TypeId.of("com.example.OrderService");

    private TypeStructure interfaceStructure;

    @BeforeEach
    void setUp() {
        interfaceStructure = TypeStructure.builder(TypeNature.INTERFACE).build();
    }

    private DrivingPort createDrivingPort(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DRIVING_PORT, "test", "Test");
        return new DrivingPort(id, interfaceStructure, trace);
    }

    private DrivenPort createRepository(TypeId id, TypeId aggregateId) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DRIVEN_PORT, "test", "Test");
        return DrivenPort.repository(id, interfaceStructure, trace, TypeRef.of(aggregateId.qualifiedName()));
    }

    private DrivenPort createGateway(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DRIVEN_PORT, "test", "Test");
        return DrivenPort.of(id, interfaceStructure, trace, DrivenPortType.GATEWAY);
    }

    private DrivenPort createEventPublisher(TypeId id) {
        ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DRIVEN_PORT, "test", "Test");
        return DrivenPort.of(id, interfaceStructure, trace, DrivenPortType.EVENT_PUBLISHER);
    }

    @Nested
    @DisplayName("from(TypeRegistry)")
    class FromTypeRegistry {

        @Test
        @DisplayName("should create from empty registry")
        void shouldCreateFromEmptyRegistry() {
            // given
            TypeRegistry registry = TypeRegistry.builder().build();

            // when
            PortIndex index = PortIndex.from(registry);

            // then
            assertThat(index.drivingPorts().count()).isEqualTo(0);
            assertThat(index.drivenPorts().count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create from registry with ports")
        void shouldCreateFromRegistryWithPorts() {
            // given
            DrivingPort useCase = createDrivingPort(ORDER_USE_CASE_ID);
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(useCase).add(repository).build();

            // when
            PortIndex index = PortIndex.from(registry);

            // then
            assertThat(index.drivingPorts().count()).isEqualTo(1);
            assertThat(index.drivenPorts().count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("drivingPorts()")
    class DrivingPorts {

        @Test
        @DisplayName("should return all driving ports")
        void shouldReturnAllDrivingPorts() {
            // given
            DrivingPort useCase = createDrivingPort(ORDER_USE_CASE_ID);
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(useCase).add(repository).build();

            PortIndex index = PortIndex.from(registry);

            // when
            List<DrivingPort> ports = index.drivingPorts().toList();

            // then
            assertThat(ports).containsExactly(useCase);
        }
    }

    @Nested
    @DisplayName("drivenPorts()")
    class DrivenPorts {

        @Test
        @DisplayName("should return all driven ports")
        void shouldReturnAllDrivenPorts() {
            // given
            DrivingPort useCase = createDrivingPort(ORDER_USE_CASE_ID);
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);
            DrivenPort gateway = createGateway(PAYMENT_GATEWAY_ID);

            TypeRegistry registry = TypeRegistry.builder()
                    .add(useCase)
                    .add(repository)
                    .add(gateway)
                    .build();

            PortIndex index = PortIndex.from(registry);

            // when
            List<DrivenPort> ports = index.drivenPorts().toList();

            // then
            assertThat(ports).containsExactlyInAnyOrder(repository, gateway);
        }
    }

    @Nested
    @DisplayName("repositories()")
    class Repositories {

        @Test
        @DisplayName("should return only repository ports")
        void shouldReturnOnlyRepositories() {
            // given
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);
            DrivenPort gateway = createGateway(PAYMENT_GATEWAY_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(repository).add(gateway).build();

            PortIndex index = PortIndex.from(registry);

            // when
            List<DrivenPort> repos = index.repositories().toList();

            // then
            assertThat(repos).containsExactly(repository);
        }
    }

    @Nested
    @DisplayName("gateways()")
    class Gateways {

        @Test
        @DisplayName("should return only gateway ports")
        void shouldReturnOnlyGateways() {
            // given
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);
            DrivenPort gateway = createGateway(PAYMENT_GATEWAY_ID);

            TypeRegistry registry =
                    TypeRegistry.builder().add(repository).add(gateway).build();

            PortIndex index = PortIndex.from(registry);

            // when
            List<DrivenPort> gateways = index.gateways().toList();

            // then
            assertThat(gateways).containsExactly(gateway);
        }
    }

    @Nested
    @DisplayName("repositoryFor(TypeId)")
    class RepositoryFor {

        @Test
        @DisplayName("should return repository for aggregate")
        void shouldReturnRepositoryForAggregate() {
            // given
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);

            TypeRegistry registry = TypeRegistry.builder().add(repository).build();

            PortIndex index = PortIndex.from(registry);

            // when
            Optional<DrivenPort> result = index.repositoryFor(ORDER_ID);

            // then
            assertThat(result).contains(repository);
        }

        @Test
        @DisplayName("should return empty for no matching repository")
        void shouldReturnEmptyForNoMatch() {
            // given
            DrivenPort repository = createRepository(ORDER_REPOSITORY_ID, ORDER_ID);

            TypeRegistry registry = TypeRegistry.builder().add(repository).build();

            PortIndex index = PortIndex.from(registry);

            // when
            Optional<DrivenPort> result = index.repositoryFor(TypeId.of("com.example.NonExistent"));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not match gateway when looking for repository")
        void shouldNotMatchGateway() {
            // given
            DrivenPort gateway = createGateway(PAYMENT_GATEWAY_ID);

            TypeRegistry registry = TypeRegistry.builder().add(gateway).build();

            PortIndex index = PortIndex.from(registry);

            // when
            Optional<DrivenPort> result = index.repositoryFor(ORDER_ID);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("portsImplementedBy(TypeId)")
    class PortsImplementedBy {

        @Test
        @DisplayName("should return driving ports implemented by service")
        void shouldReturnPortsImplementedByService() {
            // given
            List<TypeRef> implementations = List.of(TypeRef.of(ORDER_SERVICE_ID.qualifiedName()));
            ClassificationTrace trace = ClassificationTrace.highConfidence(ElementKind.DRIVING_PORT, "test", "Test");
            TypeStructure structureWithImpls = TypeStructure.builder(TypeNature.INTERFACE)
                    .interfaces(implementations)
                    .build();
            DrivingPort useCase = new DrivingPort(ORDER_USE_CASE_ID, structureWithImpls, trace);

            TypeRegistry registry = TypeRegistry.builder().add(useCase).build();

            PortIndex index = PortIndex.from(registry);

            // when
            List<DrivingPort> ports = index.portsImplementedBy(ORDER_SERVICE_ID);

            // then
            assertThat(ports).containsExactly(useCase);
        }

        @Test
        @DisplayName("should return empty for service without port implementations")
        void shouldReturnEmptyForNoImplementations() {
            // given
            DrivingPort useCase = createDrivingPort(ORDER_USE_CASE_ID);

            TypeRegistry registry = TypeRegistry.builder().add(useCase).build();

            PortIndex index = PortIndex.from(registry);

            // when
            List<DrivingPort> ports = index.portsImplementedBy(ORDER_SERVICE_ID);

            // then
            assertThat(ports).isEmpty();
        }
    }
}
