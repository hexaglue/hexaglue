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

package io.hexaglue.plugin.livingdoc.content;

import static io.hexaglue.plugin.livingdoc.V5TestModelBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Method;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PortContentSelector using v5 ArchitecturalModel API.
 *
 * @since 4.0.0
 * @since 5.0.0 - Migrated to v5 ArchType API
 */
@DisplayName("PortContentSelector")
class PortContentSelectorTest {

    private static final String PKG = "com.example.ports";

    @Nested
    @DisplayName("Driving Port Selection")
    class DrivingPortSelection {

        @Test
        @DisplayName("should select driving ports")
        void shouldSelectDrivingPorts() {
            // Given
            Method placeOrder = method(
                    "placeOrder", TypeRef.of("com.example.domain.OrderId"), List.of(TypeRef.of("com.example.OrderRequest")));

            DrivingPort orderUseCase = drivingPort(PKG + ".in.OrderingProducts", List.of(placeOrder));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderUseCase);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();

            // Then
            assertThat(results).hasSize(1);
            PortDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderingProducts");
            assertThat(doc.packageName()).isEqualTo(PKG + ".in");
            assertThat(doc.kind()).isEqualTo(PortKind.USE_CASE);
            assertThat(doc.direction()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        @DisplayName("should transform methods correctly")
        void shouldTransformMethodsCorrectly() {
            // Given
            Method placeOrder = method(
                    "placeOrder", TypeRef.of("com.example.domain.OrderId"), List.of(TypeRef.of("com.example.OrderRequest")));
            Method cancelOrder =
                    voidMethod("cancelOrder", List.of(TypeRef.of("com.example.domain.OrderId")));

            DrivingPort orderUseCase = drivingPort(PKG + ".in.OrderingProducts", List.of(placeOrder, cancelOrder));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderUseCase);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();
            PortDoc doc = results.get(0);

            // Then
            assertThat(doc.methods()).hasSize(2);

            MethodDoc method1 = doc.methods().get(0);
            assertThat(method1.name()).isEqualTo("placeOrder");
            assertThat(method1.returnType()).isEqualTo("com.example.domain.OrderId");
            assertThat(method1.parameters()).containsExactly("com.example.OrderRequest");

            MethodDoc method2 = doc.methods().get(1);
            assertThat(method2.name()).isEqualTo("cancelOrder");
            assertThat(method2.returnType()).isEqualTo("void");
        }
    }

    @Nested
    @DisplayName("Driven Port Selection")
    class DrivenPortSelection {

        @Test
        @DisplayName("should select driven ports")
        void shouldSelectDrivenPorts() {
            // Given
            Method findById = method(
                    "findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of("com.example.domain.OrderId")));
            Method save = method(
                    "save", TypeRef.of("com.example.domain.Order"), List.of(TypeRef.of("com.example.domain.Order")));

            DrivenPort orderRepository =
                    drivenPort(PKG + ".out.OrderRepository", DrivenPortType.REPOSITORY, List.of(findById, save));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderRepository);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();

            // Then
            assertThat(results).hasSize(1);
            PortDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderRepository");
            assertThat(doc.packageName()).isEqualTo(PKG + ".out");
            assertThat(doc.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(doc.direction()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("should transform repository methods")
        void shouldTransformRepositoryMethods() {
            // Given
            Method findById =
                    method("findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of("com.example.OrderId")));
            Method save = method("save", TypeRef.of("com.example.Order"), List.of(TypeRef.of("com.example.Order")));
            Method delete = voidMethod("delete", List.of(TypeRef.of("com.example.OrderId")));

            DrivenPort orderRepository =
                    drivenPort(PKG + ".out.OrderRepository", DrivenPortType.REPOSITORY, List.of(findById, save, delete));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), orderRepository);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();
            PortDoc doc = results.get(0);

            // Then
            assertThat(doc.methods()).hasSize(3);
            assertThat(doc.methods()).extracting("name").containsExactly("findById", "save", "delete");
        }
    }

    @Nested
    @DisplayName("Method Transformation")
    class MethodTransformation {

        @Test
        @DisplayName("should transform method with no parameters")
        void shouldTransformMethodWithNoParameters() {
            // Given
            Method getAll = method("getAll", TypeRef.of("java.util.List"));

            DrivingPort port = drivingPort(PKG + ".in.OrderQuery", List.of(getAll));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), port);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();
            MethodDoc methodDoc = results.get(0).methods().get(0);

            // Then
            assertThat(methodDoc.name()).isEqualTo("getAll");
            assertThat(methodDoc.returnType()).isEqualTo("java.util.List");
            assertThat(methodDoc.parameters()).isEmpty();
        }

