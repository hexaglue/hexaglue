package io.hexaglue.core.graph.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NodeIdTest {

    @Test
    void shouldCreateTypeNodeId() {
        NodeId id = NodeId.type("com.example.Order");

        assertThat(id.value()).isEqualTo("type:com.example.Order");
        assertThat(id.kind()).isEqualTo("type");
        assertThat(id.isType()).isTrue();
        assertThat(id.isMember()).isFalse();
    }

    @Test
    void shouldCreateFieldNodeId() {
        NodeId id = NodeId.field("com.example.Order", "id");

        assertThat(id.value()).isEqualTo("field:com.example.Order#id");
        assertThat(id.kind()).isEqualTo("field");
        assertThat(id.isField()).isTrue();
        assertThat(id.isMember()).isTrue();
    }

    @Test
    void shouldCreateMethodNodeId() {
        NodeId id = NodeId.method("com.example.Order", "getTotal", "");

        assertThat(id.value()).isEqualTo("method:com.example.Order#getTotal()");
        assertThat(id.kind()).isEqualTo("method");
        assertThat(id.isMethod()).isTrue();
        assertThat(id.isMember()).isTrue();
    }

    @Test
    void shouldCreateMethodNodeIdWithParams() {
        NodeId id = NodeId.method("com.example.Order", "addItem", "com.example.OrderItem,int");

        assertThat(id.value()).isEqualTo("method:com.example.Order#addItem(com.example.OrderItem,int)");
    }

    @Test
    void shouldCreateConstructorNodeId() {
        NodeId id = NodeId.constructor("com.example.Order", "com.example.OrderId");

        assertThat(id.value()).isEqualTo("ctor:com.example.Order#<init>(com.example.OrderId)");
        assertThat(id.kind()).isEqualTo("ctor");
        assertThat(id.isConstructor()).isTrue();
        assertThat(id.isMember()).isTrue();
    }

    @Test
    void shouldBeComparable() {
        NodeId a = NodeId.type("com.example.A");
        NodeId b = NodeId.type("com.example.B");

        assertThat(a).isLessThan(b);
        assertThat(a.compareTo(a)).isZero();
    }

    @Test
    void shouldRejectNullValue() {
        assertThatThrownBy(() -> new NodeId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectBlankValue() {
        assertThatThrownBy(() -> new NodeId("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
