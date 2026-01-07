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

package io.hexaglue.core.graph;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.*;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationGraphTest {

    private ApplicationGraph graph;

    @BeforeEach
    void setUp() {
        GraphMetadata metadata = GraphMetadata.of("com.example", 17, 0);
        graph = new ApplicationGraph(metadata);
    }

    // === Invariant G-1: Edge endpoints must exist ===

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

    // === Invariant G-2: No duplicate node ids ===

    @Test
    void shouldRejectDuplicateNodeId() {
        TypeNode node1 = typeNode("com.example.Order");
        TypeNode node2 = typeNode("com.example.Order"); // Same id

        graph.addNode(node1);

        assertThatThrownBy(() -> graph.addNode(node2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate node id");
    }

    // === Node access ===

    @Test
    void shouldRetrieveNodeById() {
        TypeNode order = typeNode("com.example.Order");
        graph.addNode(order);

        assertThat(graph.node(order.id())).contains(order);
        assertThat(graph.typeNode(order.id())).contains(order);
        assertThat(graph.typeNode("com.example.Order")).contains(order);
    }

    @Test
    void shouldReturnEmptyForUnknownNode() {
        assertThat(graph.node(NodeId.type("unknown"))).isEmpty();
        assertThat(graph.typeNode(NodeId.type("unknown"))).isEmpty();
        assertThat(graph.typeNode("unknown")).isEmpty();
    }

    @Test
    void shouldListAllNodes() {
        TypeNode order = typeNode("com.example.Order");
        TypeNode customer = typeNode("com.example.Customer");
        graph.addNode(order);
        graph.addNode(customer);

        assertThat(graph.nodes()).containsExactly(order, customer);
        assertThat(graph.typeNodes()).containsExactly(order, customer);
    }

    @Test
    void shouldListMemberNodes() {
        TypeNode order = typeNode("com.example.Order");
        FieldNode field = fieldNode("com.example.Order", "id");
        MethodNode method = methodNode("com.example.Order", "getTotal");

        graph.addNode(order);
        graph.addNode(field);
        graph.addNode(method);

        assertThat(graph.memberNodes()).containsExactly(field, method);
    }

    // === Edge access ===

    @Test
    void shouldListAllEdges() {
        TypeNode child = typeNode("com.example.Child");
        TypeNode parent = typeNode("com.example.Parent");
        graph.addNode(child);
        graph.addNode(parent);

        Edge edge = Edge.extends_(child.id(), parent.id());
        graph.addEdge(edge);

        assertThat(graph.edges()).containsExactly(edge);
    }

    @Test
    void shouldFilterEdgesByKind() {
        TypeNode order = typeNode("com.example.Order");
        TypeNode baseEntity = typeNode("com.example.BaseEntity");
        TypeNode serializable = typeNode("java.io.Serializable");
        graph.addNode(order);
        graph.addNode(baseEntity);
        graph.addNode(serializable);

        Edge extendsEdge = Edge.extends_(order.id(), baseEntity.id());
        Edge implementsEdge = Edge.implements_(order.id(), serializable.id());
        graph.addEdge(extendsEdge);
        graph.addEdge(implementsEdge);

        assertThat(graph.edges(EdgeKind.EXTENDS)).containsExactly(extendsEdge);
        assertThat(graph.edges(EdgeKind.IMPLEMENTS)).containsExactly(implementsEdge);
    }

    @Test
    void shouldFilterEdgesByFromNode() {
        TypeNode order = typeNode("com.example.Order");
        TypeNode customer = typeNode("com.example.Customer");
        TypeNode baseEntity = typeNode("com.example.BaseEntity");
        graph.addNode(order);
        graph.addNode(customer);
        graph.addNode(baseEntity);

        Edge orderExtends = Edge.extends_(order.id(), baseEntity.id());
        Edge customerExtends = Edge.extends_(customer.id(), baseEntity.id());
        graph.addEdge(orderExtends);
        graph.addEdge(customerExtends);

        assertThat(graph.edgesFrom(order.id())).containsExactly(orderExtends);
        assertThat(graph.edgesTo(baseEntity.id())).containsExactlyInAnyOrder(orderExtends, customerExtends);
    }

    @Test
    void shouldSeparateRawAndDerivedEdges() {
        TypeNode repo = typeNode("com.example.OrderRepository");
        TypeNode order = typeNode("com.example.Order");
        graph.addNode(repo);
        graph.addNode(order);

        Edge rawEdge = Edge.extends_(repo.id(), order.id());
        graph.addEdge(rawEdge);

        NodeId method = NodeId.method("com.example.OrderRepository", "save", "com.example.Order");
        graph.addNode(methodNode("com.example.OrderRepository", "save"));
        EdgeProof proof = EdgeProof.signatureUsage(method, "param:0");
        Edge derivedEdge = Edge.usesInSignature(repo.id(), order.id(), proof);
        graph.addEdge(derivedEdge);

        assertThat(graph.rawEdges()).containsExactly(rawEdge);
        assertThat(graph.derivedEdges()).containsExactly(derivedEdge);
    }

    // === Statistics ===

    @Test
    void shouldTrackCounts() {
        TypeNode order = typeNode("com.example.Order");
        FieldNode field = fieldNode("com.example.Order", "id");
        graph.addNode(order);
        graph.addNode(field);

        assertThat(graph.nodeCount()).isEqualTo(2);
        assertThat(graph.typeCount()).isEqualTo(1);
        assertThat(graph.memberCount()).isEqualTo(1);
    }

    // === Convenience methods ===

    @Test
    void shouldReturnMembersOfType() {
        TypeNode order = typeNode("com.example.Order");
        FieldNode field = fieldNode("com.example.Order", "id");
        MethodNode method = methodNode("com.example.Order", "getTotal");

        graph.addNode(order);
        graph.addNode(field);
        graph.addNode(method);
        graph.addEdge(Edge.declares(order.id(), field.id()));
        graph.addEdge(Edge.declares(order.id(), method.id()));

        assertThat(graph.membersOf(order)).containsExactlyInAnyOrder(field, method);
        assertThat(graph.fieldsOf(order)).containsExactly(field);
        assertThat(graph.methodsOf(order)).containsExactly(method);
    }

    @Test
    void shouldReturnSupertypeAndInterfaces() {
        TypeNode order = typeNode("com.example.Order");
        TypeNode baseEntity = typeNode("com.example.BaseEntity");
        TypeNode serializable = typeNode("java.io.Serializable");

        graph.addNode(order);
        graph.addNode(baseEntity);
        graph.addNode(serializable);
        graph.addEdge(Edge.extends_(order.id(), baseEntity.id()));
        graph.addEdge(Edge.implements_(order.id(), serializable.id()));

        assertThat(graph.supertypeOf(order)).contains(baseEntity);
        assertThat(graph.interfacesOf(order)).containsExactly(serializable);
    }

    // === GraphQuery ===

    @Test
    void shouldProvideQueryInterface() {
        TypeNode order = typeNode("com.example.domain.Order");
        TypeNode repo = TypeNode.builder()
                .qualifiedName("com.example.ports.OrderRepository")
                .form(JavaForm.INTERFACE)
                .build();

        graph.addNode(order);
        graph.addNode(repo);

        var query = graph.query();

        assertThat(query.types()).containsExactlyInAnyOrder(order, repo);
        assertThat(query.interfaces().toList()).containsExactly(repo);
        assertThat(query.classes().toList()).containsExactly(order);
        assertThat(query.type("com.example.domain.Order")).contains(order);
    }

    // === Metadata ===

    @Test
    void shouldStoreMetadata() {
        GraphMetadata metadata = GraphMetadata.builder()
                .basePackage("com.example")
                .javaVersion(17)
                .sourceCount(10)
                .build();

        ApplicationGraph graphWithMeta = new ApplicationGraph(metadata);

        assertThat(graphWithMeta.metadata().basePackage()).isEqualTo("com.example");
        assertThat(graphWithMeta.metadata().javaVersion()).isEqualTo(17);
        assertThat(graphWithMeta.metadata().sourceCount()).isEqualTo(10);
    }

    // === Helper methods ===

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

    private MethodNode methodNode(String declaringType, String name) {
        return MethodNode.builder()
                .declaringTypeName(declaringType)
                .simpleName(name)
                .returnType(TypeRef.of("void"))
                .modifiers(Set.of(JavaModifier.PUBLIC))
                .build();
    }
}
