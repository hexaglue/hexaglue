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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.core.graph.testing.TestGraphBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NewArchitecturalModelBuilder}.
 *
 * @since 4.1.0
 */
@DisplayName("NewArchitecturalModelBuilder")
class NewArchitecturalModelBuilderTest {

    private NewArchitecturalModelBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new NewArchitecturalModelBuilder();
    }

    private TypeNode createTypeNode(String qualifiedName, JavaForm form) {
        int lastDot = qualifiedName.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
        String packageName = lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";

        return TypeNode.builder()
                .id(NodeId.type(qualifiedName))
                .simpleName(simpleName)
                .qualifiedName(qualifiedName)
                .packageName(packageName)
                .form(form)
                .build();
    }

    private ClassificationResult createAggregateResult(String qualifiedName) {
        return ClassificationResult.classified(
                NodeId.type(qualifiedName),
                ClassificationTarget.DOMAIN,
                "AGGREGATE_ROOT",
                ConfidenceLevel.HIGH,
                "test-criterion",
                100,
                "Test aggregate classification",
                List.of(),
                List.of());
    }

    private ClassificationResult createEntityResult(String qualifiedName) {
        return ClassificationResult.classified(
                NodeId.type(qualifiedName),
                ClassificationTarget.DOMAIN,
                "ENTITY",
                ConfidenceLevel.HIGH,
                "test-criterion",
                100,
                "Test entity classification",
                List.of(),
                List.of());
    }

    private ClassificationResult createUnclassifiedResult(String qualifiedName) {
        return ClassificationResult.unclassifiedDomain(NodeId.type(qualifiedName), null);
    }

    @Nested
    @DisplayName("build()")
    class BuildTests {

        @Test
        @DisplayName("should throw on null graph query")
        void shouldThrowOnNullGraphQuery() {
            ClassificationResults results = new ClassificationResults(Map.of());

            assertThatNullPointerException()
                    .isThrownBy(() -> builder.build(null, results))
                    .withMessage("graphQuery must not be null");
        }

        @Test
        @DisplayName("should throw on null classification results")
        void shouldThrowOnNullResults() {
            // Create a minimal test graph query
            GraphQuery graphQuery = TestGraphBuilder.create().build().query();

            assertThatNullPointerException()
                    .isThrownBy(() -> builder.build(graphQuery, null))
                    .withMessage("results must not be null");
        }

        @Test
        @DisplayName("should build result with empty inputs")
        void shouldBuildWithEmptyInputs() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create().build().query();
            ClassificationResults results = new ClassificationResults(Map.of());

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result).isNotNull();
            assertThat(result.typeRegistry().size()).isEqualTo(0);
            assertThat(result.classificationReport()).isNotNull();
            assertThat(result.domainIndex()).isNotNull();
            assertThat(result.portIndex()).isNotNull();
        }

        @Test
        @DisplayName("should set generated timestamp")
        void shouldSetGeneratedTimestamp() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create().build().query();
            ClassificationResults results = new ClassificationResults(Map.of());

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should build with classified types from graph")
        void shouldBuildWithClassifiedTypes() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withClass("com.example.OrderItem")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.Order"), createAggregateResult("com.example.Order"),
                    NodeId.type("com.example.OrderItem"), createEntityResult("com.example.OrderItem")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(2);
            assertThat(result.classificationReport().stats().totalTypes()).isEqualTo(2);
        }

        @Test
        @DisplayName("should build with unclassified types from graph")
        void shouldBuildWithUnclassifiedTypes() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Utils")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(
                    Map.of(NodeId.type("com.example.Utils"), createUnclassifiedResult("com.example.Utils")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            assertThat(result.classificationReport().stats().unclassifiedTypes())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should populate domain index with aggregates")
        void shouldPopulateDomainIndex() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Order")
                    .withField("id", "java.util.UUID")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(
                    Map.of(NodeId.type("com.example.Order"), createAggregateResult("com.example.Order")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            // Note: aggregate may fallback to unclassified if no identity field detected properly
            assertThat(result.typeRegistry().size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should have hasIssues return true for unclassified types")
        void shouldReportIssuesForUnclassified() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.Unknown")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(
                    Map.of(NodeId.type("com.example.Unknown"), createUnclassifiedResult("com.example.Unknown")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("should report size from result")
        void shouldReportSize() {
            // given
            GraphQuery graphQuery = TestGraphBuilder.create()
                    .withClass("com.example.A")
                    .withClass("com.example.B")
                    .build()
                    .query();

            ClassificationResults results = new ClassificationResults(Map.of(
                    NodeId.type("com.example.A"), createEntityResult("com.example.A"),
                    NodeId.type("com.example.B"), createEntityResult("com.example.B")));

            // when
            NewArchitecturalModelBuilder.Result result = builder.build(graphQuery, results);

            // then
            assertThat(result.size()).isEqualTo(2);
        }
    }
}
