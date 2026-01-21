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

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ArchKind;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.MethodNode;
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
 * Tests for {@link AggregateRootBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("AggregateRootBuilder")
class AggregateRootBuilderTest {

    private AggregateRootBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        MethodRoleDetector methodRoleDetector = new MethodRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        builder = new AggregateRootBuilder(typeStructureBuilder, traceConverter, fieldRoleDetector);
    }

    @Nested
    @DisplayName("Basic Building")
    class BasicBuilding {

        @Test
        @DisplayName("should build AggregateRoot with correct id")
        void shouldBuildAggregateRootWithCorrectId() {
            FieldNode idField = createIdField("com.example.Order");
            TypeNode typeNode = createTypeNode("com.example.Order");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(idField), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.id().qualifiedName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("should build AggregateRoot with correct kind")
        void shouldBuildAggregateRootWithCorrectKind() {
            FieldNode idField = createIdField("com.example.Customer");
            TypeNode typeNode = createTypeNode("com.example.Customer");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(idField), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("should build AggregateRoot with structure")
        void shouldBuildAggregateRootWithStructure() {
            FieldNode idField = createIdField("com.example.Product");
            TypeNode typeNode = createTypeNode("com.example.Product");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(idField), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.structure()).isNotNull();
        }

        @Test
        @DisplayName("should throw when no identity field found")
        void shouldThrowWhenNoIdentityFieldFound() {
            FieldNode nameField = FieldNode.builder()
                    .simpleName("name")
                    .declaringTypeName("com.example.BadAggregate")
                    .type(TypeRef.of("java.lang.String"))
                    .modifiers(Set.of(JavaModifier.PRIVATE))
                    .build();

            TypeNode typeNode = createTypeNode("com.example.BadAggregate");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(nameField), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            assertThatThrownBy(() -> builder.build(typeNode, classification, context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("identity field");
        }
    }

    @Nested
    @DisplayName("Identity Handling")
    class IdentityHandling {

        @Test
        @DisplayName("should extract identity field")
        void shouldExtractIdentityField() {
            FieldNode idField = createIdField("com.example.Order");
            TypeNode typeNode = createTypeNode("com.example.Order");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(idField), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.identityField()).isNotNull();
            assertThat(aggregateRoot.identityField().name()).isEqualTo("id");
        }

        @Test
        @DisplayName("should detect identity by naming convention")
        void shouldDetectIdentityByNamingConvention() {
            FieldNode orderId = FieldNode.builder()
                    .simpleName("orderId")
                    .declaringTypeName("com.example.Order")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            TypeNode typeNode = createTypeNode("com.example.Order");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(orderId), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.identityField()).isNotNull();
            assertThat(aggregateRoot.identityField().name()).isEqualTo("orderId");
        }

        @Test
        @DisplayName("should compute effective identity type")
        void shouldComputeEffectiveIdentityType() {
            FieldNode idField = createIdField("com.example.Order");
            TypeNode typeNode = createTypeNode("com.example.Order");

            TestGraphQueryWithMembers graphQuery = new TestGraphQueryWithMembers(List.of(idField), List.of());
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.effectiveIdentityType()).isNotNull();
            assertThat(aggregateRoot.effectiveIdentityType().qualifiedName()).isEqualTo("java.util.UUID");
        }
    }

    @Nested
    @DisplayName("Invariant Detection")
    class InvariantDetection {

        @Test
        @DisplayName("should detect validate methods as invariants")
        void shouldDetectValidateMethodsAsInvariants() {
            FieldNode idField = createIdField("com.example.Order");
            MethodNode validateMethod = MethodNode.builder()
                    .simpleName("validateItems")
                    .declaringTypeName("com.example.Order")
                    .returnType(TypeRef.of("void"))
                    .build();

            TypeNode typeNode = createTypeNode("com.example.Order");

            TestGraphQueryWithMembers graphQuery =
                    new TestGraphQueryWithMembers(List.of(idField), List.of(validateMethod));
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.hasInvariants()).isTrue();
            assertThat(aggregateRoot.invariants()).hasSize(1);
        }

        @Test
        @DisplayName("should detect check methods as invariants")
        void shouldDetectCheckMethodsAsInvariants() {
            FieldNode idField = createIdField("com.example.Account");
            MethodNode checkMethod = MethodNode.builder()
                    .simpleName("checkBalance")
                    .declaringTypeName("com.example.Account")
                    .returnType(TypeRef.of("void"))
                    .build();

            TypeNode typeNode = createTypeNode("com.example.Account");

            TestGraphQueryWithMembers graphQuery =
                    new TestGraphQueryWithMembers(List.of(idField), List.of(checkMethod));
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.hasInvariants()).isTrue();
        }

        @Test
        @DisplayName("should detect ensure methods as invariants")
        void shouldDetectEnsureMethodsAsInvariants() {
            FieldNode idField = createIdField("com.example.Order");
            MethodNode ensureMethod = MethodNode.builder()
                    .simpleName("ensureNotEmpty")
                    .declaringTypeName("com.example.Order")
                    .returnType(TypeRef.of("void"))
                    .build();

            TypeNode typeNode = createTypeNode("com.example.Order");

            TestGraphQueryWithMembers graphQuery =
                    new TestGraphQueryWithMembers(List.of(idField), List.of(ensureMethod));
            ClassificationResult classification = createClassification(typeNode, "AGGREGATE_ROOT");
            BuilderContext context = createContextWithQuery(typeNode, classification, graphQuery);

            AggregateRoot aggregateRoot = builder.build(typeNode, classification, context);

            assertThat(aggregateRoot.hasInvariants()).isTrue();
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
            TypeNode typeNode = createTypeNode("com.example.Test");
            BuilderContext context = createEmptyContext();

            assertThatThrownBy(() -> builder.build(typeNode, null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classification");
        }

        @Test
        @DisplayName("should throw when context is null")
        void shouldThrowWhenContextIsNull() {
            TypeNode typeNode = createTypeNode("com.example.Test");
            ClassificationResult classification = createClassificationForNullCheck();

            assertThatThrownBy(() -> builder.build(typeNode, classification, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }

    // Helper methods

    private FieldNode createIdField(String declaringType) {
        return FieldNode.builder()
                .simpleName("id")
                .declaringTypeName(declaringType)
                .type(TypeRef.of("java.util.UUID"))
                .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                .annotations(List.of(AnnotationRef.of("jakarta.persistence.Id")))
                .build();
    }

    private TypeNode createTypeNode(String qualifiedName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .build();
    }

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
                "AGGREGATE_ROOT",
                ConfidenceLevel.HIGH,
                "test-criterion",
                80,
                "Test justification",
                List.of(),
                List.of());
    }

    private BuilderContext createContextWithQuery(
            TypeNode typeNode, ClassificationResult classification, TestGraphQueryWithMembers graphQuery) {
        ClassificationResults results = new ClassificationResults(Map.of(typeNode.id(), classification));
        return BuilderContext.of(graphQuery, results);
    }

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }

    /**
     * Test helper class that provides fields and methods for testing.
     */
    private static class TestGraphQueryWithMembers extends TestGraphQuery {
        private final List<FieldNode> fields;
        private final List<MethodNode> methods;

        TestGraphQueryWithMembers(List<FieldNode> fields, List<MethodNode> methods) {
            this.fields = fields;
            this.methods = methods;
        }

        @Override
        public List<FieldNode> fieldsOf(TypeNode typeNode) {
            return fields;
        }

        @Override
        public List<MethodNode> methodsOf(TypeNode typeNode) {
            return methods;
        }
    }
}
