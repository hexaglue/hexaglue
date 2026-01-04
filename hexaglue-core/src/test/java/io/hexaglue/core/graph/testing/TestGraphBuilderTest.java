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

package io.hexaglue.core.graph.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TestGraphBuilder")
class TestGraphBuilderTest {

    @Nested
    @DisplayName("Type creation")
    class TypeCreationTest {

        @Test
        @DisplayName("should create a class")
        void shouldCreateClass() {
            ApplicationGraph graph =
                    TestGraphBuilder.create().withClass("com.example.Order").build();

            assertThat(graph.typeCount()).isEqualTo(1);
            TypeNode type = graph.typeNode("com.example.Order").orElseThrow();
            assertThat(type.isClass()).isTrue();
            assertThat(type.simpleName()).isEqualTo("Order");
            assertThat(type.packageName()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("should create a record")
        void shouldCreateRecord() {
            ApplicationGraph graph =
                    TestGraphBuilder.create().withRecord("com.example.OrderId").build();

            TypeNode type = graph.typeNode("com.example.OrderId").orElseThrow();
            assertThat(type.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should create an interface")
        void shouldCreateInterface() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withInterface("com.example.OrderRepository")
                    .build();

            TypeNode type = graph.typeNode("com.example.OrderRepository").orElseThrow();
            assertThat(type.isInterface()).isTrue();
        }

        @Test
        @DisplayName("should create multiple types")
        void shouldCreateMultipleTypes() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withRecord("com.example.OrderId")
                    .withInterface("com.example.OrderRepository")
                    .build();

            assertThat(graph.typeCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Type modifiers")
    class TypeModifiersTest {

        @Test
        @DisplayName("should add annotation")
        void shouldAddAnnotation() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .annotatedWith("org.jmolecules.ddd.annotation.AggregateRoot")
                    .build();

            TypeNode type = graph.typeNode("com.example.Order").orElseThrow();
            assertThat(type.annotations())
                    .extracting("qualifiedName")
                    .containsExactly("org.jmolecules.ddd.annotation.AggregateRoot");
        }

        @Test
        @DisplayName("should set supertype")
        void shouldSetSupertype() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.BaseEntity")
                    .withClass("com.example.Order")
                    .extending("com.example.BaseEntity")
                    .build();

            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            assertThat(order.superType()).isPresent();
            assertThat(order.superType().get().rawQualifiedName()).isEqualTo("com.example.BaseEntity");

            // Should have EXTENDS edge
            assertThat(graph.edgesFrom(order.id()))
                    .anyMatch(e ->
                            e.kind().name().equals("EXTENDS") && e.to().equals(NodeId.type("com.example.BaseEntity")));
        }

        @Test
        @DisplayName("should set interface implementation")
        void shouldSetInterfaceImplementation() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withInterface("com.example.OrderRepository")
                    .withClass("com.example.JpaOrderRepository")
                    .implementing("com.example.OrderRepository")
                    .build();

            TypeNode impl = graph.typeNode("com.example.JpaOrderRepository").orElseThrow();
            assertThat(impl.interfaces()).hasSize(1);
            assertThat(impl.interfaces().get(0).rawQualifiedName()).isEqualTo("com.example.OrderRepository");

            // Should have IMPLEMENTS edge
            assertThat(graph.edgesFrom(impl.id()))
                    .anyMatch(e -> e.kind().name().equals("IMPLEMENTS")
                            && e.to().equals(NodeId.type("com.example.OrderRepository")));
        }
    }

    @Nested
    @DisplayName("Field creation")
    class FieldCreationTest {

        @Test
        @DisplayName("should add simple field")
        void shouldAddSimpleField() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withField("id", "java.util.UUID")
                    .build();

            TypeNode type = graph.typeNode("com.example.Order").orElseThrow();
            assertThat(graph.fieldsOf(type)).hasSize(1);

            FieldNode field = graph.fieldsOf(type).get(0);
            assertThat(field.simpleName()).isEqualTo("id");
            assertThat(field.type().rawQualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should add list field")
        void shouldAddListField() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.LineItem")
                    .withClass("com.example.Order")
                    .withListField("items", "com.example.LineItem")
                    .build();

            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            FieldNode field = graph.fieldsOf(order).get(0);

            assertThat(field.type().rawQualifiedName()).isEqualTo("java.util.List");
            assertThat(field.type().isCollectionLike()).isTrue();
            assertThat(field.type().firstArgument().rawQualifiedName()).isEqualTo("com.example.LineItem");
        }

        @Test
        @DisplayName("should create DECLARES edge for field")
        void shouldCreateDeclaresEdgeForField() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withField("id", "java.util.UUID")
                    .build();

            TypeNode type = graph.typeNode("com.example.Order").orElseThrow();
            FieldNode field = graph.fieldsOf(type).get(0);

            assertThat(graph.edgesFrom(type.id()))
                    .anyMatch(e -> e.kind().name().equals("DECLARES") && e.to().equals(field.id()));
        }
    }

