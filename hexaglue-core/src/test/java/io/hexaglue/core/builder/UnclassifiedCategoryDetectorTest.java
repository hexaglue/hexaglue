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

import io.hexaglue.arch.model.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Conflict;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UnclassifiedCategoryDetector}.
 *
 * @since 4.1.0
 */
@DisplayName("UnclassifiedCategoryDetector")
class UnclassifiedCategoryDetectorTest {

    private UnclassifiedCategoryDetector detector;

    @BeforeEach
    void setUp() {
        detector = new UnclassifiedCategoryDetector();
    }

    @Nested
    @DisplayName("Conflicting Detection")
    class ConflictingDetection {

        @Test
        @DisplayName("should detect CONFLICTING when classification has conflicts")
        void shouldDetectConflictingWhenClassificationHasConflicts() {
            TypeNode typeNode = createTypeNode("com.example.Order");
            ClassificationResult result = createConflictResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.CONFLICTING);
        }
    }

    @Nested
    @DisplayName("Out of Scope Detection")
    class OutOfScopeDetection {

        @Test
        @DisplayName("should detect OUT_OF_SCOPE for test package")
        void shouldDetectOutOfScopeForTestPackage() {
            TypeNode typeNode = createTypeNode("com.example.test.OrderTest");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }

        @Test
        @DisplayName("should detect OUT_OF_SCOPE for tests package")
        void shouldDetectOutOfScopeForTestsPackage() {
            TypeNode typeNode = createTypeNode("com.example.tests.OrderTests");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }

        @Test
        @DisplayName("should detect OUT_OF_SCOPE for mock package")
        void shouldDetectOutOfScopeForMockPackage() {
            TypeNode typeNode = createTypeNode("com.example.mock.MockOrder");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }

        @Test
        @DisplayName("should detect OUT_OF_SCOPE for stub package")
        void shouldDetectOutOfScopeForStubPackage() {
            TypeNode typeNode = createTypeNode("com.example.stub.OrderStub");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }
    }

    @Nested
    @DisplayName("Utility Detection")
    class UtilityDetection {

        @Test
        @DisplayName("should detect UTILITY for Utils suffix")
        void shouldDetectUtilityForUtilsSuffix() {
            TypeNode typeNode = createTypeNode("com.example.StringUtils");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.UTILITY);
        }

        @Test
        @DisplayName("should detect UTILITY for Util suffix")
        void shouldDetectUtilityForUtilSuffix() {
            TypeNode typeNode = createTypeNode("com.example.DateUtil");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.UTILITY);
        }

        @Test
        @DisplayName("should detect UTILITY for Helper suffix")
        void shouldDetectUtilityForHelperSuffix() {
            TypeNode typeNode = createTypeNode("com.example.OrderHelper");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.UTILITY);
        }

        @Test
        @DisplayName("should detect UTILITY for Constants suffix")
        void shouldDetectUtilityForConstantsSuffix() {
            TypeNode typeNode = createTypeNode("com.example.AppConstants");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.UTILITY);
        }
    }

    @Nested
    @DisplayName("Technical Detection")
    class TechnicalDetection {

        @Test
        @DisplayName("should detect TECHNICAL for @Configuration annotation")
        void shouldDetectTechnicalForConfigurationAnnotation() {
            TypeNode typeNode = createTypeNodeWithAnnotation(
                    "com.example.AppConfig", "org.springframework.context.annotation.Configuration");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.TECHNICAL);
        }

        @Test
        @DisplayName("should detect TECHNICAL for @Component annotation")
        void shouldDetectTechnicalForComponentAnnotation() {
            TypeNode typeNode =
                    createTypeNodeWithAnnotation("com.example.MyComponent", "org.springframework.stereotype.Component");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.TECHNICAL);
        }
    }

    @Nested
    @DisplayName("Ambiguous Detection")
    class AmbiguousDetection {

        @Test
        @DisplayName("should detect AMBIGUOUS when classification has evidence but not classified")
        void shouldDetectAmbiguousWhenHasEvidence() {
            TypeNode typeNode = createTypeNode("com.example.SomeClass");
            ClassificationResult result = createUnclassifiedResultWithEvidence(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.AMBIGUOUS);
        }
    }

    @Nested
    @DisplayName("Unknown Detection")
    class UnknownDetection {

        @Test
        @DisplayName("should detect UNKNOWN when no other category matches")
        void shouldDetectUnknownAsDefault() {
            TypeNode typeNode = createTypeNode("com.example.SomeClass");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Priority Order")
    class PriorityOrder {

        @Test
        @DisplayName("should prioritize CONFLICTING over other categories")
        void shouldPrioritizeConflicting() {
            // A utility class with conflicts
            TypeNode typeNode = createTypeNode("com.example.StringUtils");
            ClassificationResult result = createConflictResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.CONFLICTING);
        }

        @Test
        @DisplayName("should prioritize OUT_OF_SCOPE over UTILITY")
        void shouldPrioritizeOutOfScopeOverUtility() {
            // A test utility class
            TypeNode typeNode = createTypeNode("com.example.test.TestUtils");
            ClassificationResult result = createUnclassifiedResult(typeNode);

            UnclassifiedCategory category = detector.detect(typeNode, result);

            assertThat(category).isEqualTo(UnclassifiedCategory.OUT_OF_SCOPE);
        }
    }

    // Helper methods

    private TypeNode createTypeNode(String qualifiedName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .build();
    }

    private TypeNode createTypeNodeWithAnnotation(String qualifiedName, String annotationName) {
        return TypeNode.builder()
                .qualifiedName(qualifiedName)
                .form(JavaForm.CLASS)
                .annotations(List.of(AnnotationRef.of(annotationName)))
                .build();
    }

    private ClassificationResult createUnclassifiedResult(TypeNode typeNode) {
        return ClassificationResult.unclassifiedDomain(typeNode.id(), null);
    }

    private ClassificationResult createUnclassifiedResultWithEvidence(TypeNode typeNode) {
        return new ClassificationResult(
                typeNode.id(),
                null,
                null,
                null,
                null,
                0,
                null,
                List.of(Evidence.fromNaming("*Service", "SomeClass")),
                List.of(),
                null,
                ClassificationStatus.UNCLASSIFIED,
                null);
    }

    private ClassificationResult createConflictResult(TypeNode typeNode) {
        return ClassificationResult.conflictDomain(
                typeNode.id(),
                List.of(Conflict.error("ENTITY", "test-criterion", ConfidenceLevel.HIGH, 75, "Conflicting signals")));
    }
}
