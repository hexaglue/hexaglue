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

package io.hexaglue.arch.model.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.PortType;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.UseCase.UseCaseType;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PortQuery}.
 *
 * @since 5.0.0
 */
@DisplayName("PortQuery")
class PortQueryTest {

    private static final TypeId ORDER_SERVICE_ID = TypeId.of("com.example.order.OrderService");
    private static final TypeId ORDER_REPO_ID = TypeId.of("com.example.order.OrderRepository");
    private static final TypeId PAYMENT_GATEWAY_ID = TypeId.of("com.example.payment.PaymentGateway");

    private PortIndex portIndex;

    @BeforeEach
    void setUp() {
        TypeStructure interfaceStructure =
                TypeStructure.builder(TypeNature.INTERFACE).build();

        ClassificationTrace drivingTrace = ClassificationTrace.highConfidence(ElementKind.DRIVING_PORT, "test", "Test");
        ClassificationTrace drivenTrace = ClassificationTrace.highConfidence(ElementKind.DRIVEN_PORT, "test", "Test");

        // Create a use case for the driving port
        Method createOrderMethod = Method.of("createOrder", new TypeRef("void", "void", List.of(), false, false, 0));
        UseCase createOrderUseCase = UseCase.of(createOrderMethod, UseCaseType.COMMAND);

        // Driving port with use cases
        DrivingPort orderService = DrivingPort.of(
                ORDER_SERVICE_ID,
                interfaceStructure,
                drivingTrace,
                List.of(createOrderUseCase),
                List.of(TypeRef.of("com.example.order.OrderRequest")),
                List.of());

        // Repository (driven port)
        DrivenPort orderRepository = DrivenPort.repository(
                ORDER_REPO_ID, interfaceStructure, drivenTrace, TypeRef.of("com.example.order.Order"));

        // Gateway (driven port)
        DrivenPort paymentGateway =
                DrivenPort.of(PAYMENT_GATEWAY_ID, interfaceStructure, drivenTrace, DrivenPortType.GATEWAY);

        TypeRegistry registry = TypeRegistry.builder()
                .add(orderService)
                .add(orderRepository)
                .add(paymentGateway)
                .build();

        portIndex = PortIndex.from(registry);
    }

    @Nested
    @DisplayName("Driving Ports Query")
    class DrivingPortsQuery {

        @Test
        @DisplayName("drivingPorts() should return all driving ports")
        void drivingPortsShouldReturnAllDrivingPorts() {
            // when
            PortQuery query = PortQuery.drivingPorts(portIndex);
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("withUseCases() should filter ports with use cases")
        void withUseCasesShouldFilterPortsWithUseCases() {
            // when
            PortQuery query = PortQuery.drivingPorts(portIndex).withUseCases();
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Driven Ports Query")
    class DrivenPortsQuery {

        @Test
        @DisplayName("drivenPorts() should return all driven ports")
        void drivenPortsShouldReturnAllDrivenPorts() {
            // when
            PortQuery query = PortQuery.drivenPorts(portIndex);
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("repositories() should filter only repositories")
        void repositoriesShouldFilterOnlyRepositories() {
            // when
            PortQuery query = PortQuery.drivenPorts(portIndex).repositories();
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("gateways() should filter only gateways")
        void gatewaysShouldFilterOnlyGateways() {
            // when
            PortQuery query = PortQuery.drivenPorts(portIndex).gateways();
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("All Ports Query")
    class AllPortsQuery {

        @Test
        @DisplayName("allPorts() should return all ports")
        void allPortsShouldReturnAllPorts() {
            // when
            PortQuery query = PortQuery.allPorts(portIndex);
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("inPackage() should filter by exact package match")
        void inPackageShouldFilterByExactPackageMatch() {
            // when
            PortQuery query = PortQuery.allPorts(portIndex).inPackage("com.example.order");
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(2); // OrderService and OrderRepository
        }

        @Test
        @DisplayName("inPackageTree() should filter by package prefix")
        void inPackageTreeShouldFilterByPackagePrefix() {
            // when
            PortQuery query = PortQuery.allPorts(portIndex).inPackageTree("com.example");
            List<PortType> result = query.toList();

            // then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("count() should return port count")
        void countShouldReturnPortCount() {
            // when
            long count = PortQuery.allPorts(portIndex).count();

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("exists() should return true when ports exist")
        void existsShouldReturnTrueWhenPortsExist() {
            // then
            assertThat(PortQuery.allPorts(portIndex).exists()).isTrue();
        }

        @Test
        @DisplayName("isEmpty() should return false when ports exist")
        void isEmptyShouldReturnFalseWhenPortsExist() {
            // then
            assertThat(PortQuery.allPorts(portIndex).isEmpty()).isFalse();
        }

        @Test
        @DisplayName("filters should be immutable")
        void filtersShouldBeImmutable() {
            // given
            PortQuery original = PortQuery.allPorts(portIndex);
            PortQuery filtered = original.repositories();

            // then - original should be unchanged
            assertThat(original.toList()).hasSize(3);
            assertThat(filtered.toList()).hasSize(1);
        }
    }
}
