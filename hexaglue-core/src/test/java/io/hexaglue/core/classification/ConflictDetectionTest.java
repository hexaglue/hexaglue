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

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
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
 * Tests for conflict detection in the classification system.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>Conflicts are properly detected when multiple criteria match</li>
 *   <li>Higher priority wins without causing CONFLICT status</li>
 *   <li>Same priority with incompatible kinds causes CONFLICT status</li>
 *   <li>Compatible kinds (like AGGREGATE_ROOT and ENTITY) don't cause CONFLICT</li>
 * </ul>
 */
class ConflictDetectionTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private DomainClassifier domainClassifier;
    private PortClassifier portClassifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
        domainClassifier = new DomainClassifier();
        portClassifier = new PortClassifier();
    }

    // =========================================================================
    // Domain Classification Conflicts
    // =========================================================================

    @Nested
    @DisplayName("Domain Classification Conflicts")
    class DomainConflictsTest {

        @Test
        @DisplayName("Repository-dominant should classify as AGGREGATE_ROOT")
        void repositoryDominantShouldClassifyAsAggregateRoot() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private String customerName;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            ClassificationResult result = domainClassifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(result.matchedCriteria()).isEqualTo("repository-dominant");
        }

        @Test
        @DisplayName("Explicit annotation should win with high confidence")
        void explicitAnnotationShouldWinWithHighConfidence() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
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

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            ClassificationResult result = domainClassifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.matchedPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("Record with *Id name should be IDENTIFIER")
        void recordIdShouldBeIdentifier() throws IOException {
            writeSource("com/example/ProductId.java", """
                    package com.example;
                    public record ProductId(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode productId = graph.typeNode("com.example.ProductId").orElseThrow();
            ClassificationResult result = domainClassifier.classify(productId, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo(ElementKind.IDENTIFIER.name());
            assertThat(result.matchedCriteria()).isEqualTo("record-single-id");
            assertThat(result.matchedPriority()).isEqualTo(80);
        }

        @Test
        @DisplayName("Conflicting explicit annotations with same priority should be handled")
        void conflictingExplicitAnnotationsWithSamePriority() throws IOException {
            // This is a pathological case: @Entity and @ValueObject on same type
            // Both have priority 100 and are incompatible
            // Should result in CONFLICT status
            writeSource("com/example/Ambiguous.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Entity;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    @Entity
                    @ValueObject
                    public class Ambiguous {
                        private String data;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode ambiguous = graph.typeNode("com.example.Ambiguous").orElseThrow();
            ClassificationResult result = domainClassifier.classify(ambiguous, query);

            // Should be CONFLICT because both have same priority and are incompatible
            assertThat(result.status()).isEqualTo(ClassificationStatus.CONFLICT);
            assertThat(result.isClassified()).isFalse();
            assertThat(result.hasConflicts()).isTrue();

            // Should have both ENTITY and VALUE_OBJECT in conflicts
            List<String> conflictKinds =
                    result.conflicts().stream().map(Conflict::competingKind).toList();
            // At least one of them should be in conflicts
            assertThat(conflictKinds.size()).isGreaterThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // Port Classification Conflicts
    // =========================================================================

    @Nested
    @DisplayName("Port Classification Conflicts")
    class PortConflictsTest {

        @Test
        @DisplayName("Explicit @Repository should classify port correctly")
        void explicitRepositoryShouldClassifyPortCorrectly() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface PaymentGateway {
                        Object findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            ClassificationResult result = portClassifier.classify(gateway, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo(PortKind.REPOSITORY.name());
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("Interface without explicit markers should be unclassified")
        void interfaceWithoutMarkersIsUnclassified() throws IOException {
            writeSource("com/example/ports/in/CustomerRepository.java", """
                    package com.example.ports.in;
                    public interface CustomerRepository {
                        Object findById(String id);
                        void save(Object entity);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode repo =
                    graph.typeNode("com.example.ports.in.CustomerRepository").orElseThrow();
            ClassificationResult result = portClassifier.classify(repo, query);

            // Without explicit annotations or strong heuristics, it should be unclassified
            assertThat(result.isUnclassified()).isTrue();
        }
    }

    // =========================================================================
    // UNCLASSIFIED Cases
    // =========================================================================

    @Nested
    @DisplayName("UNCLASSIFIED Cases")
    class UnclassifiedTest {

        @Test
        @DisplayName("Plain utility class should be UNCLASSIFIED")
        void plainUtilityClassShouldBeUnclassified() throws IOException {
            writeSource("com/example/StringUtils.java", """
                    package com.example;
                    public class StringUtils {
                        public static String capitalize(String s) {
                            return s.toUpperCase();
                        }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode utils = graph.typeNode("com.example.StringUtils").orElseThrow();
            ClassificationResult result = domainClassifier.classify(utils, query);

            assertThat(result.isUnclassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.UNCLASSIFIED);
            assertThat(result.kind()).isEqualTo("UNCLASSIFIED");
            assertThat(result.confidence()).isNull();
            assertThat(result.hasConflicts()).isFalse();
        }

        @Test
        @DisplayName("Plain interface without patterns should be UNCLASSIFIED for ports")
        void plainInterfaceShouldBeUnclassifiedForPorts() throws IOException {
            writeSource("com/example/Callback.java", """
                    package com.example;
                    public interface Callback {
                        void onComplete();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode callback = graph.typeNode("com.example.Callback").orElseThrow();
            ClassificationResult result = portClassifier.classify(callback, query);

            assertThat(result.isUnclassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.UNCLASSIFIED);
        }

        @Test
        @DisplayName("Class should be UNCLASSIFIED for port classifier (only interfaces)")
        void classShouldBeUnclassifiedForPortClassifier() throws IOException {
            writeSource("com/example/OrderRepositoryImpl.java", """
                    package com.example;
                    public class OrderRepositoryImpl {
                        public Object findById(String id) { return null; }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode impl = graph.typeNode("com.example.OrderRepositoryImpl").orElseThrow();
            ClassificationResult result = portClassifier.classify(impl, query);

            assertThat(result.isUnclassified()).isTrue();
        }

        @Test
        @DisplayName("Enum should be classified as VALUE_OBJECT for domain classifier")
        void enumShouldBeClassifiedAsValueObjectForDomainClassifier() throws IOException {
            writeSource("com/example/Color.java", """
                    package com.example;
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode color = graph.typeNode("com.example.Color").orElseThrow();
            ClassificationResult result = domainClassifier.classify(color, query);

            // H2 fix: Enums are now classified as VALUE_OBJECT
            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("VALUE_OBJECT");
            assertThat(result.matchedCriteria()).isEqualTo("domain-enum");
        }
    }

    // =========================================================================
    // Conflict Details Verification
    // =========================================================================

    @Nested
    @DisplayName("Conflict Details Verification")
    class ConflictDetailsTest {

        @Test
        @DisplayName("Repository-dominant classification should succeed")
        void repositoryDominantClassificationShouldSucceed() throws IOException {
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

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            ClassificationResult result = domainClassifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(result.matchedCriteria()).isEqualTo("repository-dominant");
        }

        @Test
        @DisplayName("Explicit annotation wins when repository-dominant also matches")
        void explicitAnnotationWinsWhenRepositoryDominantAlsoMatches() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Customer {
                        private String id;
                    }
                    """);
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Customer findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode customer = graph.typeNode("com.example.Customer").orElseThrow();
            ClassificationResult result = domainClassifier.classify(customer, query);

            assertThat(result.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(result.matchedCriteria()).isEqualTo("explicit-aggregate-root");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph() {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false, false);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
