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
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
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
 * Tests for {@link IdentifierBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("IdentifierBuilder")
class IdentifierBuilderTest {

    private IdentifierBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        MethodRoleDetector methodRoleDetector = new MethodRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        builder = new IdentifierBuilder(typeStructureBuilder, traceConverter);
    }

    @Nested
    @DisplayName("Build Identifier")
    class BuildIdentifier {

        @Test
        @DisplayName("should build Identifier with correct id")
        void shouldBuildIdentifierWithCorrectId() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderId")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContext(typeNode, classification);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.id().qualifiedName()).isEqualTo("com.example.OrderId");
        }

        @Test
        @DisplayName("should build Identifier with correct kind")
        void shouldBuildIdentifierWithCorrectKind() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.CustomerId")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContext(typeNode, classification);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.kind()).isEqualTo(ArchKind.IDENTIFIER);
        }

        @Test
        @DisplayName("should build Identifier with structure")
        void shouldBuildIdentifierWithStructure() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.ProductId")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContext(typeNode, classification);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.structure()).isNotNull();
            assertThat(identifier.structure().isRecord()).isTrue();
        }
    }

    @Nested
    @DisplayName("Wrapped Type Detection")
    class WrappedTypeDetection {

        @Test
        @DisplayName("should detect UUID wrapped type")
        void shouldDetectUuidWrappedType() {
            FieldNode valueField = FieldNode.builder()
                    .simpleName("value")
                    .declaringTypeName("com.example.OrderId")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderId")
                    .form(JavaForm.RECORD)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(valueField));
            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.wrappedType()).isNotNull();
            assertThat(identifier.wrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should detect Long wrapped type")
        void shouldDetectLongWrappedType() {
            FieldNode valueField = FieldNode.builder()
                    .simpleName("value")
                    .declaringTypeName("com.example.UserId")
                    .type(TypeRef.of("java.lang.Long"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.UserId")
                    .form(JavaForm.RECORD)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(valueField));
            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.wrappedType()).isNotNull();
            assertThat(identifier.wrappedType().qualifiedName()).isEqualTo("java.lang.Long");
        }

        @Test
        @DisplayName("should detect String wrapped type")
        void shouldDetectStringWrappedType() {
            FieldNode valueField = FieldNode.builder()
                    .simpleName("id")
                    .declaringTypeName("com.example.TransactionId")
                    .type(TypeRef.of("java.lang.String"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.TransactionId")
                    .form(JavaForm.RECORD)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(valueField));
            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.wrappedType()).isNotNull();
            assertThat(identifier.wrappedType().qualifiedName()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("should use first field when multiple fields exist")
        void shouldUseFirstFieldWhenMultipleFieldsExist() {
            FieldNode valueField = FieldNode.builder()
                    .simpleName("value")
                    .declaringTypeName("com.example.EventId")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();
            FieldNode otherField = FieldNode.builder()
                    .simpleName("createdAt")
                    .declaringTypeName("com.example.EventId")
                    .type(TypeRef.of("java.time.Instant"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.EventId")
                    .form(JavaForm.RECORD)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of(valueField, otherField));
            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Identifier identifier = builder.build(typeNode, classification, context);

            assertThat(identifier.wrappedType()).isNotNull();
            assertThat(identifier.wrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should handle identifier with no fields")
        void shouldHandleIdentifierWithNoFields() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.EmptyId")
                    .form(JavaForm.RECORD)
                    .build();

            TestGraphQueryWithFields graphQuery = new TestGraphQueryWithFields(List.of());
            ClassificationResult classification = createClassification(typeNode, "IDENTIFIER");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            Identifier identifier = builder.build(typeNode, classification, context);

            // When no fields, wrappedType should still be present but may be empty or a fallback
            assertThat(identifier).isNotNull();
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
                    .form(JavaForm.RECORD)
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
                    .form(JavaForm.RECORD)
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
                "IDENTIFIER",
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
