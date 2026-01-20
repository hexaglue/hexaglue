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
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import java.util.Map;
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
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector);
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

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }
}
