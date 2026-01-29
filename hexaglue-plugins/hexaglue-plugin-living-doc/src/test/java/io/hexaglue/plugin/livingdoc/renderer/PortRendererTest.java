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

package io.hexaglue.plugin.livingdoc.renderer;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.arch.model.ir.ConfidenceLevel;
import io.hexaglue.arch.model.ir.PortDirection;
import io.hexaglue.arch.model.ir.PortKind;
import io.hexaglue.plugin.livingdoc.model.DebugInfo;
import io.hexaglue.plugin.livingdoc.model.MethodDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortRendererTest {

    private PortRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new PortRenderer();
    }

    @Nested
    class PortRendering {

        @Test
        void shouldRenderBasicPortHeader() {
            PortDoc port = createSimpleDrivingPort();
            String result = renderer.renderPort(port);

            assertThat(result).contains("### OrderUseCase");
            assertThat(result).contains("| **Kind** | Use Case |");
            assertThat(result).contains("| **Direction** | Driving (Primary/Inbound) |");
            assertThat(result).contains("| **Package** | `com.example.ports.in` |");
            assertThat(result).contains("| **Confidence** | HIGH |");
        }

        @Test
        void shouldRenderDrivenPortDirection() {
            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    createDebugInfo("OrderRepository", "com.example.ports.out"),
                    null);

            String result = renderer.renderPort(port);

            assertThat(result).contains("| **Direction** | Driven (Secondary/Outbound) |");
        }

        @Test
        void shouldIncludeSeparatorAtEnd() {
            PortDoc port = createSimpleDrivingPort();
            String result = renderer.renderPort(port);

            assertThat(result).endsWith("---\n\n");
        }

        @Test
        void shouldNotShowConfidenceBadgeForHighConfidence() {
            PortDoc port = createSimpleDrivingPort();
            String result = renderer.renderPort(port);

            assertThat(result).contains("| **Kind** | Use Case |");
            assertThat(result).doesNotContain("[Medium Confidence]");
        }

        @Test
        void shouldShowConfidenceBadgeForMediumConfidence() {
            PortDoc port = new PortDoc(
                    "SomePort",
                    "com.example.ports",
                    PortKind.GENERIC,
                    PortDirection.DRIVING,
                    ConfidenceLevel.MEDIUM,
                    List.of(),
                    List.of(),
                    createDebugInfo("SomePort", "com.example.ports"),
                    null);

            String result = renderer.renderPort(port);

            assertThat(result).contains("| **Kind** | Generic Port [Medium Confidence] |");
        }

        @Test
        void shouldFormatDifferentPortKinds() {
            assertThat(createPortWithKind(PortKind.REPOSITORY)).contains("| **Kind** | Repository |");
            assertThat(createPortWithKind(PortKind.GATEWAY)).contains("| **Kind** | Gateway |");
            assertThat(createPortWithKind(PortKind.USE_CASE)).contains("| **Kind** | Use Case |");
            assertThat(createPortWithKind(PortKind.COMMAND)).contains("| **Kind** | Command Handler |");
            assertThat(createPortWithKind(PortKind.QUERY)).contains("| **Kind** | Query Handler |");
            assertThat(createPortWithKind(PortKind.EVENT_PUBLISHER)).contains("| **Kind** | Event Publisher |");
            assertThat(createPortWithKind(PortKind.GENERIC)).contains("| **Kind** | Generic Port |");
        }
    }

    @Nested
    class ManagedTypesRendering {

        @Test
        void shouldRenderManagedTypes() {
            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of("com.example.domain.Order", "com.example.domain.Customer"),
                    List.of(),
                    createDebugInfo("OrderRepository", "com.example.ports.out"),
                    null);

            String result = renderer.renderPort(port);

            assertThat(result).contains("**Managed Domain Types**");
            assertThat(result).contains("- `Order`");
            assertThat(result).contains("- `Customer`");
        }

        @Test
        void shouldNotRenderManagedTypesWhenEmpty() {
            PortDoc port = new PortDoc(
                    "UtilityPort",
                    "com.example.ports",
                    PortKind.GENERIC,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    createDebugInfo("UtilityPort", "com.example.ports"),
                    null);

            String result = renderer.renderPort(port);

            assertThat(result).doesNotContain("**Managed Domain Types**");
        }
    }

    @Nested
    class MethodsRendering {

        @Test
        void shouldRenderMethodsTable() {
            MethodDoc method1 = new MethodDoc("save", "com.example.domain.Order", List.of("com.example.domain.Order"));
            MethodDoc method2 =
                    new MethodDoc("findById", "java.util.Optional<Order>", List.of("com.example.domain.OrderId"));

            List<MethodDoc> methods = List.of(method1, method2);

            String result = renderer.renderMethods(methods);

            assertThat(result).contains("**Methods**");
            assertThat(result).contains("| Method | Return Type | Parameters |");
            assertThat(result).contains("| `save` | `Order` | `Order` |");
            assertThat(result).contains("| `findById` | `Optional<Order>` | `OrderId` |");
        }

        @Test
        void shouldRenderMethodWithNoParameters() {
            MethodDoc method = new MethodDoc("getAll", "java.util.List<Order>", List.of());

            String result = renderer.renderMethods(List.of(method));

            assertThat(result).contains("| `getAll` | `List<Order>` | - |");
        }

        @Test
        void shouldRenderMethodWithMultipleParameters() {
            MethodDoc method = new MethodDoc(
                    "search",
                    "java.util.List<Order>",
                    List.of("java.lang.String", "java.time.LocalDate", "java.time.LocalDate"));

            String result = renderer.renderMethods(List.of(method));

            assertThat(result).contains("| `search` | `List<Order>` | `String`, `LocalDate`, `LocalDate` |");
        }

        @Test
        void shouldSimplifyTypeNamesInMethodsTable() {
            MethodDoc method =
                    new MethodDoc("process", "com.example.dto.OrderDto", List.of("com.example.dto.CreateOrderRequest"));

            String result = renderer.renderMethods(List.of(method));

            assertThat(result).contains("| `process` | `OrderDto` | `CreateOrderRequest` |");
        }
    }

    @Nested
    class MethodSignaturesRendering {

        @Test
        void shouldRenderMethodSignaturesSection() {
            MethodDoc method1 = new MethodDoc("save", "com.example.domain.Order", List.of("com.example.domain.Order"));
            MethodDoc method2 = new MethodDoc("findById", "java.util.Optional<Order>", List.of("OrderId"));

            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(method1, method2),
                    createDebugInfo("OrderRepository", "com.example.ports.out"),
                    null);

            String result = renderer.renderMethodSignatures(port);

            assertThat(result).contains("<details>");
            assertThat(result).contains("<summary>Method Signatures</summary>");
            assertThat(result).contains("```java");
            assertThat(result).contains("public interface OrderRepository {");
            assertThat(result).contains("Order save(Order arg0);");
            assertThat(result).contains("Optional<Order> findById(OrderId arg0);");
            assertThat(result).contains("```");
            assertThat(result).contains("</details>");
        }

        @Test
        void shouldRenderMethodWithNoParameters() {
            MethodDoc method = new MethodDoc("getAll", "List<Order>", List.of());
            PortDoc port = new PortDoc(
                    "OrderQuery",
                    "com.example.ports.in",
                    PortKind.QUERY,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(method),
                    createDebugInfo("OrderQuery", "com.example.ports.in"),
                    null);

            String result = renderer.renderMethodSignatures(port);

            assertThat(result).contains("List<Order> getAll();");
        }

        @Test
        void shouldRenderVoidMethod() {
            MethodDoc method = new MethodDoc("notify", "void", List.of("String"));
            PortDoc port = new PortDoc(
                    "NotificationPort",
                    "com.example.ports.out",
                    PortKind.GATEWAY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(method),
                    createDebugInfo("NotificationPort", "com.example.ports.out"),
                    null);

            String result = renderer.renderMethodSignatures(port);

            assertThat(result).contains("void notify(String arg0);");
        }

        @Test
        void shouldNumberParametersSequentially() {
            MethodDoc method =
                    new MethodDoc("create", "OrderId", List.of("CustomerId", "List<LineItem>", "Address", "Money"));
            PortDoc port = new PortDoc(
                    "OrderCreation",
                    "com.example.ports.in",
                    PortKind.COMMAND,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(method),
                    createDebugInfo("OrderCreation", "com.example.ports.in"),
                    null);

            String result = renderer.renderMethodSignatures(port);

            assertThat(result)
                    .contains("OrderId create(CustomerId arg0, List<LineItem> arg1, Address arg2, Money arg3);");
        }
    }

    @Nested
    class DebugSectionRendering {

        @Test
        void shouldRenderDebugSectionWithAllInformation() {
            MethodDoc method = new MethodDoc("save", "Order", List.of("Order"));
            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of("com.example.domain.Order"),
                    List.of(method),
                    new DebugInfo(
                            "com.example.ports.out.OrderRepository",
                            List.of("org.springframework.stereotype.Repository"),
                            "src/main/java/com/example/ports/out/OrderRepository.java",
                            10,
                            25),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("<details>");
            assertThat(result).contains("<summary>Debug Information</summary>");
            assertThat(result).contains("#### Port Information");
            assertThat(result).contains("| **Qualified Name** | `com.example.ports.out.OrderRepository` |");
            assertThat(result).contains("| **Kind** | REPOSITORY |");
            assertThat(result).contains("| **Direction** | DRIVEN |");
            assertThat(result).contains("</details>");
        }

        @Test
        void shouldRenderManagedTypesInDebugSection() {
            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of("com.example.domain.Order", "com.example.domain.Customer"),
                    List.of(),
                    createDebugInfo("OrderRepository", "com.example.ports.out"),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("#### Managed Domain Types");
            assertThat(result).contains("- `com.example.domain.Order`");
            assertThat(result).contains("- `com.example.domain.Customer`");
        }

        @Test
        void shouldRenderMethodDetailsInDebugSection() {
            MethodDoc method1 = new MethodDoc("save", "Order", List.of("Order"));
            MethodDoc method2 = new MethodDoc("findAll", "List<Order>", List.of());
            MethodDoc method3 = new MethodDoc("search", "List<Order>", List.of("String", "LocalDate", "LocalDate"));

            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(method1, method2, method3),
                    createDebugInfo("OrderRepository", "com.example.ports.out"),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("#### Methods Details");
            assertThat(result).contains("**save**");
            assertThat(result).contains("| **Return Type** | `Order` |");
            assertThat(result).contains("| **Parameters** | `Order` |");

            assertThat(result).contains("**findAll**");
            assertThat(result).contains("| **Parameters** | *none* |");

            assertThat(result).contains("**search**");
            assertThat(result).contains("| **Parameters** | `String`, `LocalDate`, `LocalDate` |");
        }

        @Test
        void shouldRenderAnnotations() {
            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    new DebugInfo(
                            "com.example.ports.out.OrderRepository",
                            List.of(
                                    "org.springframework.stereotype.Repository",
                                    "org.springframework.data.repository.Repository"),
                            "OrderRepository.java",
                            10,
                            25),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("#### Annotations");
            assertThat(result).contains("- `@Repository`");
            assertThat(result).contains("- `@Repository`");
        }

        @Test
        void shouldRenderNoAnnotationsMessage() {
            PortDoc port = new PortDoc(
                    "PlainPort",
                    "com.example.ports",
                    PortKind.GENERIC,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    new DebugInfo("com.example.ports.PlainPort", List.of(), "PlainPort.java", 5, 15),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("#### Annotations");
            assertThat(result).contains("*none*");
        }

        @Test
        void shouldRenderSourceLocation() {
            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    new DebugInfo(
                            "com.example.ports.out.OrderRepository",
                            List.of(),
                            "src/main/java/com/example/ports/out/OrderRepository.java",
                            10,
                            25),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("#### Source Location");
            assertThat(result).contains("| **File** | `src/main/java/com/example/ports/out/OrderRepository.java` |");
            assertThat(result).contains("| **Lines** | 10-25 |");
        }

        @Test
        void shouldRenderSyntheticSourceLocation() {
            PortDoc port = new PortDoc(
                    "SyntheticPort",
                    "com.example.ports",
                    PortKind.GENERIC,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of(),
                    List.of(),
                    new DebugInfo("com.example.ports.SyntheticPort", List.of(), null, 0, 0),
                    null);

            String result = renderer.renderDebugSection(port);

            assertThat(result).contains("| **File** | *synthetic* |");
        }
    }

    @Nested
    class CompletePortRendering {

        @Test
        void shouldRenderCompleteRepositoryPort() {
            MethodDoc method1 = new MethodDoc("save", "Order", List.of("Order"));
            MethodDoc method2 = new MethodDoc("findById", "Optional<Order>", List.of("OrderId"));

            PortDoc port = new PortDoc(
                    "OrderRepository",
                    "com.example.ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of("com.example.domain.Order"),
                    List.of(method1, method2),
                    new DebugInfo(
                            "com.example.ports.out.OrderRepository",
                            List.of("org.springframework.stereotype.Repository"),
                            "src/main/java/com/example/ports/out/OrderRepository.java",
                            10,
                            25),
                    null);

            String result = renderer.renderPort(port);

            // Should contain all sections
            assertThat(result).contains("### OrderRepository");
            assertThat(result).contains("**Managed Domain Types**");
            assertThat(result).contains("**Methods**");
            assertThat(result).contains("<summary>Method Signatures</summary>");
            assertThat(result).contains("<summary>Debug Information</summary>");
            assertThat(result).endsWith("---\n\n");
        }

        @Test
        void shouldRenderCompleteUseCasePort() {
            MethodDoc method = new MethodDoc("execute", "OrderId", List.of("CreateOrderRequest"));

            PortDoc port = new PortDoc(
                    "CreateOrderUseCase",
                    "com.example.ports.in",
                    PortKind.USE_CASE,
                    PortDirection.DRIVING,
                    ConfidenceLevel.HIGH,
                    List.of("com.example.domain.Order"),
                    List.of(method),
                    createDebugInfo("CreateOrderUseCase", "com.example.ports.in"),
                    null);

            String result = renderer.renderPort(port);

            assertThat(result).contains("### CreateOrderUseCase");
            assertThat(result).contains("| **Direction** | Driving (Primary/Inbound) |");
            assertThat(result).contains("**Managed Domain Types**");
            assertThat(result).contains("**Methods**");
        }

        @Test
        void shouldRenderPortWithNoMethodsOrManagedTypes() {
            PortDoc port = new PortDoc(
                    "EmptyPort",
                    "com.example.ports",
                    PortKind.GENERIC,
                    PortDirection.DRIVING,
                    ConfidenceLevel.LOW,
                    List.of(),
                    List.of(),
                    createDebugInfo("EmptyPort", "com.example.ports"),
                    null);

            String result = renderer.renderPort(port);

            assertThat(result).contains("### EmptyPort");
            assertThat(result).doesNotContain("**Managed Domain Types**");
            assertThat(result).doesNotContain("**Methods**");
            assertThat(result).contains("<summary>Debug Information</summary>");
        }
    }

    // Helper methods

    private PortDoc createSimpleDrivingPort() {
        return new PortDoc(
                "OrderUseCase",
                "com.example.ports.in",
                PortKind.USE_CASE,
                PortDirection.DRIVING,
                ConfidenceLevel.HIGH,
                List.of(),
                List.of(),
                createDebugInfo("OrderUseCase", "com.example.ports.in"),
                null);
    }

    private String createPortWithKind(PortKind kind) {
        PortDoc port = new PortDoc(
                "TestPort",
                "com.example.ports",
                kind,
                PortDirection.DRIVING,
                ConfidenceLevel.HIGH,
                List.of(),
                List.of(),
                createDebugInfo("TestPort", "com.example.ports"),
                null);
        return renderer.renderPort(port);
    }

    private DebugInfo createDebugInfo(String name, String packageName) {
        return new DebugInfo(
                packageName + "." + name,
                List.of("org.springframework.stereotype.Service"),
                "src/main/java/" + packageName.replace('.', '/') + "/" + name + ".java",
                10,
                25);
    }
}
