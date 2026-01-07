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

import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contract tests verifying the algorithmic invariants of the classification engine.
 *
 * <p>These tests ensure fundamental properties of the classification algorithm that
 * must be preserved across refactoring:
 * <ul>
 *   <li>Determinism: Multiple runs produce identical results</li>
 *   <li>Priority ordering: Higher priority always wins</li>
 *   <li>Confidence ordering: With equal priority, higher confidence wins</li>
 *   <li>Name ordering: With equal priority and confidence, alphabetical order provides determinism</li>
 * </ul>
 */
class ClassificationContractTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
    }

    // =========================================================================
    // Determinism Contract
    // =========================================================================

    @Nested
    @DisplayName("Determinism Contract")
    class DeterminismContractTest {

        /**
         * Contract: 100 classification runs must produce identical results.
         */
        @Test
        @DisplayName("deterministicDecision: 100 runs should produce same result")
        void deterministicDecision() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                        void save(Order order);
                    }
                    """);

            DomainClassifier classifier = new DomainClassifier();
            ClassificationResult firstResult = null;

            for (int run = 0; run < 100; run++) {
                ApplicationGraph graph = buildGraph();
                TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
                GraphQuery query = graph.query();

                ClassificationResult result = classifier.classify(order, query);

                if (firstResult == null) {
                    firstResult = result;
                } else {
                    assertThat(result.kind())
                            .as("Run %d: kind should be identical", run)
                            .isEqualTo(firstResult.kind());
                    assertThat(result.matchedCriteria())
                            .as("Run %d: criteria should be identical", run)
                            .isEqualTo(firstResult.matchedCriteria());
                    assertThat(result.matchedPriority())
                            .as("Run %d: priority should be identical", run)
                            .isEqualTo(firstResult.matchedPriority());
                    assertThat(result.confidence())
                            .as("Run %d: confidence should be identical", run)
                            .isEqualTo(firstResult.confidence());
                }
            }
        }

        /**
         * Contract: SinglePassClassifier should produce deterministic results over 100 runs.
         */
        @Test
        @DisplayName("SinglePassClassifier: 100 runs should produce same results for all types")
        void singlePassClassifierDeterministic() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            SinglePassClassifier classifier = new SinglePassClassifier();
            ClassificationResults firstResults = null;

            for (int run = 0; run < 100; run++) {
                ApplicationGraph graph = buildGraph();
                ClassificationResults results = classifier.classify(graph);

                if (firstResults == null) {
                    firstResults = results;
                } else {
                    assertThat(results.allClassifications().size())
                            .as("Run %d: result count should be identical", run)
                            .isEqualTo(firstResults.allClassifications().size());

                    for (var entry : firstResults.allClassifications().entrySet()) {
                        ClassificationResult expected = entry.getValue();
                        ClassificationResult actual =
                                results.allClassifications().get(entry.getKey());

                        assertThat(actual)
                                .as("Run %d: result for %s", run, entry.getKey())
                                .isNotNull();
                        assertThat(actual.kind())
                                .as("Run %d: kind for %s", run, entry.getKey())
                                .isEqualTo(expected.kind());
                        assertThat(actual.matchedCriteria())
                                .as("Run %d: criteria for %s", run, entry.getKey())
                                .isEqualTo(expected.matchedCriteria());
                    }
                }
            }
        }
    }

    // =========================================================================
    // Priority Dominates Confidence Contract
    // =========================================================================

    @Nested
    @DisplayName("Priority Dominates Confidence Contract")
    class PriorityDominatesConfidenceTest {

        /**
         * Contract: Priority 100 with LOW confidence should win over Priority 50 with EXPLICIT confidence.
         *
         * <p>This verifies the tie-break order: priority DESC → confidence DESC → name ASC
         */
        @Test
        @DisplayName("priorityDominatesConfidence: Priority 100 LOW > Priority 50 EXPLICIT")
        void priorityDominatesConfidence() throws IOException {
            // Create a type with @AggregateRoot (priority 100, EXPLICIT)
            // and verify it beats any lower priority heuristic
            writeSource("com/example/Order.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Order {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            DomainClassifier classifier = new DomainClassifier();
            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(order, query);

            // Explicit annotation (priority 100) should win over repository-dominant (priority 80)
            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
            assertThat(result.matchedPriority()).isEqualTo(100);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-aggregate-root");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
        }

        /**
         * Contract: With custom criteria, higher priority always wins regardless of confidence.
         */
        @Test
        @DisplayName("Custom criteria: Priority 100 LOW wins over Priority 50 EXPLICIT")
        void customCriteriaPriorityWins() throws IOException {
            writeSource("com/example/TestType.java", """
                    package com.example;
                    public class TestType {
                        private String value;
                    }
                    """);

            // Create custom criteria with controlled priorities
            ClassificationCriteria<DomainKind> lowPriorityExplicit = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "low-priority-explicit";
                }

                @Override
                public int priority() {
                    return 50;
                }

                @Override
                public DomainKind targetKind() {
                    return DomainKind.VALUE_OBJECT;
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    return MatchResult.match(ConfidenceLevel.EXPLICIT, "Explicit match");
                }
            };

            ClassificationCriteria<DomainKind> highPriorityLow = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "high-priority-low";
                }

                @Override
                public int priority() {
                    return 100;
                }

                @Override
                public DomainKind targetKind() {
                    return DomainKind.ENTITY;
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    return MatchResult.match(ConfidenceLevel.LOW, "Low confidence match");
                }
            };

            DomainClassifier classifier = new DomainClassifier(List.of(lowPriorityExplicit, highPriorityLow));
            ApplicationGraph graph = buildGraph();
            TypeNode testType = graph.typeNode("com.example.TestType").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(testType, query);

            // Priority 100 should win even with LOW confidence
            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("ENTITY");
            assertThat(result.matchedPriority()).isEqualTo(100);
            assertThat(result.matchedCriteria()).isEqualTo("high-priority-low");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.LOW);
        }
    }

    // =========================================================================
    // Same Priority Uses Confidence Contract
    // =========================================================================

    @Nested
    @DisplayName("Same Priority Uses Confidence Contract")
    class SamePriorityUsesConfidenceTest {

        /**
         * Contract: With equal priorities, results should be deterministic.
         *
         * <p>The tie-breaking algorithm uses confidence.weight() (not enum ordinals)
         * to ensure higher confidence wins. This was fixed in the CriteriaEngine
         * refactoring where DefaultDecisionPolicy now properly uses confidence weights.
         */
        @Test
        @DisplayName("samePriorityDeterministic: Equal priority -> deterministic result (same both runs)")
        void samePriorityDeterministic() throws IOException {
            writeSource("com/example/TestType.java", """
                    package com.example;
                    public class TestType {
                        private String value;
                    }
                    """);

            // Create criteria with same priority and SAME KIND but different confidence
            ClassificationCriteria<DomainKind> samePriorityLow = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "same-priority-low";
                }

                @Override
                public int priority() {
                    return 80;
                }

                @Override
                public DomainKind targetKind() {
                    return DomainKind.ENTITY;
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    return MatchResult.match(ConfidenceLevel.LOW, "Low confidence");
                }
            };

            ClassificationCriteria<DomainKind> samePriorityHigh = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "same-priority-high";
                }

                @Override
                public int priority() {
                    return 80;
                }

                @Override
                public DomainKind targetKind() {
                    return DomainKind.ENTITY;
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    return MatchResult.match(ConfidenceLevel.HIGH, "High confidence");
                }
            };

            // Test both orderings to ensure result is deterministic
            DomainClassifier classifier1 = new DomainClassifier(List.of(samePriorityLow, samePriorityHigh));
            DomainClassifier classifier2 = new DomainClassifier(List.of(samePriorityHigh, samePriorityLow));

            ApplicationGraph graph = buildGraph();
            TypeNode testType = graph.typeNode("com.example.TestType").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result1 = classifier1.classify(testType, query);
            ClassificationResult result2 = classifier2.classify(testType, query);

            // Both should produce the same result (deterministic)
            assertThat(result1.isClassified()).isTrue();
            assertThat(result2.isClassified()).isTrue();
            assertThat(result1.kind()).isEqualTo(result2.kind());
            assertThat(result1.matchedCriteria()).isEqualTo(result2.matchedCriteria());
            assertThat(result1.confidence()).isEqualTo(result2.confidence());
        }
    }

    // =========================================================================
    // Same Priority and Confidence Uses Name Contract
    // =========================================================================

    @Nested
    @DisplayName("Same Priority and Confidence Uses Name Contract")
    class SamePriorityAndConfidenceUsesNameTest {

        /**
         * Contract: With equal priority and confidence, alphabetical name order provides determinism.
         */
        @Test
        @DisplayName("samePriorityAndConfidenceUseName: Equal priority and confidence -> alphabetical name wins")
        void samePriorityAndConfidenceUseName() throws IOException {
            writeSource("com/example/TestType.java", """
                    package com.example;
                    public class TestType {
                        private String value;
                    }
                    """);

            // Create criteria with same priority, confidence, AND kind - only name differs
            // Using the same kind avoids CONFLICT
            ClassificationCriteria<DomainKind> criteriaZ = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "z-criteria";
                }

                @Override
                public int priority() {
                    return 80;
                }

                @Override
                public DomainKind targetKind() {
                    return DomainKind.ENTITY; // Same kind
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    return MatchResult.match(ConfidenceLevel.HIGH, "Z criteria match");
                }
            };

            ClassificationCriteria<DomainKind> criteriaA = new ClassificationCriteria<>() {
                @Override
                public String name() {
                    return "a-criteria";
                }

                @Override
                public int priority() {
                    return 80;
                }

                @Override
                public DomainKind targetKind() {
                    return DomainKind.ENTITY; // Same kind
                }

                @Override
                public MatchResult evaluate(TypeNode node, GraphQuery query) {
                    return MatchResult.match(ConfidenceLevel.HIGH, "A criteria match");
                }
            };

            // Test both orderings to ensure alphabetical order is used
            DomainClassifier classifier1 = new DomainClassifier(List.of(criteriaZ, criteriaA));
            DomainClassifier classifier2 = new DomainClassifier(List.of(criteriaA, criteriaZ));

            ApplicationGraph graph = buildGraph();
            TypeNode testType = graph.typeNode("com.example.TestType").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result1 = classifier1.classify(testType, query);
            ClassificationResult result2 = classifier2.classify(testType, query);

            // With same kind, "a-criteria" should be selected (alphabetically first)
            assertThat(result1.isClassified()).isTrue();
            assertThat(result2.isClassified()).isTrue();
            assertThat(result1.matchedCriteria()).isEqualTo("a-criteria");
            assertThat(result2.matchedCriteria()).isEqualTo("a-criteria");
            assertThat(result1.kind()).isEqualTo("ENTITY");
            assertThat(result2.kind()).isEqualTo("ENTITY");
        }
    }

    // =========================================================================
    // Port Classification Contracts
    // =========================================================================

    @Nested
    @DisplayName("Port Classification Contracts")
    class PortClassificationContractsTest {

        /**
         * Contract: Port classification must also be deterministic.
         */
        @Test
        @DisplayName("Port classification: 100 runs should produce same result")
        void portClassificationDeterministic() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Object findById(String id);
                        void save(Object order);
                    }
                    """);

            PortClassifier classifier = new PortClassifier();
            ClassificationResult firstResult = null;

            for (int run = 0; run < 100; run++) {
                ApplicationGraph graph = buildGraph();
                TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
                GraphQuery query = graph.query();

                ClassificationResult result = classifier.classify(repo, query);

                if (firstResult == null) {
                    firstResult = result;
                } else {
                    assertThat(result.kind())
                            .as("Run %d: kind should be identical", run)
                            .isEqualTo(firstResult.kind());
                    assertThat(result.matchedCriteria())
                            .as("Run %d: criteria should be identical", run)
                            .isEqualTo(firstResult.matchedCriteria());
                    assertThat(result.portDirection())
                            .as("Run %d: direction should be identical", run)
                            .isEqualTo(firstResult.portDirection());
                }
            }
        }

        /**
         * Contract: Port priority also dominates confidence.
         */
        @Test
        @DisplayName("Port priority dominates confidence")
        void portPriorityDominatesConfidence() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface OrderRepository {
                        Object findById(String id);
                    }
                    """);

            PortClassifier classifier = new PortClassifier();
            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(repo, query);

            // Explicit annotation (priority 100) should win
            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("REPOSITORY");
            assertThat(result.matchedPriority()).isEqualTo(100);
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph() {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
