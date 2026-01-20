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

import io.hexaglue.arch.ClassificationTrace;
import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeNature;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BuilderContext}.
 *
 * @since 4.1.0
 */
@DisplayName("BuilderContext")
class BuilderContextTest {

    private GraphQuery graphQuery;
    private ClassificationResults classificationResults;

    @BeforeEach
    void setUp() {
        graphQuery = new TestGraphQuery();
        classificationResults = new ClassificationResults(Map.of());
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with required parameters")
        void shouldCreateWithRequiredParameters() {
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults);

            assertThat(context.graphQuery()).isSameAs(graphQuery);
            assertThat(context.classificationResults()).isSameAs(classificationResults);
            assertThat(context.builtTypes()).isEmpty();
        }

        @Test
        @DisplayName("should create with built types")
        void shouldCreateWithBuiltTypes() {
            Map<String, ArchType> builtTypes = new HashMap<>();
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD).build();
            ClassificationTrace trace =
                    ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "Test classification");
            ValueObject vo = new ValueObject(TypeId.of("com.example.Money"), structure, trace);
            builtTypes.put("com.example.Money", vo);

            BuilderContext context = BuilderContext.of(graphQuery, classificationResults, builtTypes);

            assertThat(context.builtTypes()).hasSize(1);
            assertThat(context.builtTypes()).containsKey("com.example.Money");
        }

        @Test
        @DisplayName("should throw when graphQuery is null")
        void shouldThrowWhenGraphQueryIsNull() {
            assertThatThrownBy(() -> BuilderContext.of(null, classificationResults))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("graphQuery");
        }

        @Test
        @DisplayName("should throw when classificationResults is null")
        void shouldThrowWhenClassificationResultsIsNull() {
            assertThatThrownBy(() -> BuilderContext.of(graphQuery, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("classificationResults");
        }

        @Test
        @DisplayName("should handle null builtTypes by creating empty map")
        void shouldHandleNullBuiltTypes() {
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults, null);

            assertThat(context.builtTypes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should return defensive copy of builtTypes")
        void shouldReturnDefensiveCopyOfBuiltTypes() {
            Map<String, ArchType> original = new HashMap<>();
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults, original);

            Map<String, ArchType> returned = context.builtTypes();

            assertThatThrownBy(() -> returned.put("test", null)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Classification Lookup")
    class ClassificationLookup {

        @Test
        @DisplayName("should return classification by qualified name")
        void shouldReturnClassificationByQualifiedName() {
            NodeId nodeId = NodeId.type("com.example.Order");
            ClassificationResult result = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test justification",
                    List.of(),
                    List.of());

            classificationResults = new ClassificationResults(Map.of(nodeId, result));
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults);

            Optional<ClassificationResult> found = context.getClassification("com.example.Order");

            assertThat(found).isPresent();
            assertThat(found.get().kind()).isEqualTo("AGGREGATE_ROOT");
        }

        @Test
        @DisplayName("should return empty when classification not found")
        void shouldReturnEmptyWhenClassificationNotFound() {
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults);

            Optional<ClassificationResult> found = context.getClassification("com.example.NotFound");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Built Type Lookup")
    class BuiltTypeLookup {

        @Test
        @DisplayName("should return built type by qualified name")
        void shouldReturnBuiltTypeByQualifiedName() {
            Map<String, ArchType> builtTypes = new HashMap<>();
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD).build();
            ClassificationTrace trace =
                    ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "Test classification");
            ValueObject vo = new ValueObject(TypeId.of("com.example.Money"), structure, trace);
            builtTypes.put("com.example.Money", vo);

            BuilderContext context = BuilderContext.of(graphQuery, classificationResults, builtTypes);

            Optional<ArchType> found = context.getBuiltType("com.example.Money");

            assertThat(found).isPresent();
            assertThat(found.get()).isInstanceOf(ValueObject.class);
        }

        @Test
        @DisplayName("should return empty when built type not found")
        void shouldReturnEmptyWhenBuiltTypeNotFound() {
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults);

            Optional<ArchType> found = context.getBuiltType("com.example.NotFound");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Kind Checking")
    class KindChecking {

        @Test
        @DisplayName("should check if type is classified as specific kind")
        void shouldCheckIfTypeIsClassifiedAsSpecificKind() {
            NodeId nodeId = NodeId.type("com.example.Order");
            ClassificationResult result = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "AGGREGATE_ROOT",
                    io.hexaglue.core.classification.ConfidenceLevel.HIGH,
                    "test-criterion",
                    80,
                    "Test justification",
                    List.of(),
                    List.of());

            classificationResults = new ClassificationResults(Map.of(nodeId, result));
            BuilderContext context = BuilderContext.of(graphQuery, classificationResults);

            assertThat(context.isClassifiedAs("com.example.Order", "AGGREGATE_ROOT"))
                    .isTrue();
            assertThat(context.isClassifiedAs("com.example.Order", "ENTITY")).isFalse();
            assertThat(context.isClassifiedAs("com.example.NotFound", "AGGREGATE_ROOT"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("With Methods")
    class WithMethods {

        @Test
        @DisplayName("should create new context with additional built type")
        void shouldCreateNewContextWithAdditionalBuiltType() {
            BuilderContext original = BuilderContext.of(graphQuery, classificationResults);
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD).build();
            ClassificationTrace trace =
                    ClassificationTrace.highConfidence(ElementKind.VALUE_OBJECT, "test", "Test classification");
            ValueObject vo = new ValueObject(TypeId.of("com.example.Money"), structure, trace);

            BuilderContext updated = original.withBuiltType("com.example.Money", vo);

            assertThat(original.builtTypes()).isEmpty();
            assertThat(updated.builtTypes()).hasSize(1);
            assertThat(updated.getBuiltType("com.example.Money")).isPresent();
        }
    }
}
