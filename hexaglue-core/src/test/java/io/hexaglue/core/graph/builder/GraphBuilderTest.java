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

package io.hexaglue.core.graph.builder;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraphBuilderTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(false, analyzer); // Disable derived edges for focused testing
    }

    // === Basic type creation ===

    @Test
    void shouldCreateTypeNodesFromClasses() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                }
                """);

        ApplicationGraph graph = buildGraph();

        assertThat(graph.typeCount()).isEqualTo(1);
        assertThat(graph.typeNode("com.example.Order")).isPresent();

        TypeNode order = graph.typeNode("com.example.Order").get();
        assertThat(order.form()).isEqualTo(JavaForm.CLASS);
        assertThat(order.simpleName()).isEqualTo("Order");
        assertThat(order.packageName()).isEqualTo("com.example");
    }

    @Test
    void shouldCreateTypeNodesFromInterfaces() throws IOException {
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    void save(Object order);
                }
                """);

        ApplicationGraph graph = buildGraph();

        TypeNode repo = graph.typeNode("com.example.OrderRepository").get();
        assertThat(repo.form()).isEqualTo(JavaForm.INTERFACE);
    }

    @Test
    void shouldCreateTypeNodesFromRecords() throws IOException {
        writeSource("com/example/OrderId.java", """
                package com.example;
                public record OrderId(String value) {}
                """);

        ApplicationGraph graph = buildGraph();

        TypeNode orderId = graph.typeNode("com.example.OrderId").get();
        assertThat(orderId.form()).isEqualTo(JavaForm.RECORD);
    }

    // === Hierarchy edges ===

    @Test
    void shouldCreateExtendsEdge() throws IOException {
        writeSource("com/example/BaseEntity.java", """
                package com.example;
                public abstract class BaseEntity {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order extends BaseEntity {}
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> extendsEdges = graph.edges(EdgeKind.EXTENDS);
        assertThat(extendsEdges).hasSize(1);

        Edge edge = extendsEdges.get(0);
        assertThat(edge.from()).isEqualTo(NodeId.type("com.example.Order"));
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.BaseEntity"));
        assertThat(edge.isRaw()).isTrue();
    }

    @Test
    void shouldCreateImplementsEdge() throws IOException {
        writeSource("com/example/Identifiable.java", """
                package com.example;
                public interface Identifiable {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order implements Identifiable {}
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> implementsEdges = graph.edges(EdgeKind.IMPLEMENTS);
        assertThat(implementsEdges).hasSize(1);

        Edge edge = implementsEdges.get(0);
        assertThat(edge.from()).isEqualTo(NodeId.type("com.example.Order"));
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.Identifiable"));
    }

    @Test
    void shouldNotCreateEdgeForExternalTypes() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                import java.io.Serializable;
                public class Order implements Serializable {}
                """);

        ApplicationGraph graph = buildGraph();

        // No edge to java.io.Serializable (external type)
        assertThat(graph.edges(EdgeKind.IMPLEMENTS)).isEmpty();
    }

    // === Field nodes and edges ===

    @Test
    void shouldCreateFieldNodes() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    private String id;
                    private int quantity;
                }
                """);

        ApplicationGraph graph = buildGraph();

        // 2 fields + 1 default constructor
        assertThat(graph.memberCount()).isGreaterThanOrEqualTo(2);

        NodeId fieldId = NodeId.field("com.example.Order", "id");
        assertThat(graph.fieldNode(fieldId)).isPresent();

        FieldNode field = graph.fieldNode(fieldId).get();
        assertThat(field.simpleName()).isEqualTo("id");
        assertThat(field.type().rawQualifiedName()).isEqualTo("java.lang.String");
    }

    @Test
    void shouldCreateDeclaresEdgeForField() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    private String id;
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> declaresEdges = graph.edges(EdgeKind.DECLARES);
        // At least 1 for field, potentially more for default constructor
        assertThat(declaresEdges).hasSizeGreaterThanOrEqualTo(1);

        // Check that field declares edge exists
        NodeId fieldId = NodeId.field("com.example.Order", "id");
        boolean hasFieldDeclares = declaresEdges.stream()
                .anyMatch(e -> e.from().equals(NodeId.type("com.example.Order")) && e.to().equals(fieldId));
        assertThat(hasFieldDeclares).isTrue();
    }

    @Test
    void shouldCreateFieldTypeEdge() throws IOException {
        writeSource("com/example/OrderId.java", """
                package com.example;
                public record OrderId(String value) {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    private OrderId id;
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> fieldTypeEdges = graph.edges(EdgeKind.FIELD_TYPE);
        assertThat(fieldTypeEdges).hasSize(1);

        Edge edge = fieldTypeEdges.get(0);
        assertThat(edge.from()).isEqualTo(NodeId.field("com.example.Order", "id"));
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.OrderId"));
    }

    // === Method nodes and edges ===

    @Test
    void shouldCreateMethodNodes() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    public int getTotal() {
                        return 0;
                    }
                }
                """);

        ApplicationGraph graph = buildGraph();

        NodeId methodId = NodeId.method("com.example.Order", "getTotal", "");
        assertThat(graph.methodNode(methodId)).isPresent();

        MethodNode method = graph.methodNode(methodId).get();
        assertThat(method.simpleName()).isEqualTo("getTotal");
        assertThat(method.returnType().rawQualifiedName()).isEqualTo("int");
    }

    @Test
    void shouldCreateReturnTypeEdge() throws IOException {
        writeSource("com/example/OrderId.java", """
                package com.example;
                public record OrderId(String value) {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    public OrderId getId() {
                        return null;
                    }
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> returnTypeEdges = graph.edges(EdgeKind.RETURN_TYPE);
        assertThat(returnTypeEdges).hasSize(1);

        Edge edge = returnTypeEdges.get(0);
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.OrderId"));
    }

    @Test
    void shouldCreateParameterTypeEdge() throws IOException {
        writeSource("com/example/OrderId.java", """
                package com.example;
                public record OrderId(String value) {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    void deleteById(OrderId id);
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> paramTypeEdges = graph.edges(EdgeKind.PARAMETER_TYPE);
        assertThat(paramTypeEdges).hasSize(1);

        Edge edge = paramTypeEdges.get(0);
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.OrderId"));
    }

    // === Type argument edges ===

    @Test
    void shouldCreateTypeArgumentEdge() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                import java.util.List;
                public interface OrderRepository {
                    List<Order> findAll();
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> typeArgEdges = graph.edges(EdgeKind.TYPE_ARGUMENT);
        assertThat(typeArgEdges).hasSize(1);

        Edge edge = typeArgEdges.get(0);
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.Order"));
    }

    // === Constructor nodes ===

    @Test
    void shouldCreateConstructorNodes() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {
                    public Order() {}
                    public Order(String id) {}
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<ConstructorNode> ctors = graph.memberNodes().stream()
                .filter(m -> m instanceof ConstructorNode)
                .map(m -> (ConstructorNode) m)
                .toList();

        assertThat(ctors).hasSize(2);
    }

    // === Graph query ===

    @Test
    void shouldSupportQueryInterface() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {}
                """);

        ApplicationGraph graph = buildGraph();

        assertThat(graph.query().classes().count()).isEqualTo(1);
        assertThat(graph.query().interfaces().count()).isEqualTo(1);
        assertThat(graph.query().types().count()).isEqualTo(2);
    }

    // === Deterministic ordering ===

    @Test
    void shouldBuildDeterministicGraph() throws IOException {
        writeSource("com/example/B.java", """
                package com.example;
                public class B {}
                """);
        writeSource("com/example/A.java", """
                package com.example;
                public class A {}
                """);
        writeSource("com/example/C.java", """
                package com.example;
                public class C {}
                """);

        ApplicationGraph graph1 = buildGraph();
        ApplicationGraph graph2 = buildGraph();

        // Types should be in alphabetical order
        List<String> names1 =
                graph1.typeNodes().stream().map(TypeNode::qualifiedName).toList();
        List<String> names2 =
                graph2.typeNodes().stream().map(TypeNode::qualifiedName).toList();

        assertThat(names1).isEqualTo(names2);
        assertThat(names1).containsExactly("com.example.A", "com.example.B", "com.example.C");
    }

    // === Helper methods ===

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph() {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().size());

        // Rebuild model since stream was consumed
        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
