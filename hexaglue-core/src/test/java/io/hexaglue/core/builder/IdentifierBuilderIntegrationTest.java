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

import io.hexaglue.arch.model.Identifier;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationResults;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link IdentifierBuilder} using the real Spoon pipeline.
 *
 * <p>Verifies that the wrappedType detected by IdentifierBuilder matches the actual
 * type declared in the source code, when the full Spoon → GraphBuilder → IdentifierBuilder
 * pipeline is used (no mocks).
 *
 * @since 5.0.0
 */
@DisplayName("IdentifierBuilder Integration (real Spoon pipeline)")
class IdentifierBuilderIntegrationTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder graphBuilder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        graphBuilder = new GraphBuilder(true, analyzer);
    }

    @Test
    @DisplayName("should resolve UUID field type in record identifier via real Spoon pipeline")
    void shouldResolveUuidFieldTypeInRecordIdentifier() throws IOException {
        // Given: A record identifier wrapping UUID
        writeSource("com/example/domain/OrderId.java", """
                package com.example.domain;

                import java.util.UUID;

                public record OrderId(UUID value) {}
                """);

        ApplicationGraph graph = buildGraph("com.example");
        TypeNode orderIdNode = graph.typeNode("com.example.domain.OrderId").orElseThrow();

        // When: We query the fields of the identifier
        GraphQuery query = graph.query();
        List<FieldNode> fields = query.fieldsOf(orderIdNode);

        // Then: The first field should have type java.util.UUID
        assertThat(fields).isNotEmpty();
        FieldNode firstField = fields.get(0);
        assertThat(firstField.simpleName()).isEqualTo("value");
        assertThat(firstField.type().rawQualifiedName())
                .as("wrappedType should be java.util.UUID, not java.lang.Long or java.lang.Object")
                .isEqualTo("java.util.UUID");
    }

    @Test
    @DisplayName("should resolve Long field type in record identifier via real Spoon pipeline")
    void shouldResolveLongFieldTypeInRecordIdentifier() throws IOException {
        // Given: A record identifier wrapping Long
        writeSource("com/example/domain/UserId.java", """
                package com.example.domain;

                public record UserId(Long value) {}
                """);

        ApplicationGraph graph = buildGraph("com.example");
        TypeNode userIdNode = graph.typeNode("com.example.domain.UserId").orElseThrow();

        // When
        GraphQuery query = graph.query();
        List<FieldNode> fields = query.fieldsOf(userIdNode);

        // Then
        assertThat(fields).isNotEmpty();
        FieldNode firstField = fields.get(0);
        assertThat(firstField.type().rawQualifiedName()).isEqualTo("java.lang.Long");
    }

    @Test
    @DisplayName("should resolve UUID field type in record identifier with compact constructor and factory")
    void shouldResolveUuidInRecordWithCompactConstructorAndFactory() throws IOException {
        // Given: A record identifier matching case-study-ecommerce pattern exactly
        writeSource("com/example/domain/OrderId.java", """
                package com.example.domain;

                import java.util.UUID;

                public record OrderId(UUID value) {

                    public OrderId {
                        if (value == null) {
                            throw new IllegalArgumentException("OrderId value must not be null");
                        }
                    }

                    public static OrderId generate() {
                        return new OrderId(UUID.randomUUID());
                    }
                }
                """);

        ApplicationGraph graph = buildGraph("com.example");
        TypeNode orderIdNode = graph.typeNode("com.example.domain.OrderId").orElseThrow();

        // When: We query the fields of the identifier
        GraphQuery query = graph.query();
        List<FieldNode> fields = query.fieldsOf(orderIdNode);

        // Then: The first field should have type java.util.UUID
        assertThat(fields).isNotEmpty();
        FieldNode firstField = fields.get(0);
        assertThat(firstField.simpleName()).isEqualTo("value");
        assertThat(firstField.type().rawQualifiedName())
                .as("wrappedType should be java.util.UUID, not java.lang.Long or java.lang.Object")
                .isEqualTo("java.util.UUID");
    }

    @Test
    @DisplayName("should resolve String field type in record identifier via real Spoon pipeline")
    void shouldResolveStringFieldTypeInRecordIdentifier() throws IOException {
        // Given: A record identifier wrapping String
        writeSource("com/example/domain/TransactionId.java", """
                package com.example.domain;

                public record TransactionId(String value) {}
                """);

        ApplicationGraph graph = buildGraph("com.example");
        TypeNode txIdNode = graph.typeNode("com.example.domain.TransactionId").orElseThrow();

        // When
        GraphQuery query = graph.query();
        List<FieldNode> fields = query.fieldsOf(txIdNode);

        // Then
        assertThat(fields).isNotEmpty();
        FieldNode firstField = fields.get(0);
        assertThat(firstField.type().rawQualifiedName()).isEqualTo("java.lang.String");
    }

    @Nested
    @DisplayName("Full pipeline (Spoon → Graph → Classification → ModelBuilder)")
    class FullPipelineTests {

        @Test
        @DisplayName("should produce Identifier with UUID wrappedType through full pipeline")
        void shouldProduceIdentifierWithUuidWrappedTypeThroughFullPipeline() throws IOException {
            // Given: A record identifier wrapping UUID (case-study-ecommerce pattern)
            writeSource("com/example/domain/OrderId.java", """
                    package com.example.domain;

                    import java.util.UUID;

                    public record OrderId(UUID value) {

                        public OrderId {
                            if (value == null) {
                                throw new IllegalArgumentException("OrderId value must not be null");
                            }
                        }

                        public static OrderId generate() {
                            return new OrderId(UUID.randomUUID());
                        }
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example");
            GraphQuery query = graph.query();

            // Simulate classification result as IDENTIFIER
            NodeId nodeId = NodeId.type("com.example.domain.OrderId");
            ClassificationResult classification = ClassificationResult.classified(
                    nodeId,
                    ClassificationTarget.DOMAIN,
                    "IDENTIFIER",
                    ConfidenceLevel.HIGH,
                    "single-field-record",
                    90,
                    "Record with single UUID field",
                    List.of(),
                    List.of());

            ClassificationResults results = new ClassificationResults(Map.of(nodeId, classification));

            // When: Build through the full NewArchitecturalModelBuilder pipeline
            NewArchitecturalModelBuilder modelBuilder = new NewArchitecturalModelBuilder();
            NewArchitecturalModelBuilder.Result result = modelBuilder.build(query, results);

            // Then: The identifier wrappedType should be java.util.UUID
            assertThat(result.typeRegistry().size()).isEqualTo(1);
            List<Identifier> identifiers =
                    result.typeRegistry().all(Identifier.class).toList();
            assertThat(identifiers).hasSize(1);

            Identifier identifier = identifiers.get(0);
            assertThat(identifier.id().qualifiedName()).isEqualTo("com.example.domain.OrderId");
            assertThat(identifier.wrappedType().qualifiedName())
                    .as("wrappedType should be java.util.UUID")
                    .isEqualTo("java.util.UUID");
        }
    }

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, basePackage, false, false);
        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of(basePackage, 17, (int) model.types().size());
        return graphBuilder.build(model, metadata);
    }
}
