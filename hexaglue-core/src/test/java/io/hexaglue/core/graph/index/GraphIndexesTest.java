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

package io.hexaglue.core.graph.index;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphIndexesTest {

    private GraphIndexes indexes;

    @BeforeEach
    void setUp() {
        indexes = new GraphIndexes();
    }

    // === Type indexing tests ===

    @Test
    void shouldIndexTypeByPackage() {
        TypeNode order = typeNode("com.example.domain.Order", JavaForm.CLASS);
        TypeNode customer = typeNode("com.example.domain.Customer", JavaForm.CLASS);
        TypeNode repo = typeNode("com.example.ports.OrderRepository", JavaForm.INTERFACE);

        indexes.indexNode(order);
        indexes.indexNode(customer);
        indexes.indexNode(repo);

        assertThat(indexes.typesByPackage("com.example.domain")).containsExactlyInAnyOrder(order.id(), customer.id());
        assertThat(indexes.typesByPackage("com.example.ports")).containsExactly(repo.id());
        assertThat(indexes.typesByPackage("com.other")).isEmpty();
    }

    @Test
    void shouldIndexTypeByForm() {
        TypeNode classNode = typeNode("com.example.Order", JavaForm.CLASS);
        TypeNode interfaceNode = typeNode("com.example.OrderRepository", JavaForm.INTERFACE);
        TypeNode recordNode = typeNode("com.example.OrderId", JavaForm.RECORD);
        TypeNode enumNode = typeNode("com.example.OrderStatus", JavaForm.ENUM);

        indexes.indexNode(classNode);
        indexes.indexNode(interfaceNode);
        indexes.indexNode(recordNode);
        indexes.indexNode(enumNode);

        assertThat(indexes.typesByForm(JavaForm.CLASS)).containsExactly(classNode.id());
        assertThat(indexes.typesByForm(JavaForm.INTERFACE)).containsExactly(interfaceNode.id());
        assertThat(indexes.typesByForm(JavaForm.RECORD)).containsExactly(recordNode.id());
        assertThat(indexes.typesByForm(JavaForm.ENUM)).containsExactly(enumNode.id());

        assertThat(indexes.allClasses()).containsExactly(classNode.id());
        assertThat(indexes.allInterfaces()).containsExactly(interfaceNode.id());
        assertThat(indexes.allRecords()).containsExactly(recordNode.id());
    }

    @Test
    void shouldIndexTypeByAnnotation() {
        TypeNode entity = TypeNode.builder()
                .qualifiedName("com.example.Order")
                .form(JavaForm.CLASS)
                .annotations(List.of(AnnotationRef.of("org.jmolecules.ddd.annotation.AggregateRoot")))
                .build();

        TypeNode valueObject = TypeNode.builder()
                .qualifiedName("com.example.OrderId")
                .form(JavaForm.RECORD)
                .annotations(List.of(AnnotationRef.of("org.jmolecules.ddd.annotation.ValueObject")))
                .build();

        indexes.indexNode(entity);
        indexes.indexNode(valueObject);

        assertThat(indexes.byAnnotation("org.jmolecules.ddd.annotation.AggregateRoot"))
                .containsExactly(entity.id());
        assertThat(indexes.byAnnotation("org.jmolecules.ddd.annotation.ValueObject"))
                .containsExactly(valueObject.id());
        assertThat(indexes.byAnnotation("other.Annotation")).isEmpty();
    }

    @Test
    void shouldTrackAllTypes() {
        TypeNode a = typeNode("A", JavaForm.CLASS);
        TypeNode b = typeNode("B", JavaForm.INTERFACE);

        indexes.indexNode(a);
        indexes.indexNode(b);

        assertThat(indexes.allTypes()).containsExactlyInAnyOrder(a.id(), b.id());
        assertThat(indexes.typeCount()).isEqualTo(2);
    }

    // === Member indexing tests ===

    @Test
    void shouldTrackAllMembers() {
        FieldNode field = fieldNode("com.example.Order", "id");
        MethodNode method = methodNode("com.example.Order", "getTotal");

        indexes.indexNode(field);
        indexes.indexNode(method);

        assertThat(indexes.allMembers()).containsExactlyInAnyOrder(field.id(), method.id());
        assertThat(indexes.memberCount()).isEqualTo(2);
    }

    // === Edge indexing tests ===

    @Test
    void shouldIndexDeclaresEdge() {
        NodeId typeId = NodeId.type("com.example.Order");
        NodeId fieldId = NodeId.field("com.example.Order", "id");
        NodeId methodId = NodeId.method("com.example.Order", "getTotal", "");

        indexes.indexEdge(Edge.declares(typeId, fieldId));
        indexes.indexEdge(Edge.declares(typeId, methodId));

        assertThat(indexes.membersOf(typeId)).containsExactlyInAnyOrder(fieldId, methodId);
        assertThat(indexes.declaringTypeOf(fieldId)).contains(typeId);
        assertThat(indexes.declaringTypeOf(methodId)).contains(typeId);
    }

    @Test
    void shouldIndexExtendsEdge() {
        NodeId subtype = NodeId.type("com.example.Order");
        NodeId supertype = NodeId.type("com.example.BaseEntity");

        indexes.indexEdge(Edge.extends_(subtype, supertype));

        assertThat(indexes.subtypesOf(supertype)).containsExactly(subtype);
        assertThat(indexes.supertypesOf(subtype)).containsExactly(supertype);
        assertThat(indexes.hasSubtypes(supertype)).isTrue();
        assertThat(indexes.hasSubtypes(subtype)).isFalse();
    }

    @Test
    void shouldIndexImplementsEdge() {
        NodeId type = NodeId.type("com.example.OrderServiceImpl");
        NodeId iface1 = NodeId.type("com.example.OrderService");
        NodeId iface2 = NodeId.type("java.io.Serializable");

        indexes.indexEdge(Edge.implements_(type, iface1));
        indexes.indexEdge(Edge.implements_(type, iface2));

        assertThat(indexes.implementorsOf(iface1)).containsExactly(type);
        assertThat(indexes.interfacesOf(type)).containsExactlyInAnyOrder(iface1, iface2);
        assertThat(indexes.hasImplementors(iface1)).isTrue();
        assertThat(indexes.hasImplementors(NodeId.type("other"))).isFalse();
    }

    @Test
    void shouldIndexFieldTypeEdge() {
        NodeId field1 = NodeId.field("com.example.Order", "id");
        NodeId field2 = NodeId.field("com.example.Customer", "id");
        NodeId uuidType = NodeId.type("java.util.UUID");

        indexes.indexEdge(Edge.fieldType(field1, uuidType));
        indexes.indexEdge(Edge.fieldType(field2, uuidType));

        assertThat(indexes.fieldsOfType(uuidType)).containsExactlyInAnyOrder(field1, field2);
    }

    @Test
    void shouldIndexReturnTypeEdge() {
        NodeId method = NodeId.method("com.example.OrderRepository", "findById", "com.example.OrderId");
        NodeId returnType = NodeId.type("java.util.Optional");

        indexes.indexEdge(Edge.returnType(method, returnType));

        assertThat(indexes.methodsReturning(returnType)).containsExactly(method);
    }

    @Test
    void shouldIndexParameterTypeEdge() {
        NodeId method = NodeId.method("com.example.OrderRepository", "save", "com.example.Order");
        NodeId paramType = NodeId.type("com.example.Order");

        indexes.indexEdge(Edge.parameterType(method, paramType));

        assertThat(indexes.methodsWithParameter(paramType)).containsExactly(method);
    }

    @Test
    void shouldIndexUsesInSignatureEdge() {
        NodeId repoInterface = NodeId.type("com.example.OrderRepository");
        NodeId orderType = NodeId.type("com.example.Order");
        NodeId methodId = NodeId.method("com.example.OrderRepository", "save", "com.example.Order");
        EdgeProof proof = EdgeProof.signatureUsage(methodId, "param:0");

        indexes.indexEdge(Edge.usesInSignature(repoInterface, orderType, proof));

        assertThat(indexes.interfacesUsingInSignature(orderType)).containsExactly(repoInterface);
    }

    @Test
    void shouldDetectRepositorySignatureUsage() {
        NodeId repoInterface = NodeId.type("com.example.OrderRepository");
        NodeId orderType = NodeId.type("com.example.Order");
        NodeId methodId = NodeId.method("com.example.OrderRepository", "save", "com.example.Order");
        EdgeProof proof = EdgeProof.signatureUsage(methodId, "param:0");

        indexes.indexEdge(Edge.usesInSignature(repoInterface, orderType, proof));

        assertThat(indexes.isUsedInRepositorySignature(orderType)).isTrue();
        assertThat(indexes.isUsedInRepositorySignature(NodeId.type("other"))).isFalse();
    }

    // === Annotation query tests ===

    @Test
    void shouldCheckAnnotationPresence() {
        TypeNode entity = TypeNode.builder()
                .qualifiedName("com.example.Order")
                .form(JavaForm.CLASS)
                .annotations(List.of(AnnotationRef.of("org.jmolecules.ddd.annotation.Entity")))
                .build();

        indexes.indexNode(entity);

        assertThat(indexes.hasAnnotation(entity.id(), "org.jmolecules.ddd.annotation.Entity"))
                .isTrue();
        assertThat(indexes.hasAnnotation(entity.id(), "other.Annotation")).isFalse();
    }

    // === Helper methods ===

    private TypeNode typeNode(String qualifiedName, JavaForm form) {
        return TypeNode.builder().qualifiedName(qualifiedName).form(form).build();
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
