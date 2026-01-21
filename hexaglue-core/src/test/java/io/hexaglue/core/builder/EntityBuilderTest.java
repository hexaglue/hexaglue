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

import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EntityBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("EntityBuilder")
class EntityBuilderTest {

    private EntityBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        MethodRoleDetector methodRoleDetector = new MethodRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        builder = new EntityBuilder(typeStructureBuilder, traceConverter, fieldRoleDetector);
    }

    @Nested
    @DisplayName("Build Entity")
    class BuildEntity {

        @Test
        @DisplayName("should build Entity with correct id")
        void shouldBuildEntityWithCorrectId() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderLine")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContext(typeNode, classification);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.id().qualifiedName()).isEqualTo("com.example.OrderLine");
        }

        @Test
        @DisplayName("should build Entity with correct kind")
        void shouldBuildEntityWithCorrectKind() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Customer")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContext(typeNode, classification);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.kind()).isEqualTo(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("should build Entity with structure")
        void shouldBuildEntityWithStructure() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Product")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContext(typeNode, classification);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.structure()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Identity Field Detection")
    class IdentityFieldDetection {

        @Test
        @DisplayName("should detect identity field with @Id annotation (jakarta)")
        void shouldDetectIdentityFieldWithJakartaIdAnnotation() {
            FieldNode idField = FieldNode.builder()
                    .simpleName("id")
                    .declaringTypeName("com.example.OrderLine")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE))
                    .annotations(List.of(AnnotationRef.of("jakarta.persistence.Id")))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderLine")
                    .form(JavaForm.CLASS)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(idField));
            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.hasIdentity()).isTrue();
            assertThat(entity.identityField()).isPresent();
            assertThat(entity.identityField().get().name()).isEqualTo("id");
        }

        @Test
        @DisplayName("should detect identity field with @Id annotation (javax)")
        void shouldDetectIdentityFieldWithJavaxIdAnnotation() {
            FieldNode idField = FieldNode.builder()
                    .simpleName("customerId")
                    .declaringTypeName("com.example.Customer")
                    .type(TypeRef.of("java.lang.Long"))
                    .modifiers(Set.of(JavaModifier.PRIVATE))
                    .annotations(List.of(AnnotationRef.of("javax.persistence.Id")))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Customer")
                    .form(JavaForm.CLASS)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(idField));
            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.hasIdentity()).isTrue();
            assertThat(entity.identityField()).isPresent();
            assertThat(entity.identityField().get().name()).isEqualTo("customerId");
        }

        @Test
        @DisplayName("should detect identity field by naming convention")
        void shouldDetectIdentityFieldByNamingConvention() {
            FieldNode idField = FieldNode.builder()
                    .simpleName("id")
                    .declaringTypeName("com.example.Product")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Product")
                    .form(JavaForm.CLASS)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(idField));
            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.hasIdentity()).isTrue();
            assertThat(entity.identityField()).isPresent();
        }

        @Test
        @DisplayName("should return empty identity when no identity field found")
        void shouldReturnEmptyIdentityWhenNoIdentityFieldFound() {
            FieldNode nameField = FieldNode.builder()
                    .simpleName("name")
                    .declaringTypeName("com.example.BaseEntity")
                    .type(TypeRef.of("java.lang.String"))
                    .modifiers(Set.of(JavaModifier.PRIVATE))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.BaseEntity")
                    .form(JavaForm.CLASS)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(nameField));
            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.hasIdentity()).isFalse();
            assertThat(entity.identityField()).isEmpty();
        }

        @Test
        @DisplayName("should detect identity field ending with Id matching type name")
        void shouldDetectIdentityFieldEndingWithId() {
            // productId in Product class should be detected as identity (productId matches Product)
            FieldNode productIdField = FieldNode.builder()
                    .simpleName("productId")
                    .declaringTypeName("com.example.Product")
                    .type(TypeRef.of("com.example.ProductId"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Product")
                    .form(JavaForm.CLASS)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(productIdField));
            ClassificationResult classification = createClassification(typeNode, "ENTITY");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Entity entity = builder.build(typeNode, classification, context);

            assertThat(entity.hasIdentity()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when typeNode is null")
        void shouldThrowWhenTypeNodeIsNull() {
            ClassificationResult classification = createClassificationForNullCheck();
            BuilderContext context = createEmptyContext();

            assertThatThrownBy(() -> builder.build(null, classification, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeNode");
        }

        @Test
        @DisplayName("should throw when classification is null")
        void shouldThrowWhenClassificationIsNull() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.CLASS)
                    .build();
            BuilderContext context = createEmptyContext();

            assertThatThrownBy(() -> builder.build(typeNode, null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should throw when context is null")
        void shouldThrowWhenContextIsNull() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Test")
                    .form(JavaForm.CLASS)
                    .build();
            ClassificationResult classification = createClassificationForNullCheck();

            assertThatThrownBy(() -> builder.build(typeNode, classification, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }

    // Helper methods

    private ClassificationResult createClassification(TypeNode typeNode, String kind) {
        return ClassificationResult.classified(
                typeNode.id(),
                ClassificationTarget.DOMAIN,
                kind,
                ConfidenceLevel.HIGH,
                "test-criterion",
                80,
                "Test justification",
                List.of(),
                List.of());
    }

    private ClassificationResult createClassificationForNullCheck() {
        NodeId nodeId = NodeId.type("com.example.Test");
        return ClassificationResult.classified(
                nodeId,
                ClassificationTarget.DOMAIN,
                "ENTITY",
                ConfidenceLevel.HIGH,
                "test-criterion",
                80,
                "Test justification",
                List.of(),
                List.of());
    }

    private BuilderContext createContext(TypeNode typeNode, ClassificationResult classification) {
        ClassificationResults results = new ClassificationResults(Map.of(typeNode.id(), classification));
        return BuilderContext.of(new TestGraphQuery(), results);
    }

    private BuilderContext createContextWithQuery(
            TypeNode typeNode, ClassificationResult classification, TestGraphQueryWithFields graphQuery) {
        ClassificationResults results = new ClassificationResults(Map.of(typeNode.id(), classification));
        return BuilderContext.of(graphQuery, results);
    }

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }

    /**
     * Test helper class that provides fields for testing.
     */
    private static class TestGraphQueryWithFields extends TestGraphQuery {
        private final List<FieldNode> fields;

        TestGraphQueryWithFields(List<FieldNode> fields) {
            this.fields = fields;
        }

        @Override
        public List<FieldNode> fieldsOf(TypeNode typeNode) {
            return fields;
        }
    }
}