        @Test
        @DisplayName("should transform void method")
        void shouldTransformVoidMethod() {
            // Given: void method (null return type)
            Method notify = voidMethod("notifyCustomer", List.of(TypeRef.of("java.lang.String")));

            DrivenPort port = drivenPort(PKG + ".out.NotificationGateway", DrivenPortType.GATEWAY, List.of(notify));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), port);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();
            MethodDoc methodDoc = results.get(0).methods().get(0);

            // Then
            assertThat(methodDoc.returnType()).isEqualTo("void");
        }

        @Test
        @DisplayName("should preserve parameter order")
        void shouldPreserveParameterOrder() {
            // Given
            Method create = method(
                    "createOrder",
                    TypeRef.of("OrderId"),
                    List.of(
                            TypeRef.of("CustomerId"),
                            TypeRef.of("List<OrderLineItem>"),
                            TypeRef.of("Address"),
                            TypeRef.of("Money")));

            DrivingPort port = drivingPort(PKG + ".in.OrderCreation", List.of(create));

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), port);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();
            MethodDoc methodDoc = results.get(0).methods().get(0);

            // Then
            assertThat(methodDoc.parameters()).containsExactly("CustomerId", "List<OrderLineItem>", "Address", "Money");
        }
    }

    @Nested
    @DisplayName("Mixed Port Selection")
    class MixedPortSelection {

        @Test
        @DisplayName("should select only driving ports")
        void shouldSelectOnlyDrivingPorts() {
            // Given
            DrivingPort drivingPort1 = drivingPort(PKG + ".in.UseCase1");
            DrivingPort drivingPort2 = drivingPort(PKG + ".in.UseCase2");
            DrivenPort drivenPort1 = drivenPort(PKG + ".out.Repository1", DrivenPortType.REPOSITORY);
            DrivenPort drivenPort2 = drivenPort(PKG + ".out.Gateway1", DrivenPortType.GATEWAY);

            ArchitecturalModel model =
                    createModel(ProjectContext.forTesting("app", PKG), drivingPort1, drivingPort2, drivenPort1, drivenPort2);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting("name").containsExactlyInAnyOrder("UseCase1", "UseCase2");
            assertThat(results).allMatch(doc -> doc.direction() == PortDirection.DRIVING);
        }

        @Test
        @DisplayName("should select only driven ports")
        void shouldSelectOnlyDrivenPorts() {
            // Given
            DrivingPort drivingPort1 = drivingPort(PKG + ".in.UseCase1");
            DrivenPort drivenPort1 = drivenPort(PKG + ".out.Repository1", DrivenPortType.REPOSITORY);
            DrivenPort drivenPort2 = drivenPort(PKG + ".out.Gateway1", DrivenPortType.GATEWAY);

            ArchitecturalModel model =
                    createModel(ProjectContext.forTesting("app", PKG), drivingPort1, drivenPort1, drivenPort2);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting("name").containsExactlyInAnyOrder("Repository1", "Gateway1");
            assertThat(results).allMatch(doc -> doc.direction() == PortDirection.DRIVEN);
        }
    }

    @Nested
    @DisplayName("Empty Selections")
    class EmptySelections {

        @Test
        @DisplayName("should return empty list when no driving ports")
        void shouldReturnEmptyListWhenNoDrivingPorts() {
            // Given
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG));

            PortContentSelector selector = new PortContentSelector(model);

            // Then
            assertThat(selector.selectDrivingPorts()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no driven ports")
        void shouldReturnEmptyListWhenNoDrivenPorts() {
            // Given
            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG));

            PortContentSelector selector = new PortContentSelector(model);

            // Then
            assertThat(selector.selectDrivenPorts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Port Classification Variations")
    class PortClassificationVariations {

        @Test
        @DisplayName("should handle event publisher")
        void shouldHandleEventPublisher() {
            // Given
            DrivenPort eventPort = drivenPort(PKG + ".out.OrderEventPublisher", DrivenPortType.EVENT_PUBLISHER);

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), eventPort);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();

            // Then
            assertThat(results.get(0).kind()).isEqualTo(PortKind.EVENT_PUBLISHER);
        }

        @Test
        @DisplayName("should handle gateway")
        void shouldHandleGateway() {
            // Given
            DrivenPort gatewayPort = drivenPort(PKG + ".out.ExternalService", DrivenPortType.GATEWAY);

            ArchitecturalModel model = createModel(ProjectContext.forTesting("app", PKG), gatewayPort);

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();

            // Then
            assertThat(results.get(0).kind()).isEqualTo(PortKind.GATEWAY);
        }
    }
}
