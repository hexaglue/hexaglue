package io.hexaglue.core.graph;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.builder.DerivedEdgeComputer;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ApplicationGraph invariants.
 *
 * <p>Invariants:
 * <ul>
 *   <li><b>G-1</b>: Edge endpoints must exist as nodes before the edge is added</li>
 *   <li><b>G-2</b>: Node ids are unique - adding a duplicate throws an exception</li>
 *   <li><b>G-3</b>: Graph and indexes are deterministic - same input = same output</li>
 *   <li><b>G-4</b>: DERIVED edges must have a proof, RAW edges must not</li>
 *   <li><b>G-5</b>: DerivedEdgeComputer is idempotent - compute() twice = same result</li>
 * </ul>
 */
class GraphInvariantsTest {

    private ApplicationGraph graph;

    @BeforeEach
    void setUp() {
        graph = new ApplicationGraph(GraphMetadata.of("com.example", 17, 0));
    }

    // =========================================================================
    // G-1: Edge endpoints must exist
    // =========================================================================

    @Nested
    class InvariantG1_EdgeEndpointsMustExist {

        @Test
        void shouldRejectEdgeWithUnknownFromNode() {
            TypeNode to = typeNode("com.example.Target");
            graph.addNode(to);

            NodeId unknownFrom = NodeId.type("com.example.Unknown");

            assertThatThrownBy(() -> graph.addEdge(Edge.extends_(unknownFrom, to.id())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Edge from unknown node");
        }

        @Test
        void shouldRejectEdgeWithUnknownToNode() {
            TypeNode from = typeNode("com.example.Source");
            graph.addNode(from);

            NodeId unknownTo = NodeId.type("com.example.Unknown");

            assertThatThrownBy(() -> graph.addEdge(Edge.extends_(from.id(), unknownTo)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Edge to unknown node");
        }

        @Test
        void shouldAcceptEdgeWithBothEndpointsExisting() {
            TypeNode from = typeNode("com.example.Child");
            TypeNode to = typeNode("com.example.Parent");
            graph.addNode(from);
            graph.addNode(to);

            graph.addEdge(Edge.extends_(from.id(), to.id()));

            assertThat(graph.edgeCount()).isEqualTo(1);
        }

        @Test
        void shouldRejectDerivedEdgeWithUnknownEndpoints() {
            TypeNode from = typeNode("com.example.Repo");
            graph.addNode(from);

            NodeId unknownTo = NodeId.type("com.example.Order");
            EdgeProof proof = EdgeProof.signatureUsage(NodeId.method("com.example.Repo", "find", ""), "return");

            assertThatThrownBy(() -> graph.addEdge(Edge.usesInSignature(from.id(), unknownTo, proof)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Edge to unknown node");
        }
    }

    // =========================================================================
    // G-2: No duplicate node ids
    // =========================================================================

    @Nested
    class InvariantG2_NoDuplicateNodeIds {

        @Test
        void shouldRejectDuplicateTypeNode() {
            TypeNode node1 = typeNode("com.example.Order");
            TypeNode node2 = typeNode("com.example.Order"); // Same id

            graph.addNode(node1);

            assertThatThrownBy(() -> graph.addNode(node2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicate node id");
        }

        @Test
        void shouldRejectDuplicateFieldNode() {
            TypeNode type = typeNode("com.example.Order");
            FieldNode field1 = fieldNode("com.example.Order", "id");
            FieldNode field2 = fieldNode("com.example.Order", "id"); // Same id

            graph.addNode(type);
            graph.addNode(field1);

            assertThatThrownBy(() -> graph.addNode(field2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicate node id");
        }

        @Test
        void shouldRejectDuplicateMethodNode() {
            TypeNode type = typeNode("com.example.Order");
            MethodNode method1 = methodNode("com.example.Order", "getTotal", "");
            MethodNode method2 = methodNode("com.example.Order", "getTotal", ""); // Same id

            graph.addNode(type);
            graph.addNode(method1);

            assertThatThrownBy(() -> graph.addNode(method2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duplicate node id");
        }

        @Test
        void shouldAllowDifferentNodes() {
            TypeNode type = typeNode("com.example.Order");
            FieldNode field = fieldNode("com.example.Order", "id");
            MethodNode method = methodNode("com.example.Order", "getTotal", "");

            graph.addNode(type);
            graph.addNode(field);
            graph.addNode(method);

            assertThat(graph.nodeCount()).isEqualTo(3);
        }
    }

    // =========================================================================
    // G-3: Deterministic graph and indexes
    // =========================================================================

    @Nested
    class InvariantG3_DeterministicGraphAndIndexes {

        @TempDir
        Path tempDir;

        @Test
        void shouldBuildDeterministicGraph_sameOrder() throws IOException {
            // Build graph twice with same order
            writeSourceFiles();

            ApplicationGraph graph1 = buildGraph();
            ApplicationGraph graph2 = buildGraph();

            // Should have identical structure
            assertThat(graph1.nodeCount()).isEqualTo(graph2.nodeCount());
            assertThat(graph1.edgeCount()).isEqualTo(graph2.edgeCount());

            // Types should be in same order
            List<String> typeNames1 =
                    graph1.typeNodes().stream().map(TypeNode::qualifiedName).toList();
            List<String> typeNames2 =
                    graph2.typeNodes().stream().map(TypeNode::qualifiedName).toList();

            assertThat(typeNames1).isEqualTo(typeNames2);
        }

        @Test
        void shouldBuildDeterministicGraph_alphabeticalOrder() throws IOException {
            writeSourceFiles();

            ApplicationGraph graph = buildGraph();

            // Types should be in alphabetical order
            List<String> typeNames =
                    graph.typeNodes().stream().map(TypeNode::qualifiedName).toList();

            assertThat(typeNames)
                    .containsExactly("com.example.Customer", "com.example.Order", "com.example.OrderRepository");
        }

        @Test
        void shouldHaveDeterministicIndexes() throws IOException {
            writeSourceFiles();

            ApplicationGraph graph1 = buildGraph();
            ApplicationGraph graph2 = buildGraph();

            // Same package queries should return same results
            Set<NodeId> pkg1 = graph1.indexes().typesByPackage("com.example");
            Set<NodeId> pkg2 = graph2.indexes().typesByPackage("com.example");

            assertThat(pkg1).isEqualTo(pkg2);

            // Same form queries should return same results
            Set<NodeId> classes1 = graph1.indexes().typesByForm(JavaForm.CLASS);
            Set<NodeId> classes2 = graph2.indexes().typesByForm(JavaForm.CLASS);

            assertThat(classes1).isEqualTo(classes2);

            Set<NodeId> interfaces1 = graph1.indexes().typesByForm(JavaForm.INTERFACE);
            Set<NodeId> interfaces2 = graph2.indexes().typesByForm(JavaForm.INTERFACE);

            assertThat(interfaces1).isEqualTo(interfaces2);
        }

        @Test
        void shouldHaveDeterministicEdges() throws IOException {
            writeSourceFiles();

            ApplicationGraph graph1 = buildGraph();
            ApplicationGraph graph2 = buildGraph();

            // Edges by kind should be in same order
            List<Edge> extends1 = graph1.edges(EdgeKind.EXTENDS);
            List<Edge> extends2 = graph2.edges(EdgeKind.EXTENDS);

            assertThat(extends1.size()).isEqualTo(extends2.size());
            for (int i = 0; i < extends1.size(); i++) {
                assertThat(extends1.get(i).from()).isEqualTo(extends2.get(i).from());
                assertThat(extends1.get(i).to()).isEqualTo(extends2.get(i).to());
            }

            // Derived edges should be same
            List<Edge> derived1 = graph1.derivedEdges();
            List<Edge> derived2 = graph2.derivedEdges();

            assertThat(derived1.size()).isEqualTo(derived2.size());
        }

        @Test
        void shouldQueryDeterministically() throws IOException {
            writeSourceFiles();

            ApplicationGraph graph1 = buildGraph();
            ApplicationGraph graph2 = buildGraph();

            // Query results should be identical
            List<TypeNode> classes1 = graph1.query().classes().toList();
            List<TypeNode> classes2 = graph2.query().classes().toList();

            assertThat(classes1.stream().map(TypeNode::qualifiedName).toList())
                    .isEqualTo(classes2.stream().map(TypeNode::qualifiedName).toList());

            List<TypeNode> interfaces1 = graph1.query().interfaces().toList();
            List<TypeNode> interfaces2 = graph2.query().interfaces().toList();

            assertThat(interfaces1.stream().map(TypeNode::qualifiedName).toList())
                    .isEqualTo(interfaces2.stream().map(TypeNode::qualifiedName).toList());
        }

        private void writeSourceFiles() throws IOException {
            // Write files in non-alphabetical order to test sorting
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {}
                    """);
            writeSource(
                    "com/example/Customer.java",
                    """
                    package com.example;
                    public class Customer {}
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);
        }

        private void writeSource(String relativePath, String content) throws IOException {
            Path file = tempDir.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }

        private ApplicationGraph buildGraph() {
            SpoonFrontend frontend = new SpoonFrontend();
            GraphBuilder builder = new GraphBuilder(true);

            JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

            JavaSemanticModel model = frontend.build(input);
            GraphMetadata metadata =
                    GraphMetadata.of("com.example", 17, (int) model.types().count());

            model = frontend.build(input);
            return builder.build(model, metadata);
        }
    }

    // =========================================================================
    // G-4: DERIVED edges require proof, RAW edges must not have proof
    // =========================================================================

    @Nested
    class InvariantG4_ProofRequirements {

        @Test
        void shouldRejectDerivedEdgeWithoutProof() {
            NodeId from = NodeId.type("A");
            NodeId to = NodeId.type("B");

            assertThatThrownBy(() -> new Edge(from, to, EdgeKind.USES_IN_SIGNATURE, EdgeOrigin.DERIVED, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DERIVED edges must have a proof");
        }

        @Test
        void shouldRejectRawEdgeWithProof() {
            NodeId from = NodeId.type("A");
            NodeId to = NodeId.type("B");
            EdgeProof proof = EdgeProof.signatureUsage(from, "return");

            assertThatThrownBy(() -> new Edge(from, to, EdgeKind.EXTENDS, EdgeOrigin.RAW, proof))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RAW edges should not have a proof");
        }

        @Test
        void shouldAcceptDerivedEdgeWithProof() {
            NodeId from = NodeId.type("com.example.Repo");
            NodeId to = NodeId.type("com.example.Order");
            NodeId method = NodeId.method("com.example.Repo", "find", "");
            EdgeProof proof = EdgeProof.signatureUsage(method, "return");

            Edge edge = Edge.usesInSignature(from, to, proof);

            assertThat(edge.isDerived()).isTrue();
            assertThat(edge.proof()).isNotNull();
        }

        @Test
        void shouldAcceptRawEdgeWithoutProof() {
            NodeId from = NodeId.type("A");
            NodeId to = NodeId.type("B");

            Edge edge = Edge.extends_(from, to);

            assertThat(edge.isRaw()).isTrue();
            assertThat(edge.proof()).isNull();
        }

        @Test
        void shouldHaveProofWithRequiredFields() {
            NodeId method = NodeId.method("com.example.Repo", "find", "");

            EdgeProof proof = EdgeProof.signatureUsage(method, "return");

            assertThat(proof.sourceNode()).isEqualTo(method);
            assertThat(proof.via()).isEqualTo("return");
            assertThat(proof.derivationRule()).isEqualTo(EdgeProof.RULE_SIGNATURE_USAGE);
        }

        @Test
        void shouldRejectProofWithNullSourceNode() {
            assertThatThrownBy(() -> new EdgeProof(null, "return", EdgeProof.RULE_SIGNATURE_USAGE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sourceNode cannot be null");
        }

        @Test
        void shouldRejectProofWithNullVia() {
            NodeId method = NodeId.method("Test", "test", "");

            assertThatThrownBy(() -> new EdgeProof(method, null, EdgeProof.RULE_SIGNATURE_USAGE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("via cannot be null");
        }

        @Test
        void shouldRejectProofWithNullRule() {
            NodeId method = NodeId.method("Test", "test", "");

            assertThatThrownBy(() -> new EdgeProof(method, "return", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("derivationRule cannot be null");
        }
    }

    // =========================================================================
    // G-5: DerivedEdgeComputer is idempotent
    // =========================================================================

    @Nested
    class InvariantG5_DerivedComputeIdempotent {

        @TempDir
        Path tempDir;

        @Test
        void shouldBeIdempotent_computeTwice() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {}
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            int initialEdgeCount = graph.edgeCount();

            // Compute again
            new DerivedEdgeComputer().compute(graph);
            int afterSecondCompute = graph.edgeCount();

            assertThat(afterSecondCompute).isEqualTo(initialEdgeCount);
        }

        @Test
        void shouldBeIdempotent_computeThrice() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {}
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    import java.util.List;
                    public interface OrderRepository {
                        List<Order> findAll();
                        Order save(Order order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            int initialEdgeCount = graph.edgeCount();

            // Compute multiple times
            new DerivedEdgeComputer().compute(graph);
            new DerivedEdgeComputer().compute(graph);
            new DerivedEdgeComputer().compute(graph);
            int afterMultipleComputes = graph.edgeCount();

            assertThat(afterMultipleComputes).isEqualTo(initialEdgeCount);
        }

        @Test
        void shouldNotDuplicateDerivedEdges() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {}
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();

            // Count USES_IN_SIGNATURE edges
            long initialCount = graph.edges(EdgeKind.USES_IN_SIGNATURE).size();

            // Compute again
            new DerivedEdgeComputer().compute(graph);
            long afterCompute = graph.edges(EdgeKind.USES_IN_SIGNATURE).size();

            assertThat(afterCompute).isEqualTo(initialCount);
        }

        private void writeSource(String relativePath, String content) throws IOException {
            Path file = tempDir.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        }

        private ApplicationGraph buildGraph() {
            SpoonFrontend frontend = new SpoonFrontend();
            GraphBuilder builder = new GraphBuilder(true);

            JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

            JavaSemanticModel model = frontend.build(input);
            GraphMetadata metadata =
                    GraphMetadata.of("com.example", 17, (int) model.types().count());

            model = frontend.build(input);
            return builder.build(model, metadata);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private TypeNode typeNode(String qualifiedName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .build();
    }

    private FieldNode fieldNode(String declaringType, String name) {
        return FieldNode.builder()
                .declaringTypeName(declaringType)
                .simpleName(name)
                .type(TypeRef.of("java.lang.String"))
                .modifiers(Set.of(JavaModifier.PRIVATE))
                .build();
    }

    private MethodNode methodNode(String declaringType, String name, String paramSig) {
        return MethodNode.builder()
                .declaringTypeName(declaringType)
                .simpleName(name)
                .returnType(TypeRef.of("void"))
                .modifiers(Set.of(JavaModifier.PUBLIC))
                .build();
    }
}
