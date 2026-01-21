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

package io.hexaglue.core.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.arch.model.Annotation;
import io.hexaglue.arch.model.Constructor;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.ConstructorNode;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.syntax.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeStructureBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("TypeStructureBuilder")
class TypeStructureBuilderTest {

    private TypeStructureBuilder builder;
    private FieldRoleDetector fieldRoleDetector;
    private MethodRoleDetector methodRoleDetector;

    @BeforeEach
    void setUp() {
        fieldRoleDetector = new FieldRoleDetector();
        methodRoleDetector = new MethodRoleDetector();
        builder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
    }

    @Nested
    @DisplayName("Build Basic Structure")
    class BuildBasicStructure {

        @Test
        @DisplayName("should build structure for class")
        void shouldBuildStructureForClass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .modifiers(Set.of(JavaModifier.PUBLIC))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.nature()).isEqualTo(TypeNature.CLASS);
            assertThat(structure.modifiers()).contains(Modifier.PUBLIC);
            assertThat(structure.isClass()).isTrue();
        }

        @Test
        @DisplayName("should build structure for interface")
        void shouldBuildStructureForInterface() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderRepository")
                    .form(JavaForm.INTERFACE)
                    .modifiers(Set.of(JavaModifier.PUBLIC))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.nature()).isEqualTo(TypeNature.INTERFACE);
            assertThat(structure.isInterface()).isTrue();
        }

        @Test
        @DisplayName("should build structure for record")
        void shouldBuildStructureForRecord() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderId")
                    .form(JavaForm.RECORD)
                    .modifiers(Set.of(JavaModifier.PUBLIC, JavaModifier.FINAL))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.nature()).isEqualTo(TypeNature.RECORD);
            assertThat(structure.isRecord()).isTrue();
        }

        @Test
        @DisplayName("should build structure for enum")
        void shouldBuildStructureForEnum() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderStatus")
                    .form(JavaForm.ENUM)
                    .modifiers(Set.of(JavaModifier.PUBLIC))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.nature()).isEqualTo(TypeNature.ENUM);
        }
    }

    @Nested
    @DisplayName("Map Modifiers")
    class MapModifiers {

        @Test
        @DisplayName("should map all modifiers correctly")
        void shouldMapAllModifiersCorrectly() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.CLASS)
                    .modifiers(Set.of(JavaModifier.PUBLIC, JavaModifier.ABSTRACT, JavaModifier.SEALED))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.modifiers())
                    .containsExactlyInAnyOrder(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.SEALED);
        }

        @Test
        @DisplayName("should handle empty modifiers")
        void shouldHandleEmptyModifiers() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.CLASS)
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.modifiers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Build With Superclass")
    class BuildWithSuperclass {

        @Test
        @DisplayName("should include superclass")
        void shouldIncludeSuperclass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .superType(TypeRef.of("com.example.BaseEntity"))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.superClass()).isPresent();
            assertThat(structure.superClass().get().qualifiedName()).isEqualTo("com.example.BaseEntity");
        }

        @Test
        @DisplayName("should handle no superclass")
        void shouldHandleNoSuperclass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.superClass()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Build With Interfaces")
    class BuildWithInterfaces {

        @Test
        @DisplayName("should include interfaces")
        void shouldIncludeInterfaces() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .interfaces(List.of(TypeRef.of("java.io.Serializable"), TypeRef.of("java.lang.Comparable")))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.interfaces()).hasSize(2);
            assertThat(structure.interfaces().stream().map(io.hexaglue.syntax.TypeRef::qualifiedName))
                    .containsExactly("java.io.Serializable", "java.lang.Comparable");
        }
    }

    @Nested
    @DisplayName("Build With Fields")
    class BuildWithFields {

        @Test
        @DisplayName("should include fields with roles")
        void shouldIncludeFieldsWithRoles() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();

            FieldNode idField = FieldNode.builder()
                    .simpleName("id")
                    .declaringTypeName("com.example.Order")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .annotations(List.of(AnnotationRef.of("javax.persistence.Id")))
                    .build();

            BuilderContext context = createContextWithFields(typeNode, List.of(idField));

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.fields()).hasSize(1);
            Field field = structure.fields().get(0);
            assertThat(field.name()).isEqualTo("id");
            assertThat(field.hasRole(FieldRole.IDENTITY)).isTrue();
        }

        @Test
        @DisplayName("should detect collection field role")
        void shouldDetectCollectionFieldRole() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();

            FieldNode itemsField = FieldNode.builder()
                    .simpleName("items")
                    .declaringTypeName("com.example.Order")
                    .type(TypeRef.parameterized("java.util.List", TypeRef.of("com.example.OrderItem")))
                    .build();

            BuilderContext context = createContextWithFields(typeNode, List.of(itemsField));

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.fields()).hasSize(1);
            Field field = structure.fields().get(0);
            assertThat(field.name()).isEqualTo("items");
            assertThat(field.hasRole(FieldRole.COLLECTION)).isTrue();
            assertThat(field.elementType()).isPresent();
            assertThat(field.elementType().get().qualifiedName()).isEqualTo("com.example.OrderItem");
        }
    }

    @Nested
    @DisplayName("Build With Methods")
    class BuildWithMethods {

        @Test
        @DisplayName("should include methods")
        void shouldIncludeMethods() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();

            MethodNode method = MethodNode.builder()
                    .simpleName("getId")
                    .declaringTypeName("com.example.Order")
                    .returnType(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PUBLIC))
                    .parameters(List.of())
                    .build();

            BuilderContext context = createContextWithMethods(typeNode, List.of(method));

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.methods()).hasSize(1);
            Method m = structure.methods().get(0);
            assertThat(m.name()).isEqualTo("getId");
            assertThat(m.returnType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(m.isPublic()).isTrue();
        }

        @Test
        @DisplayName("should include method parameters")
        void shouldIncludeMethodParameters() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();

            MethodNode method = MethodNode.builder()
                    .simpleName("addItem")
                    .declaringTypeName("com.example.Order")
                    .returnType(TypeRef.of("void"))
                    .parameters(List.of(
                            ParameterInfo.of("item", TypeRef.of("com.example.OrderItem")),
                            ParameterInfo.of("quantity", TypeRef.of("int"))))
                    .build();

            BuilderContext context = createContextWithMethods(typeNode, List.of(method));

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.methods()).hasSize(1);
            Method m = structure.methods().get(0);
            assertThat(m.parameters()).hasSize(2);
            assertThat(m.parameters().get(0).name()).isEqualTo("item");
            assertThat(m.parameters().get(1).name()).isEqualTo("quantity");
        }
    }

    @Nested
    @DisplayName("Build With Constructors")
    class BuildWithConstructors {

        @Test
        @DisplayName("should include constructors")
        void shouldIncludeConstructors() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .build();

            ConstructorNode constructor = ConstructorNode.builder()
                    .declaringTypeName("com.example.Order")
                    .parameters(List.of(ParameterInfo.of("id", TypeRef.of("java.util.UUID"))))
                    .modifiers(Set.of(JavaModifier.PUBLIC))
                    .build();

            BuilderContext context = createContextWithConstructors(typeNode, List.of(constructor));

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.constructors()).hasSize(1);
            Constructor c = structure.constructors().get(0);
            assertThat(c.parameters()).hasSize(1);
            assertThat(c.parameters().get(0).name()).isEqualTo("id");
        }
    }

    @Nested
    @DisplayName("Build With Annotations")
    class BuildWithAnnotations {

        @Test
        @DisplayName("should include annotations")
        void shouldIncludeAnnotations() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Order")
                    .form(JavaForm.CLASS)
                    .annotations(List.of(
                            AnnotationRef.of("javax.persistence.Entity"), AnnotationRef.of("javax.persistence.Table")))
                    .build();

            BuilderContext context = createContext(typeNode);

            TypeStructure structure = builder.build(typeNode, context);

            assertThat(structure.annotations()).hasSize(2);
            assertThat(structure.annotations().stream().map(Annotation::qualifiedName))
                    .containsExactly("javax.persistence.Entity", "javax.persistence.Table");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when typeNode is null")
        void shouldThrowWhenTypeNodeIsNull() {
            BuilderContext context = createContext(TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.CLASS)
                    .build());

            assertThatThrownBy(() -> builder.build(null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeNode");
        }

        @Test
        @DisplayName("should throw when context is null")
        void shouldThrowWhenContextIsNull() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.CLASS)
                    .build();

            assertThatThrownBy(() -> builder.build(typeNode, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }

    // Helper methods

    private static final ClassificationResults EMPTY_RESULTS = new ClassificationResults(Map.of());

    private BuilderContext createContext(TypeNode typeNode) {
        return BuilderContext.of(new TestGraphQuery(), EMPTY_RESULTS);
    }

    private BuilderContext createContextWithFields(TypeNode typeNode, List<FieldNode> fields) {
        return BuilderContext.of(new TestGraphQueryWithMembers(fields, List.of(), List.of()), EMPTY_RESULTS);
    }

    private BuilderContext createContextWithMethods(TypeNode typeNode, List<MethodNode> methods) {
        return BuilderContext.of(new TestGraphQueryWithMembers(List.of(), methods, List.of()), EMPTY_RESULTS);
    }

    private BuilderContext createContextWithConstructors(TypeNode typeNode, List<ConstructorNode> constructors) {
        return BuilderContext.of(new TestGraphQueryWithMembers(List.of(), List.of(), constructors), EMPTY_RESULTS);
    }

    /**
     * Test GraphQuery implementation that provides fields, methods, and constructors.
     */
    private static class TestGraphQueryWithMembers extends TestGraphQuery {
        private final List<FieldNode> fields;
        private final List<MethodNode> methods;
        private final List<ConstructorNode> constructors;

        TestGraphQueryWithMembers(
                List<FieldNode> fields, List<MethodNode> methods, List<ConstructorNode> constructors) {
            this.fields = fields;
            this.methods = methods;
            this.constructors = constructors;
        }

        @Override
        public List<FieldNode> fieldsOf(TypeNode typeNode) {
            return fields;
        }

        @Override
        public List<MethodNode> methodsOf(TypeNode typeNode) {
            return methods;
        }

        @Override
        public List<ConstructorNode> constructorsOf(TypeNode typeNode) {
            return constructors;
        }
    }
}