    @Nested
    @DisplayName("Method creation")
    class MethodCreationTest {

        @Test
        @DisplayName("should add method with return type")
        void shouldAddMethodWithReturnType() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withInterface("com.example.OrderRepository")
                    .withMethod("save", "com.example.Order", "com.example.Order")
                    .build();

            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            assertThat(graph.methodsOf(repo)).hasSize(1);

            MethodNode method = graph.methodsOf(repo).get(0);
            assertThat(method.simpleName()).isEqualTo("save");
            assertThat(method.returnType().rawQualifiedName()).isEqualTo("com.example.Order");
            assertThat(method.parameters()).hasSize(1);
            assertThat(method.parameters().get(0).type().rawQualifiedName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("should add void method")
        void shouldAddVoidMethod() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withInterface("com.example.OrderRepository")
                    .withVoidMethod("delete", "java.util.UUID")
                    .build();

            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            MethodNode method = graph.methodsOf(repo).get(0);

            assertThat(method.isVoid()).isTrue();
        }

        @Test
        @DisplayName("should create DECLARES edge for method")
        void shouldCreateDeclaresEdgeForMethod() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withInterface("com.example.OrderRepository")
                    .withMethod("findAll", "java.util.List")
                    .build();

            TypeNode type = graph.typeNode("com.example.OrderRepository").orElseThrow();
            MethodNode method = graph.methodsOf(type).get(0);

            assertThat(graph.edgesFrom(type.id()))
                    .anyMatch(e -> e.kind().name().equals("DECLARES") && e.to().equals(method.id()));
        }
    }

    @Nested
    @DisplayName("Full scenario")
    class FullScenarioTest {

        @Test
        @DisplayName("should build complete domain model")
        void shouldBuildCompleteDomainModel() {
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withRecord("com.example.OrderId")
                    .withField("value", "java.util.UUID")
                    .withClass("com.example.LineItem")
                    .withField("product", "java.lang.String")
                    .withField("quantity", "int")
                    .withClass("com.example.Order")
                    .annotatedWith("org.jmolecules.ddd.annotation.AggregateRoot")
                    .withField("id", "com.example.OrderId")
                    .withListField("items", "com.example.LineItem")
                    .withInterface("com.example.OrderRepository")
                    .annotatedWith("org.jmolecules.ddd.annotation.Repository")
                    .withMethod("save", "com.example.Order", "com.example.Order")
                    .withMethod("findById", "java.util.Optional", "com.example.OrderId")
                    .build();

            // Verify types
            assertThat(graph.typeCount()).isEqualTo(4);

            // Verify Order fields
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            assertThat(graph.fieldsOf(order)).hasSize(2);
            assertThat(graph.fieldsOf(order)).extracting("simpleName").containsExactly("id", "items");

            // Verify repository methods
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            assertThat(graph.methodsOf(repo)).hasSize(2);

            // Verify metadata
            assertThat(graph.metadata().basePackage()).isEqualTo("com.example");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTest {

        @Test
        @DisplayName("should throw if adding field without current type")
        void shouldThrowIfAddingFieldWithoutType() {
            TestGraphBuilder builder = TestGraphBuilder.create();

            assertThatThrownBy(() -> builder.withField("id", "java.util.UUID"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No current type");
        }

        @Test
        @DisplayName("should throw if adding method without current type")
        void shouldThrowIfAddingMethodWithoutType() {
            TestGraphBuilder builder = TestGraphBuilder.create();

            assertThatThrownBy(() -> builder.withMethod("save", "void"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No current type");
        }
    }
}
