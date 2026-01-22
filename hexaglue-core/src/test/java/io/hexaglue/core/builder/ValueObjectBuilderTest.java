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
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.frontend.JavaModifier;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.ConstructorNode;
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
 * Tests for {@link ValueObjectBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("ValueObjectBuilder")
class ValueObjectBuilderTest {

    private ValueObjectBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        MethodRoleDetector methodRoleDetector = new MethodRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        builder = new ValueObjectBuilder(typeStructureBuilder, traceConverter);
    }

    @Nested
    @DisplayName("Build ValueObject")
    class BuildValueObject {

        @Test
        @DisplayName("should build ValueObject with correct id")
        void shouldBuildValueObjectWithCorrectId() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Money")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContext(typeNode, classification);

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.id().qualifiedName()).isEqualTo("com.example.Money");
        }

        @Test
        @DisplayName("should build ValueObject with correct kind")
        void shouldBuildValueObjectWithCorrectKind() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Address")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContext(typeNode, classification);

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.kind()).isEqualTo(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("should build ValueObject with structure")
        void shouldBuildValueObjectWithStructure() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.DateRange")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContext(typeNode, classification);

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.structure()).isNotNull();
            assertThat(valueObject.structure().isRecord()).isTrue();
        }

        @Test
        @DisplayName("should build ValueObject with classification trace")
        void shouldBuildValueObjectWithClassificationTrace() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Email")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContext(typeNode, classification);

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.classification()).isNotNull();
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

    /**
     * Tests for single-value detection (v5 enrichments).
     *
     * @since 5.0.0
     */
    @Nested
    @DisplayName("Single-Value Detection (v5)")
    class SingleValueDetection {

        @Test
        @DisplayName("should detect single-value ValueObject with one field")
        void shouldDetectSingleValueWithOneField() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.OrderId")
                    .form(JavaForm.RECORD)
                    .build();

            FieldNode valueField = FieldNode.builder()
                    .simpleName("value")
                    .declaringTypeName("com.example.OrderId")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContextWithFields(typeNode, classification, List.of(valueField));

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.isSingleValue()).isTrue();
        }

        @Test
        @DisplayName("should return wrapped field for single-value ValueObject")
        void shouldReturnWrappedFieldForSingleValue() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.CustomerId")
                    .form(JavaForm.RECORD)
                    .build();

            FieldNode valueField = FieldNode.builder()
                    .simpleName("id")
                    .declaringTypeName("com.example.CustomerId")
                    .type(TypeRef.of("java.util.UUID"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContextWithFields(typeNode, classification, List.of(valueField));

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.wrappedField()).isPresent();
            assertThat(valueObject.wrappedField().get().name()).isEqualTo("id");
            assertThat(valueObject.wrappedField().get().type().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should not be single-value when ValueObject has multiple fields")
        void shouldNotBeSingleValueWithMultipleFields() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Money")
                    .form(JavaForm.RECORD)
                    .build();

            FieldNode amountField = FieldNode.builder()
                    .simpleName("amount")
                    .declaringTypeName("com.example.Money")
                    .type(TypeRef.of("java.math.BigDecimal"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            FieldNode currencyField = FieldNode.builder()
                    .simpleName("currency")
                    .declaringTypeName("com.example.Money")
                    .type(TypeRef.of("java.util.Currency"))
                    .modifiers(Set.of(JavaModifier.PRIVATE, JavaModifier.FINAL))
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context =
                    createContextWithFields(typeNode, classification, List.of(amountField, currencyField));

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.isSingleValue()).isFalse();
        }

        @Test
        @DisplayName("should return empty wrapped field for multi-value ValueObject")
        void shouldReturnEmptyWrappedFieldForMultiValue() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Address")
                    .form(JavaForm.RECORD)
                    .build();

            FieldNode streetField = FieldNode.builder()
                    .simpleName("street")
                    .declaringTypeName("com.example.Address")
                    .type(TypeRef.of("java.lang.String"))
                    .build();

            FieldNode cityField = FieldNode.builder()
                    .simpleName("city")
                    .declaringTypeName("com.example.Address")
                    .type(TypeRef.of("java.lang.String"))
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContextWithFields(typeNode, classification, List.of(streetField, cityField));

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.wrappedField()).isEmpty();
        }

        @Test
        @DisplayName("should not be single-value when ValueObject has no fields")
        void shouldNotBeSingleValueWithNoFields() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.EmptyVO")
                    .form(JavaForm.RECORD)
                    .build();

            ClassificationResult classification = createClassification(typeNode, "VALUE_OBJECT");
            BuilderContext context = createContext(typeNode, classification);

            ValueObject valueObject = builder.build(typeNode, classification, context);

            assertThat(valueObject.isSingleValue()).isFalse();
            assertThat(valueObject.wrappedField()).isEmpty();
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
                "VALUE_OBJECT",
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

    private BuilderContext createContextWithFields(
            TypeNode typeNode, ClassificationResult classification, List<FieldNode> fields) {
        ClassificationResults results = new ClassificationResults(Map.of(typeNode.id(), classification));
        return BuilderContext.of(new TestGraphQueryWithMembers(fields, List.of(), List.of()), results);
    }

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
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
