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
import io.hexaglue.arch.model.UnclassifiedType;
import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationStatus;
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
 * Tests for {@link UnclassifiedTypeBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("UnclassifiedTypeBuilder")
class UnclassifiedTypeBuilderTest {

    private UnclassifiedTypeBuilder builder;

    @BeforeEach
    void setUp() {
        FieldRoleDetector fieldRoleDetector = new FieldRoleDetector();
        MethodRoleDetector methodRoleDetector = new MethodRoleDetector();
        TypeStructureBuilder typeStructureBuilder = new TypeStructureBuilder(fieldRoleDetector, methodRoleDetector);
        ClassificationTraceConverter traceConverter = new ClassificationTraceConverter();
        UnclassifiedCategoryDetector categoryDetector = new UnclassifiedCategoryDetector();
        builder = new UnclassifiedTypeBuilder(typeStructureBuilder, traceConverter, categoryDetector);
    }

    @Nested
    @DisplayName("Build UnclassifiedType")
    class BuildUnclassifiedType {

        @Test
        @DisplayName("should build UnclassifiedType with correct id")
        void shouldBuildUnclassifiedTypeWithCorrectId() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.SomeClass")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.id().qualifiedName()).isEqualTo("com.example.SomeClass");
        }

        @Test
        @DisplayName("should build UnclassifiedType with correct kind")
        void shouldBuildUnclassifiedTypeWithCorrectKind() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.UnknownType")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.kind()).isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("should build UnclassifiedType with structure")
        void shouldBuildUnclassifiedTypeWithStructure() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.Misc")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.structure()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Category Detection")
    class CategoryDetection {

        @Test
        @DisplayName("should detect UTILITY category for Utils class")
        void shouldDetectUtilityCategoryForUtilsClass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.StringUtils")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.category()).isEqualTo(UnclassifiedCategory.UTILITY);
        }

        @Test
        @DisplayName("should detect UTILITY category for Helper class")
        void shouldDetectUtilityCategoryForHelperClass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.DateHelper")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.category()).isEqualTo(UnclassifiedCategory.UTILITY);
        }

        @Test
        @DisplayName("should detect OUT_OF_SCOPE category for test class")
        void shouldDetectOutOfScopeCategoryForTestClass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.test.OrderServiceTest")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.category()).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }

        @Test
        @DisplayName("should detect UNKNOWN category for generic class")
        void shouldDetectUnknownCategoryForGenericClass() {
            TypeNode typeNode = TypeNode.builder()
                    .qualifiedName("com.example.SomeRandomClass")
                    .form(JavaForm.CLASS)
                    .build();

            ClassificationResult classification = createUnclassifiedResult(typeNode);
            BuilderContext context = createContext(typeNode, classification);

            UnclassifiedType unclassified = builder.build(typeNode, classification, context);

            assertThat(unclassified.category()).isEqualTo(UnclassifiedCategory.UNKNOWN);
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

    private ClassificationResult createUnclassifiedResult(TypeNode typeNode) {
        return new ClassificationResult(
                typeNode.id(),
                ClassificationTarget.DOMAIN,
                "UNCLASSIFIED",
                ConfidenceLevel.LOW,
                null,
                0,
                "Could not classify",
                List.of(),
                List.of(),
                null,
                ClassificationStatus.UNCLASSIFIED,
                null);
    }

    private ClassificationResult createClassificationForNullCheck() {
        NodeId nodeId = NodeId.type("com.example.Test");
        return new ClassificationResult(
                nodeId,
                ClassificationTarget.DOMAIN,
                "UNCLASSIFIED",
                ConfidenceLevel.LOW,
                null,
                0,
                "Could not classify",
                List.of(),
                List.of(),
                null,
                ClassificationStatus.UNCLASSIFIED,
                null);
    }

    private BuilderContext createContext(TypeNode typeNode, ClassificationResult classification) {
        ClassificationResults results = new ClassificationResults(Map.of(typeNode.id(), classification));
        return BuilderContext.of(new TestGraphQuery(), results);
    }

    private BuilderContext createEmptyContext() {
        return BuilderContext.of(new TestGraphQuery(), new ClassificationResults(Map.of()));
    }
}
