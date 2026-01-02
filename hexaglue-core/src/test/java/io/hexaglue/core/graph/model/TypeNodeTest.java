package io.hexaglue.core.graph.model;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.SourceRef;
import io.hexaglue.core.frontend.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TypeNodeTest {

    @Test
    void shouldBuildMinimalTypeNode() {
        TypeNode node = TypeNode.builder()
                .qualifiedName("com.example.Order")
                .form(JavaForm.CLASS)
                .build();

        assertThat(node.qualifiedName()).isEqualTo("com.example.Order");
        assertThat(node.simpleName()).isEqualTo("Order");
        assertThat(node.packageName()).isEqualTo("com.example");
        assertThat(node.id()).isEqualTo(NodeId.type("com.example.Order"));
        assertThat(node.form()).isEqualTo(JavaForm.CLASS);
        assertThat(node.modifiers()).isEmpty();
        assertThat(node.superType()).isEmpty();
        assertThat(node.interfaces()).isEmpty();
        assertThat(node.annotations()).isEmpty();
    }

    @Test
    void shouldBuildFullTypeNode() {
        AnnotationRef entityAnnotation = AnnotationRef.of("org.jmolecules.ddd.annotation.Entity");
        TypeRef superType = TypeRef.of("com.example.BaseEntity");
        TypeRef interfaceType = TypeRef.of("java.io.Serializable");
        SourceRef sourceRef = SourceRef.ofLine("/src/Order.java", 10);

        TypeNode node = TypeNode.builder()
                .qualifiedName("com.example.Order")
                .form(JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.PUBLIC, JavaModifier.FINAL))
                .superType(superType)
                .interfaces(List.of(interfaceType))
                .annotations(List.of(entityAnnotation))
                .sourceRef(sourceRef)
                .build();

        assertThat(node.modifiers()).containsExactlyInAnyOrder(JavaModifier.PUBLIC, JavaModifier.FINAL);
        assertThat(node.superType()).contains(superType);
        assertThat(node.interfaces()).containsExactly(interfaceType);
        assertThat(node.annotations()).containsExactly(entityAnnotation);
        assertThat(node.sourceRef()).contains(sourceRef);
    }

    @Test
    void shouldDetectFormConvenienceMethods() {
        assertThat(buildNode(JavaForm.CLASS).isClass()).isTrue();
        assertThat(buildNode(JavaForm.INTERFACE).isInterface()).isTrue();
        assertThat(buildNode(JavaForm.RECORD).isRecord()).isTrue();
        assertThat(buildNode(JavaForm.ENUM).isEnum()).isTrue();
        assertThat(buildNode(JavaForm.ANNOTATION).isAnnotation()).isTrue();
    }

    @Test
    void shouldDetectModifierConvenienceMethods() {
        TypeNode publicNode = TypeNode.builder()
                .qualifiedName("A")
                .form(JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.PUBLIC))
                .build();

        TypeNode abstractNode = TypeNode.builder()
                .qualifiedName("B")
                .form(JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.ABSTRACT))
                .build();

        TypeNode finalNode = TypeNode.builder()
                .qualifiedName("C")
                .form(JavaForm.CLASS)
                .modifiers(Set.of(JavaModifier.FINAL))
                .build();

        assertThat(publicNode.isPublic()).isTrue();
        assertThat(abstractNode.isAbstract()).isTrue();
        assertThat(finalNode.isFinal()).isTrue();
    }

    @Test
    void shouldDetectSuffixPatterns() {
        assertThat(buildNode("OrderRepository").hasRepositorySuffix()).isTrue();
        assertThat(buildNode("PaymentGateway").hasGatewaySuffix()).isTrue();
        assertThat(buildNode("CreateOrderUseCase").hasUseCaseSuffix()).isTrue();
        assertThat(buildNode("OrderService").hasUseCaseSuffix()).isTrue();
        assertThat(buildNode("OrderId").hasIdSuffix()).isTrue();
        assertThat(buildNode("OrderCreatedEvent").hasEventSuffix()).isTrue();

        assertThat(buildNode("Order").hasRepositorySuffix()).isFalse();
    }

    @Test
    void shouldDetectJMoleculesAnnotation() {
        TypeNode withJMolecules = TypeNode.builder()
                .qualifiedName("Order")
                .form(JavaForm.CLASS)
                .annotations(List.of(AnnotationRef.of("org.jmolecules.ddd.annotation.AggregateRoot")))
                .build();

        TypeNode withOther = TypeNode.builder()
                .qualifiedName("Order")
                .form(JavaForm.CLASS)
                .annotations(List.of(AnnotationRef.of("jakarta.persistence.Entity")))
                .build();

        assertThat(withJMolecules.hasJMoleculesAnnotation()).isTrue();
        assertThat(withOther.hasJMoleculesAnnotation()).isFalse();
    }

    @Test
    void shouldFindAnnotationByName() {
        AnnotationRef anno = AnnotationRef.of("org.jmolecules.ddd.annotation.Entity", Map.of("aggregate", "Order"));

        TypeNode node = TypeNode.builder()
                .qualifiedName("OrderItem")
                .form(JavaForm.CLASS)
                .annotations(List.of(anno))
                .build();

        assertThat(node.hasAnnotation("org.jmolecules.ddd.annotation.Entity")).isTrue();
        assertThat(node.annotation("org.jmolecules.ddd.annotation.Entity")).contains(anno);
        assertThat(node.hasAnnotation("other.Annotation")).isFalse();
    }

    @Test
    void shouldImplementEqualsHashCodeByNodeId() {
        TypeNode a =
                TypeNode.builder().qualifiedName("com.A").form(JavaForm.CLASS).build();
        TypeNode b = TypeNode.builder()
                .qualifiedName("com.A")
                .form(JavaForm.INTERFACE)
                .build();
        TypeNode c =
                TypeNode.builder().qualifiedName("com.B").form(JavaForm.CLASS).build();

        // Same id = equal (even with different form)
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        // Different id = not equal
        assertThat(a).isNotEqualTo(c);
    }

    private TypeNode buildNode(JavaForm form) {
        return TypeNode.builder().qualifiedName("Test").form(form).build();
    }

    private TypeNode buildNode(String simpleName) {
        return TypeNode.builder()
                .qualifiedName("com.example." + simpleName)
                .form(JavaForm.CLASS)
                .build();
    }
}
