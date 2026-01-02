package io.hexaglue.core.graph.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EdgeTest {

    @Test
    void shouldCreateRawEdge() {
        NodeId from = NodeId.type("com.example.Order");
        NodeId to = NodeId.type("com.example.BaseEntity");

        Edge edge = Edge.raw(from, to, EdgeKind.EXTENDS);

        assertThat(edge.from()).isEqualTo(from);
        assertThat(edge.to()).isEqualTo(to);
        assertThat(edge.kind()).isEqualTo(EdgeKind.EXTENDS);
        assertThat(edge.origin()).isEqualTo(EdgeOrigin.RAW);
        assertThat(edge.proof()).isNull();
        assertThat(edge.isRaw()).isTrue();
        assertThat(edge.isDerived()).isFalse();
    }

    @Test
    void shouldCreateDerivedEdgeWithProof() {
        NodeId from = NodeId.type("com.example.OrderRepository");
        NodeId to = NodeId.type("com.example.Order");
        NodeId method = NodeId.method("com.example.OrderRepository", "findById", "com.example.OrderId");
        EdgeProof proof = EdgeProof.signatureUsage(method, "return");

        Edge edge = Edge.derived(from, to, EdgeKind.USES_IN_SIGNATURE, proof);

        assertThat(edge.origin()).isEqualTo(EdgeOrigin.DERIVED);
        assertThat(edge.proof()).isEqualTo(proof);
        assertThat(edge.isRaw()).isFalse();
        assertThat(edge.isDerived()).isTrue();
    }

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
    void shouldCreateExtendsEdge() {
        NodeId subtype = NodeId.type("com.example.Order");
        NodeId supertype = NodeId.type("com.example.BaseEntity");

        Edge edge = Edge.extends_(subtype, supertype);

        assertThat(edge.kind()).isEqualTo(EdgeKind.EXTENDS);
        assertThat(edge.isRaw()).isTrue();
    }

    @Test
    void shouldCreateImplementsEdge() {
        NodeId type = NodeId.type("com.example.OrderService");
        NodeId iface = NodeId.type("com.example.OrderUseCase");

        Edge edge = Edge.implements_(type, iface);

        assertThat(edge.kind()).isEqualTo(EdgeKind.IMPLEMENTS);
        assertThat(edge.isRaw()).isTrue();
    }

    @Test
    void shouldCreateDeclaresEdge() {
        NodeId type = NodeId.type("com.example.Order");
        NodeId field = NodeId.field("com.example.Order", "id");

        Edge edge = Edge.declares(type, field);

        assertThat(edge.kind()).isEqualTo(EdgeKind.DECLARES);
        assertThat(edge.isTypeToMember()).isTrue();
    }

    @Test
    void shouldCreateFieldTypeEdge() {
        NodeId field = NodeId.field("com.example.Order", "id");
        NodeId type = NodeId.type("java.util.UUID");

        Edge edge = Edge.fieldType(field, type);

        assertThat(edge.kind()).isEqualTo(EdgeKind.FIELD_TYPE);
        assertThat(edge.isMemberToType()).isTrue();
    }

    @Test
    void shouldCreateReturnTypeEdge() {
        NodeId method = NodeId.method("com.example.OrderRepository", "findById", "com.example.OrderId");
        NodeId type = NodeId.type("java.util.Optional");

        Edge edge = Edge.returnType(method, type);

        assertThat(edge.kind()).isEqualTo(EdgeKind.RETURN_TYPE);
    }

    @Test
    void shouldCreateParameterTypeEdge() {
        NodeId method = NodeId.method("com.example.OrderRepository", "save", "com.example.Order");
        NodeId type = NodeId.type("com.example.Order");

        Edge edge = Edge.parameterType(method, type);

        assertThat(edge.kind()).isEqualTo(EdgeKind.PARAMETER_TYPE);
    }

    @Test
    void shouldCreateAnnotatedByEdge() {
        NodeId element = NodeId.type("com.example.Order");
        NodeId annotation = NodeId.type("org.jmolecules.ddd.annotation.AggregateRoot");

        Edge edge = Edge.annotatedBy(element, annotation);

        assertThat(edge.kind()).isEqualTo(EdgeKind.ANNOTATED_BY);
    }

    @Test
    void shouldCreateUsesInSignatureEdge() {
        NodeId iface = NodeId.type("com.example.OrderRepository");
        NodeId used = NodeId.type("com.example.Order");
        NodeId method = NodeId.method("com.example.OrderRepository", "findById", "com.example.OrderId");
        EdgeProof proof = EdgeProof.signatureUsage(method, "return");

        Edge edge = Edge.usesInSignature(iface, used, proof);

        assertThat(edge.kind()).isEqualTo(EdgeKind.USES_IN_SIGNATURE);
        assertThat(edge.isDerived()).isTrue();
        assertThat(edge.proof().derivationRule()).isEqualTo(EdgeProof.RULE_SIGNATURE_USAGE);
    }

    @Test
    void shouldDetectTypeConnections() {
        NodeId typeA = NodeId.type("A");
        NodeId typeB = NodeId.type("B");
        NodeId field = NodeId.field("A", "x");

        assertThat(Edge.extends_(typeA, typeB).connectsTypes()).isTrue();
        assertThat(Edge.declares(typeA, field).connectsTypes()).isFalse();
        assertThat(Edge.fieldType(field, typeB).connectsTypes()).isFalse();
    }
}
