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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementId;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.ProjectContext;
import io.hexaglue.arch.ports.DrivenPort;
import io.hexaglue.arch.ports.DrivingPort;
import io.hexaglue.arch.ports.PortClassification;
import io.hexaglue.arch.ports.PortOperation;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PortContentSelector using v4 ArchitecturalModel API.
 *
 * @since 4.0.0
 */
@DisplayName("PortContentSelector")
class PortContentSelectorTest {

    private static final String PKG = "com.example.ports";

    private ClassificationTrace highConfidence(ElementKind kind) {
        return ClassificationTrace.highConfidence(kind, "test", "Test classification");
    }

    @Nested
    @DisplayName("Driving Port Selection")
    class DrivingPortSelection {

        @Test
        @DisplayName("should select driving ports")
        void shouldSelectDrivingPorts() {
            // Given
            PortOperation placeOrder = new PortOperation(
                    "placeOrder",
                    TypeRef.of("com.example.domain.OrderId"),
                    List.of(TypeRef.of("com.example.OrderRequest")),
                    null);

            DrivingPort orderUseCase = new DrivingPort(
                    ElementId.of(PKG + ".in.OrderingProducts"),
                    PortClassification.USE_CASE,
                    List.of(placeOrder),
                    List.of(), // implementedBy
                    null, // syntax
                    highConfidence(ElementKind.DRIVING_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderUseCase)
                    .build();

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
            PortOperation placeOrder = new PortOperation(
                    "placeOrder",
                    TypeRef.of("com.example.domain.OrderId"),
                    List.of(TypeRef.of("com.example.OrderRequest")),
                    null);
            PortOperation cancelOrder =
                    new PortOperation("cancelOrder", null, List.of(TypeRef.of("com.example.domain.OrderId")), null);

            DrivingPort orderUseCase = new DrivingPort(
                    ElementId.of(PKG + ".in.OrderingProducts"),
                    PortClassification.USE_CASE,
                    List.of(placeOrder, cancelOrder),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderUseCase)
                    .build();

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
            PortOperation findById = new PortOperation(
                    "findById",
                    TypeRef.of("java.util.Optional"),
                    List.of(TypeRef.of("com.example.domain.OrderId")),
                    null);
            PortOperation save = new PortOperation(
                    "save",
                    TypeRef.of("com.example.domain.Order"),
                    List.of(TypeRef.of("com.example.domain.Order")),
                    null);

            DrivenPort orderRepository = new DrivenPort(
                    ElementId.of(PKG + ".out.OrderRepository"),
                    PortClassification.REPOSITORY,
                    List.of(findById, save),
                    Optional.empty(), // primaryManagedType
                    List.of(), // managedTypes
                    null, // syntax
                    highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderRepository)
                    .build();

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
            PortOperation findById = new PortOperation(
                    "findById", TypeRef.of("java.util.Optional"), List.of(TypeRef.of("com.example.OrderId")), null);
            PortOperation save = new PortOperation(
                    "save", TypeRef.of("com.example.Order"), List.of(TypeRef.of("com.example.Order")), null);
            PortOperation delete = new PortOperation("delete", null, List.of(TypeRef.of("com.example.OrderId")), null);

            DrivenPort orderRepository = new DrivenPort(
                    ElementId.of(PKG + ".out.OrderRepository"),
                    PortClassification.REPOSITORY,
                    List.of(findById, save, delete),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(orderRepository)
                    .build();

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
            PortOperation getAll = new PortOperation("getAll", TypeRef.of("java.util.List"), List.of(), null);

            DrivingPort port = new DrivingPort(
                    ElementId.of(PKG + ".in.OrderQuery"),
                    PortClassification.QUERY_HANDLER,
                    List.of(getAll),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(port)
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();
            MethodDoc method = results.get(0).methods().get(0);

            // Then
            assertThat(method.name()).isEqualTo("getAll");
            assertThat(method.returnType()).isEqualTo("java.util.List");
            assertThat(method.parameters()).isEmpty();
        }

        @Test
        @DisplayName("should transform void method")
        void shouldTransformVoidMethod() {
            // Given: void method (null return type)
            PortOperation notify =
                    new PortOperation("notifyCustomer", null, List.of(TypeRef.of("java.lang.String")), null);

            DrivenPort port = new DrivenPort(
                    ElementId.of(PKG + ".out.NotificationGateway"),
                    PortClassification.GATEWAY,
                    List.of(notify),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(port)
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();
            MethodDoc method = results.get(0).methods().get(0);

            // Then
            assertThat(method.returnType()).isEqualTo("void");
        }

        @Test
        @DisplayName("should preserve parameter order")
        void shouldPreserveParameterOrder() {
            // Given
            PortOperation create = new PortOperation(
                    "createOrder",
                    TypeRef.of("OrderId"),
                    List.of(
                            TypeRef.of("CustomerId"),
                            TypeRef.of("List<OrderLineItem>"),
                            TypeRef.of("Address"),
                            TypeRef.of("Money")),
                    null);

            DrivingPort port = new DrivingPort(
                    ElementId.of(PKG + ".in.OrderCreation"),
                    PortClassification.COMMAND_HANDLER,
                    List.of(create),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(port)
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();
            MethodDoc method = results.get(0).methods().get(0);

            // Then
            assertThat(method.parameters()).containsExactly("CustomerId", "List<OrderLineItem>", "Address", "Money");
        }
    }

    @Nested
    @DisplayName("Mixed Port Selection")
    class MixedPortSelection {

        @Test
        @DisplayName("should select only driving ports")
        void shouldSelectOnlyDrivingPorts() {
            // Given
            DrivingPort drivingPort1 = DrivingPort.of(PKG + ".in.UseCase1", highConfidence(ElementKind.DRIVING_PORT));
            DrivingPort drivingPort2 = DrivingPort.of(PKG + ".in.UseCase2", highConfidence(ElementKind.DRIVING_PORT));
            DrivenPort drivenPort1 = DrivenPort.of(PKG + ".out.Repository1", highConfidence(ElementKind.DRIVEN_PORT));
            DrivenPort drivenPort2 = DrivenPort.of(PKG + ".out.Gateway1", highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(drivingPort1)
                    .add(drivingPort2)
                    .add(drivenPort1)
                    .add(drivenPort2)
                    .build();

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
            DrivingPort drivingPort1 = DrivingPort.of(PKG + ".in.UseCase1", highConfidence(ElementKind.DRIVING_PORT));
            DrivenPort drivenPort1 = DrivenPort.of(PKG + ".out.Repository1", highConfidence(ElementKind.DRIVEN_PORT));
            DrivenPort drivenPort2 = DrivenPort.of(PKG + ".out.Gateway1", highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(drivingPort1)
                    .add(drivenPort1)
                    .add(drivenPort2)
                    .build();

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
            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // Then
            assertThat(selector.selectDrivingPorts()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no driven ports")
        void shouldReturnEmptyListWhenNoDrivenPorts() {
            // Given
            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // Then
            assertThat(selector.selectDrivenPorts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Port Classification Variations")
    class PortClassificationVariations {

        @Test
        @DisplayName("should handle command handler")
        void shouldHandleCommandHandler() {
            // Given
            DrivingPort commandPort = new DrivingPort(
                    ElementId.of(PKG + ".in.CreateOrderCommand"),
                    PortClassification.COMMAND_HANDLER,
                    List.of(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(commandPort)
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();

            // Then
            assertThat(results.get(0).kind()).isEqualTo(PortKind.COMMAND);
        }

        @Test
        @DisplayName("should handle query handler")
        void shouldHandleQueryHandler() {
            // Given
            DrivingPort queryPort = new DrivingPort(
                    ElementId.of(PKG + ".in.GetOrderQuery"),
                    PortClassification.QUERY_HANDLER,
                    List.of(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVING_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(queryPort)
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivingPorts();

            // Then
            assertThat(results.get(0).kind()).isEqualTo(PortKind.QUERY);
        }

        @Test
        @DisplayName("should handle event publisher")
        void shouldHandleEventPublisher() {
            // Given
            DrivenPort eventPort = new DrivenPort(
                    ElementId.of(PKG + ".out.OrderEventPublisher"),
                    PortClassification.EVENT_PUBLISHER,
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(eventPort)
                    .build();

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
            DrivenPort gatewayPort = new DrivenPort(
                    ElementId.of(PKG + ".out.ExternalService"),
                    PortClassification.GATEWAY,
                    List.of(),
                    Optional.empty(),
                    List.of(),
                    null,
                    highConfidence(ElementKind.DRIVEN_PORT));

            ArchitecturalModel model = ArchitecturalModel.builder(ProjectContext.forTesting("app", PKG))
                    .add(gatewayPort)
                    .build();

            PortContentSelector selector = new PortContentSelector(model);

            // When
            List<PortDoc> results = selector.selectDrivenPorts();

            // Then
            assertThat(results.get(0).kind()).isEqualTo(PortKind.GATEWAY);
        }
    }
}
