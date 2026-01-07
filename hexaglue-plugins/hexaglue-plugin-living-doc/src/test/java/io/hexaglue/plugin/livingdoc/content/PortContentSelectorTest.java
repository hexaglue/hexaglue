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

import static io.hexaglue.plugin.livingdoc.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.spi.ir.ConfidenceLevel;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import io.hexaglue.spi.ir.PortKind;
import io.hexaglue.spi.ir.PortMethod;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortContentSelectorTest {

    private PortContentSelector selector;

    @Nested
    class DrivingPortSelection {

        @BeforeEach
        void setUp() {
            PortMethod placeOrderMethod = method("placeOrder", "com.example.domain.OrderId", "OrderRequest");
            PortMethod cancelOrderMethod = voidMethod("cancelOrder", "com.example.domain.OrderId");

            Port orderUseCase = drivingPort(
                    "OrderingProducts",
                    "com.example.ports.in",
                    PortKind.USE_CASE,
                    List.of(placeOrderMethod, cancelOrderMethod),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(orderUseCase));
            selector = new PortContentSelector(ir);
        }

        @Test
        void shouldSelectDrivingPorts() {
            List<PortDoc> results = selector.selectDrivingPorts();

            assertThat(results).hasSize(1);
            PortDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderingProducts");
            assertThat(doc.packageName()).isEqualTo("com.example.ports.in");
            assertThat(doc.kind()).isEqualTo(PortKind.USE_CASE);
            assertThat(doc.direction()).isEqualTo(PortDirection.DRIVING);
            assertThat(doc.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        }

        @Test
        void shouldTransformMethodsCorrectly() {
            List<PortDoc> results = selector.selectDrivingPorts();
            PortDoc doc = results.get(0);

            assertThat(doc.methods()).hasSize(2);

            MethodDoc placeOrder = doc.methods().get(0);
            assertThat(placeOrder.name()).isEqualTo("placeOrder");
            assertThat(placeOrder.returnType()).isEqualTo("com.example.domain.OrderId");
            assertThat(placeOrder.parameters()).containsExactly("OrderRequest");

            MethodDoc cancelOrder = doc.methods().get(1);
            assertThat(cancelOrder.name()).isEqualTo("cancelOrder");
            assertThat(cancelOrder.returnType()).isEqualTo("void");
            assertThat(cancelOrder.parameters()).containsExactly("com.example.domain.OrderId");
        }

        @Test
        void shouldExtractManagedTypes() {
            List<PortDoc> results = selector.selectDrivingPorts();
            PortDoc doc = results.get(0);

            assertThat(doc.managedTypes()).containsExactly("com.example.domain.Order");
        }

        @Test
        void shouldCreateDebugInfoWithSourceLocation() {
            List<PortDoc> results = selector.selectDrivingPorts();
            PortDoc doc = results.get(0);

            assertThat(doc.debug()).isNotNull();
            DebugInfo debug = doc.debug();
            assertThat(debug.qualifiedName()).isEqualTo("com.example.ports.in.OrderingProducts");
            assertThat(debug.annotations()).containsExactly("org.springframework.stereotype.Service");
            assertThat(debug.sourceFile()).isEqualTo("src/main/java/com/example/ports/in/OrderingProducts.java");
            assertThat(debug.lineStart()).isEqualTo(12);
            assertThat(debug.lineEnd()).isEqualTo(35);
        }
    }

    @Nested
    class DrivenPortSelection {

        @BeforeEach
        void setUp() {
            PortMethod findByIdMethod = method("findById", "java.util.Optional<Order>", "com.example.domain.OrderId");
            PortMethod saveMethod = method("save", "com.example.domain.Order", "com.example.domain.Order");
            PortMethod deleteMethod = voidMethod("delete", "com.example.domain.OrderId");

            Port orderRepository = drivenPort(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    List.of(findByIdMethod, saveMethod, deleteMethod),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(orderRepository));
            selector = new PortContentSelector(ir);
        }

        @Test
        void shouldSelectDrivenPorts() {
            List<PortDoc> results = selector.selectDrivenPorts();

            assertThat(results).hasSize(1);
            PortDoc doc = results.get(0);
            assertThat(doc.name()).isEqualTo("OrderRepository");
            assertThat(doc.packageName()).isEqualTo("com.example.ports.out");
            assertThat(doc.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(doc.direction()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        void shouldTransformRepositoryMethods() {
            List<PortDoc> results = selector.selectDrivenPorts();
            PortDoc doc = results.get(0);

            assertThat(doc.methods()).hasSize(3);
            assertThat(doc.methods()).extracting("name").containsExactly("findById", "save", "delete");
        }

        @Test
        void shouldHandleMethodsWithMultipleParameters() {
            PortMethod queryMethod = method(
                    "findByStatus",
                    "java.util.List<Order>",
                    "java.lang.String",
                    "java.time.LocalDate",
                    "java.time.LocalDate");
            Port customPort = drivenPort(
                    "OrderQueryPort",
                    "com.example.ports.out",
                    PortKind.GATEWAY,
                    List.of(queryMethod),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(customPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivenPorts();
            MethodDoc method = results.get(0).methods().get(0);

            assertThat(method.parameters())
                    .containsExactly("java.lang.String", "java.time.LocalDate", "java.time.LocalDate");
        }
    }

    @Nested
    class MethodTransformation {

        @Test
        void shouldTransformMethodWithNoParameters() {
            PortMethod getAllMethod = method("getAll", "java.util.List<Order>");
            Port port = drivingPort(
                    "OrderQuery",
                    "com.example.ports.in",
                    PortKind.QUERY,
                    List.of(getAllMethod),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(port));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            MethodDoc method = results.get(0).methods().get(0);

            assertThat(method.name()).isEqualTo("getAll");
            assertThat(method.returnType()).isEqualTo("java.util.List<Order>");
            assertThat(method.parameters()).isEmpty();
        }

        @Test
        void shouldTransformVoidMethod() {
            PortMethod notifyMethod = voidMethod("notifyCustomer", "java.lang.String");
            Port port = drivenPort(
                    "NotificationGateway", "com.example.ports.out", PortKind.GATEWAY, List.of(notifyMethod), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(port));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivenPorts();
            MethodDoc method = results.get(0).methods().get(0);

            assertThat(method.returnType()).isEqualTo("void");
        }

        @Test
        void shouldPreserveParameterOrder() {
            PortMethod createMethod =
                    method("createOrder", "OrderId", "CustomerId", "List<OrderLineItem>", "Address", "Money");
            Port port = drivingPort(
                    "OrderCreation",
                    "com.example.ports.in",
                    PortKind.COMMAND,
                    List.of(createMethod),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(port));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            MethodDoc method = results.get(0).methods().get(0);

            assertThat(method.parameters()).containsExactly("CustomerId", "List<OrderLineItem>", "Address", "Money");
        }
    }

    @Nested
    class ManagedTypesHandling {

        @Test
        void shouldHandlePortWithMultipleManagedTypes() {
            Port aggregatePort = drivingPort(
                    "OrderManagement",
                    "com.example.ports.in",
                    PortKind.USE_CASE,
                    List.of(method("manage", "void")),
                    List.of("com.example.domain.Order", "com.example.domain.Customer", "com.example.domain.Product"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(aggregatePort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            PortDoc doc = results.get(0);

            assertThat(doc.managedTypes())
                    .containsExactly(
                            "com.example.domain.Order", "com.example.domain.Customer", "com.example.domain.Product");
        }

        @Test
        void shouldHandlePortWithNoManagedTypes() {
            Port utilityPort = drivingPort(
                    "SystemHealth",
                    "com.example.ports.in",
                    PortKind.QUERY,
                    List.of(method("checkHealth", "boolean")),
                    List.of());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(utilityPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            PortDoc doc = results.get(0);

            assertThat(doc.managedTypes()).isEmpty();
        }
    }

    @Nested
    class DebugInfoTransformation {

        @Test
        void shouldCreateDebugInfoForSyntheticPort() {
            Port syntheticPort = new Port(
                    "com.example.ports.SyntheticPort",
                    "SyntheticPort",
                    "com.example.ports",
                    PortKind.GENERIC,
                    io.hexaglue.spi.ir.PortDirection.DRIVING,
                    io.hexaglue.spi.ir.ConfidenceLevel.LOW,
                    List.of(),
                    List.of(),
                    List.of(),
                    io.hexaglue.spi.ir.SourceRef.unknown());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(syntheticPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            DebugInfo debug = results.get(0).debug();

            assertThat(debug.sourceFile()).isNull();
            assertThat(debug.lineStart()).isZero();
            assertThat(debug.lineEnd()).isZero();
        }

        @Test
        void shouldIncludeAnnotationsInDebugInfo() {
            Port port = drivingPort(
                    "OrderUseCase",
                    "com.example.ports.in",
                    PortKind.USE_CASE,
                    List.of(method("execute", "void")),
                    List.of());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(port));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            DebugInfo debug = results.get(0).debug();

            assertThat(debug.annotations()).containsExactly("org.springframework.stereotype.Service");
        }
    }

    @Nested
    class MixedPortSelection {

        @BeforeEach
        void setUp() {
            Port drivingPort1 = drivingPort(
                    "UseCase1",
                    "com.example.ports.in",
                    PortKind.USE_CASE,
                    List.of(method("execute", "void")),
                    List.of());
            Port drivingPort2 = drivingPort(
                    "UseCase2", "com.example.ports.in", PortKind.USE_CASE, List.of(method("run", "void")), List.of());
            Port drivenPort1 = drivenPort(
                    "Repository1",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    List.of(method("save", "void")),
                    List.of());
            Port drivenPort2 = drivenPort(
                    "Gateway1", "com.example.ports.out", PortKind.GATEWAY, List.of(method("send", "void")), List.of());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(drivingPort1, drivingPort2, drivenPort1, drivenPort2));
            selector = new PortContentSelector(ir);
        }

        @Test
        void shouldSelectOnlyDrivingPorts() {
            List<PortDoc> results = selector.selectDrivingPorts();

            assertThat(results).hasSize(2);
            assertThat(results).extracting("name").containsExactly("UseCase1", "UseCase2");
            assertThat(results).allMatch(doc -> doc.direction() == PortDirection.DRIVING);
        }

        @Test
        void shouldSelectOnlyDrivenPorts() {
            List<PortDoc> results = selector.selectDrivenPorts();

            assertThat(results).hasSize(2);
            assertThat(results).extracting("name").containsExactly("Repository1", "Gateway1");
            assertThat(results).allMatch(doc -> doc.direction() == PortDirection.DRIVEN);
        }
    }

    @Nested
    class EmptySelections {

        @BeforeEach
        void setUp() {
            IrSnapshot ir = emptyIrSnapshot();
            selector = new PortContentSelector(ir);
        }

        @Test
        void shouldReturnEmptyListWhenNoDrivingPorts() {
            assertThat(selector.selectDrivingPorts()).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoDrivenPorts() {
            assertThat(selector.selectDrivenPorts()).isEmpty();
        }
    }

    @Nested
    class PortKindVariations {

        @Test
        void shouldHandleCommandHandler() {
            Port commandPort = drivingPort(
                    "CreateOrderCommand",
                    "com.example.ports.in",
                    PortKind.COMMAND,
                    List.of(method("handle", "OrderId", "CreateOrderRequest")),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(commandPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            assertThat(results.get(0).kind()).isEqualTo(PortKind.COMMAND);
        }

        @Test
        void shouldHandleQueryHandler() {
            Port queryPort = drivingPort(
                    "GetOrderQuery",
                    "com.example.ports.in",
                    PortKind.QUERY,
                    List.of(method("query", "OrderDto", "OrderId")),
                    List.of("com.example.domain.Order"));

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(queryPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivingPorts();
            assertThat(results.get(0).kind()).isEqualTo(PortKind.QUERY);
        }

        @Test
        void shouldHandleEventPublisher() {
            Port eventPort = drivenPort(
                    "OrderEventPublisher",
                    "com.example.ports.out",
                    PortKind.EVENT_PUBLISHER,
                    List.of(voidMethod("publish", "OrderCreatedEvent")),
                    List.of());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(eventPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivenPorts();
            assertThat(results.get(0).kind()).isEqualTo(PortKind.EVENT_PUBLISHER);
        }

        @Test
        void shouldHandleGenericPort() {
            Port genericPort = drivenPort(
                    "ExternalService",
                    "com.example.ports.out",
                    PortKind.GENERIC,
                    List.of(method("call", "String", "Request")),
                    List.of());

            IrSnapshot ir = createIrSnapshot(List.of(), List.of(genericPort));
            selector = new PortContentSelector(ir);

            List<PortDoc> results = selector.selectDrivenPorts();
            assertThat(results.get(0).kind()).isEqualTo(PortKind.GENERIC);
        }
    }
}
