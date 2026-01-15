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

package io.hexaglue.core.classification;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.spi.core.ClassificationConfig;
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
 * Integration tests for SinglePassClassifier with ClassificationConfig.
 *
 * <p>Tests verify that:
 * <ul>
 *   <li>Exclusion patterns skip types from classification</li>
 *   <li>Explicit classifications override criteria-based classification</li>
 * </ul>
 */
class ClassificationConfigIntegrationTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
    }

    @Nested
    @DisplayName("Exclusion Patterns")
    class ExclusionPatternsTest {

        @Test
        @DisplayName("Types matching exclude patterns should be skipped")
        void typesMatchingExcludePatternsShouldBeSkipped() throws IOException {
            // Given: Source with an exception class
            writeSource("com/example/domain/Order.java", """
                    package com.example.domain;

                    public class Order {
                        private String id;
                    }
                    """);

            writeSource("com/example/domain/OrderException.java", """
                    package com.example.domain;

                    public class OrderException extends RuntimeException {
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example");

            // When: Classify with exclusion pattern for exceptions
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("**.*Exception"))
                    .build();

            SinglePassClassifier classifier = new SinglePassClassifier();
            ClassificationResults results = classifier.classify(graph, config);

            // Then: Order should be classified but OrderException should be excluded
            NodeId orderId = NodeId.type("com.example.domain.Order");
            NodeId exceptionId = NodeId.type("com.example.domain.OrderException");

            assertThat(results.get(orderId)).isPresent();
            assertThat(results.get(exceptionId)).isEmpty();
        }

        @Test
        @DisplayName("Multiple exclusion patterns should all be applied")
        void multipleExclusionPatternsShouldAllBeApplied() throws IOException {
            // Given: Source with various types
            writeSource("com/example/Order.java", """
                    package com.example;

                    public class Order {}
                    """);

            writeSource("com/example/OrderException.java", """
                    package com.example;

                    public class OrderException extends RuntimeException {}
                    """);

            writeSource("com/example/OrderCreatedEvent.java", """
                    package com.example;

                    public class OrderCreatedEvent {}
                    """);

            ApplicationGraph graph = buildGraph("com.example");

            // When: Classify with multiple exclusion patterns
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("**.*Exception", "**.*Event"))
                    .build();

            SinglePassClassifier classifier = new SinglePassClassifier();
            ClassificationResults results = classifier.classify(graph, config);

            // Then: Only Order should be classified
            assertThat(results.get(NodeId.type("com.example.Order"))).isPresent();
            assertThat(results.get(NodeId.type("com.example.OrderException"))).isEmpty();
            assertThat(results.get(NodeId.type("com.example.OrderCreatedEvent")))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Explicit Classifications")
    class ExplicitClassificationsTest {

        @Test
        @DisplayName("Explicit classification should override criteria-based classification")
        void explicitClassificationShouldOverrideCriteriaBased() throws IOException {
            // Given: A class that would normally be classified as something else
            writeSource("com/example/OrderDetails.java", """
                    package com.example;

                    public class OrderDetails {
                        private String name;
                        private int quantity;
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example");

            // When: Classify with explicit configuration for this type
            ClassificationConfig config = ClassificationConfig.builder()
                    .explicitClassifications(Map.of("com.example.OrderDetails", "VALUE_OBJECT"))
                    .build();

            SinglePassClassifier classifier = new SinglePassClassifier();
            ClassificationResults results = classifier.classify(graph, config);

            // Then: Type should be classified as VALUE_OBJECT with high confidence
            var result = results.get(NodeId.type("com.example.OrderDetails"));
            assertThat(result).isPresent();
            assertThat(result.get().kind()).isEqualTo("VALUE_OBJECT");
            assertThat(result.get().matchedCriteria()).isEqualTo("ExplicitConfiguration");
            assertThat(result.get().confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.get().matchedPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("Explicit classification should have highest priority")
        void explicitClassificationShouldHaveHighestPriority() throws IOException {
            // Given: A class with jMolecules annotation (would be AGGREGATE_ROOT)
            writeSource("com/example/Order.java", """
                    package com.example;

                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Order {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example");

            // When: Classify with explicit override to ENTITY
            ClassificationConfig config = ClassificationConfig.builder()
                    .explicitClassifications(Map.of("com.example.Order", "ENTITY"))
                    .build();

            SinglePassClassifier classifier = new SinglePassClassifier();
            ClassificationResults results = classifier.classify(graph, config);

            // Then: Explicit classification wins over annotation
            var result = results.get(NodeId.type("com.example.Order"));
            assertThat(result).isPresent();
            assertThat(result.get().kind()).isEqualTo("ENTITY");
        }
    }

    @Nested
    @DisplayName("Combined Configuration")
    class CombinedConfigurationTest {

        @Test
        @DisplayName("Exclusions and explicit classifications should work together")
        void exclusionsAndExplicitsShouldWorkTogether() throws IOException {
            // Given: Multiple types
            writeSource("com/example/Order.java", """
                    package com.example;

                    public class Order {
                        private String id;
                    }
                    """);

            writeSource("com/example/OrderId.java", """
                    package com.example;

                    public record OrderId(String value) {}
                    """);

            writeSource("com/example/OrderCreatedException.java", """
                    package com.example;

                    public class OrderCreatedException extends RuntimeException {}
                    """);

            ApplicationGraph graph = buildGraph("com.example");

            // When: Classify with both exclusions and explicit classifications
            ClassificationConfig config = ClassificationConfig.builder()
                    .excludePatterns(List.of("**.*Exception"))
                    .explicitClassifications(Map.of("com.example.OrderId", "VALUE_OBJECT"))
                    .build();

            SinglePassClassifier classifier = new SinglePassClassifier();
            ClassificationResults results = classifier.classify(graph, config);

            // Then: Exception excluded, OrderId explicitly classified, Order normally classified
            assertThat(results.get(NodeId.type("com.example.Order"))).isPresent();
            assertThat(results.get(NodeId.type("com.example.OrderCreatedException")))
                    .isEmpty();

            var orderIdResult = results.get(NodeId.type("com.example.OrderId"));
            assertThat(orderIdResult).isPresent();
            assertThat(orderIdResult.get().kind()).isEqualTo("VALUE_OBJECT");
            assertThat(orderIdResult.get().matchedCriteria()).isEqualTo("ExplicitConfiguration");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void writeSource(String relativePath, String source) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, source);
    }

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 21, basePackage);
        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata = GraphMetadata.of(basePackage, 21, model.types().size());
        return builder.build(model, metadata);
    }
}
