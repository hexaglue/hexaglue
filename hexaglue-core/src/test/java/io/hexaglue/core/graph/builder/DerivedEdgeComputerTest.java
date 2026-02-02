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

/**
 * Tests for {@link DerivedEdgeComputer}.
 *
 * <p>Focuses on DERIVED edges: USES_IN_SIGNATURE and USES_AS_COLLECTION_ELEMENT.
 */
class DerivedEdgeComputerTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer); // Enable derived edges
    }

    // === USES_IN_SIGNATURE ===

    @Test
    void shouldCreateUsesInSignatureForReturnType() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    Order findById(String id);
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).hasSize(1);

        Edge edge = usesInSignature.get(0);
        assertThat(edge.from()).isEqualTo(NodeId.type("com.example.OrderRepository"));
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.Order"));
        assertThat(edge.origin()).isEqualTo(EdgeOrigin.DERIVED);
        assertThat(edge.proof()).isNotNull();
        assertThat(edge.proof().derivationRule()).isEqualTo(EdgeProof.RULE_SIGNATURE_USAGE);
    }

    @Test
    void shouldCreateUsesInSignatureForParameterType() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    void save(Order order);
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).hasSize(1);

        Edge edge = usesInSignature.get(0);
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.Order"));
        assertThat(edge.proof().via()).contains("param");
    }

    @Test
    void shouldCreateUsesInSignatureForGenericReturnType() throws IOException {
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

        // Should have USES_IN_SIGNATURE for Order (type argument)
        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).anyMatch(e -> e.to().equals(NodeId.type("com.example.Order")));
    }

    @Test
    void shouldCreateUsesInSignatureForOptionalReturnType() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                import java.util.Optional;
                public interface OrderRepository {
                    Optional<Order> findById(String id);
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).anyMatch(e -> e.to().equals(NodeId.type("com.example.Order")));
    }

    @Test
    void shouldNotDuplicateUsesInSignatureEdge() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    Order findById(String id);
                    Order save(Order order);
                    void delete(Order order);
                }
                """);

        ApplicationGraph graph = buildGraph();

        // Should have only ONE edge from OrderRepository to Order
        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        long orderEdgeCount = usesInSignature.stream()
                .filter(e -> e.from().equals(NodeId.type("com.example.OrderRepository")))
                .filter(e -> e.to().equals(NodeId.type("com.example.Order")))
                .count();
        assertThat(orderEdgeCount).isEqualTo(1);
    }

    @Test
    void shouldNotCreateUsesInSignatureForClasses() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderService.java", """
                package com.example;
                public class OrderService {
                    public Order process(Order order) { return order; }
                }
                """);

        ApplicationGraph graph = buildGraph();

        // USES_IN_SIGNATURE is only for interfaces
        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).isEmpty();
    }

    @Test
    void shouldIgnoreVoidReturnType() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    void delete(String id);
                }
                """);

        ApplicationGraph graph = buildGraph();

        // Should have no USES_IN_SIGNATURE edges (void is not a type)
        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).isEmpty();
    }

    @Test
    void shouldIgnoreExternalTypesInSignature() throws IOException {
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    String findNameById(String id);
                }
                """);

        ApplicationGraph graph = buildGraph();

        // String is external, no USES_IN_SIGNATURE edge
        List<Edge> usesInSignature = graph.edges(EdgeKind.USES_IN_SIGNATURE);
        assertThat(usesInSignature).isEmpty();
    }

    // === USES_AS_COLLECTION_ELEMENT ===

    @Test
    void shouldCreateUsesAsCollectionElementForListField() throws IOException {
        writeSource("com/example/LineItem.java", """
                package com.example;
                public class LineItem {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                import java.util.List;
                public class Order {
                    private List<LineItem> items;
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> collectionEdges = graph.edges(EdgeKind.USES_AS_COLLECTION_ELEMENT);
        assertThat(collectionEdges).hasSize(1);

        Edge edge = collectionEdges.get(0);
        assertThat(edge.from()).isEqualTo(NodeId.type("com.example.Order"));
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.LineItem"));
        assertThat(edge.origin()).isEqualTo(EdgeOrigin.DERIVED);
        assertThat(edge.proof()).isNotNull();
        assertThat(edge.proof().derivationRule()).isEqualTo(EdgeProof.RULE_COLLECTION_UNWRAP);
    }

    @Test
    void shouldCreateUsesAsCollectionElementForSetField() throws IOException {
        writeSource("com/example/Tag.java", """
                package com.example;
                public class Tag {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                import java.util.Set;
                public class Order {
                    private Set<Tag> tags;
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> collectionEdges = graph.edges(EdgeKind.USES_AS_COLLECTION_ELEMENT);
        assertThat(collectionEdges).hasSize(1);

        Edge edge = collectionEdges.get(0);
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.Tag"));
        assertThat(edge.proof().derivationRule()).isEqualTo(EdgeProof.RULE_COLLECTION_UNWRAP);
    }

    @Test
    void shouldCreateUsesAsCollectionElementForOptionalField() throws IOException {
        writeSource("com/example/Discount.java", """
                package com.example;
                public class Discount {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                import java.util.Optional;
                public class Order {
                    private Optional<Discount> discount;
                }
                """);

        ApplicationGraph graph = buildGraph();

        List<Edge> collectionEdges = graph.edges(EdgeKind.USES_AS_COLLECTION_ELEMENT);
        assertThat(collectionEdges).hasSize(1);

        Edge edge = collectionEdges.get(0);
        assertThat(edge.to()).isEqualTo(NodeId.type("com.example.Discount"));
        assertThat(edge.proof().derivationRule()).isEqualTo(EdgeProof.RULE_OPTIONAL_UNWRAP);
    }

    @Test
    void shouldIgnoreExternalElementTypes() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                import java.util.List;
                public class Order {
                    private List<String> tags;
                }
                """);

        ApplicationGraph graph = buildGraph();

        // String is external, no edge
        List<Edge> collectionEdges = graph.edges(EdgeKind.USES_AS_COLLECTION_ELEMENT);
        assertThat(collectionEdges).isEmpty();
    }

    @Test
    void shouldNotDuplicateCollectionElementEdge() throws IOException {
        writeSource("com/example/LineItem.java", """
                package com.example;
                public class LineItem {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                import java.util.List;
                import java.util.Set;
                public class Order {
                    private List<LineItem> items;
                    private Set<LineItem> uniqueItems;
                }
                """);

        ApplicationGraph graph = buildGraph();

        // Should have only ONE edge from Order to LineItem
        List<Edge> collectionEdges = graph.edges(EdgeKind.USES_AS_COLLECTION_ELEMENT);
        long lineItemEdgeCount = collectionEdges.stream()
                .filter(e -> e.from().equals(NodeId.type("com.example.Order")))
                .filter(e -> e.to().equals(NodeId.type("com.example.LineItem")))
                .count();
        assertThat(lineItemEdgeCount).isEqualTo(1);
    }

    // === EdgeProof verification ===

    @Test
    void shouldIncludeMethodIdInProof() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    Order findById(String id);
                }
                """);

        ApplicationGraph graph = buildGraph();

        Edge edge = graph.edges(EdgeKind.USES_IN_SIGNATURE).get(0);
        EdgeProof proof = edge.proof();

        assertThat(proof.sourceNode()).isNotNull();
        assertThat(proof.sourceNode().value()).contains("method:");
        assertThat(proof.sourceNode().value()).contains("findById");
    }

    @Test
    void shouldIncludeFieldIdInProof() throws IOException {
        writeSource("com/example/LineItem.java", """
                package com.example;
                public class LineItem {}
                """);
        writeSource("com/example/Order.java", """
                package com.example;
                import java.util.List;
                public class Order {
                    private List<LineItem> items;
                }
                """);

        ApplicationGraph graph = buildGraph();

        Edge edge = graph.edges(EdgeKind.USES_AS_COLLECTION_ELEMENT).get(0);
        EdgeProof proof = edge.proof();

        assertThat(proof.sourceNode()).isNotNull();
        assertThat(proof.sourceNode().value()).contains("field:");
        assertThat(proof.sourceNode().value()).contains("items");
    }

    // === Idempotency ===

    @Test
    void shouldBeIdempotent() throws IOException {
        writeSource("com/example/Order.java", """
                package com.example;
                public class Order {}
                """);
        writeSource("com/example/OrderRepository.java", """
                package com.example;
                public interface OrderRepository {
                    Order findById(String id);
                }
                """);

        // Build graph (includes derived edges)
        ApplicationGraph graph = buildGraph();
        int initialEdgeCount = graph.edgeCount();

        // Compute again (should not add duplicate edges)
        new DerivedEdgeComputer().compute(graph);
        int afterSecondCompute = graph.edgeCount();

        assertThat(afterSecondCompute).isEqualTo(initialEdgeCount);
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
