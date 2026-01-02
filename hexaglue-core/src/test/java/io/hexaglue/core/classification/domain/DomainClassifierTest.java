package io.hexaglue.core.classification.domain;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationStatus;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Conflict;
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
 * Tests for DomainClassifier.
 */
class DomainClassifierTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private DomainClassifier classifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        classifier = new DomainClassifier();
    }

    // =========================================================================
    // Basic Classification
    // =========================================================================

    @Nested
    @DisplayName("Basic Classification")
    class BasicClassificationTest {

        @Test
        @DisplayName("Should classify type with @AggregateRoot as AGGREGATE_ROOT")
        void shouldClassifyExplicitAggregateRoot() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Order {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.target()).isEqualTo(ClassificationTarget.DOMAIN);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-aggregate-root");
            assertThat(result.matchedPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should classify type with @Entity as ENTITY")
        void shouldClassifyExplicitEntity() throws IOException {
            writeSource(
                    "com/example/LineItem.java",
                    """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Entity;
                    @Entity
                    public class LineItem {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode lineItem = graph.typeNode("com.example.LineItem").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(lineItem, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("ENTITY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
        }

        @Test
        @DisplayName("Should classify type with @ValueObject as VALUE_OBJECT")
        void shouldClassifyExplicitValueObject() throws IOException {
            writeSource(
                    "com/example/Money.java",
                    """
                    package com.example;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    @ValueObject
                    public record Money(int amount, String currency) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode money = graph.typeNode("com.example.Money").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(money, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("VALUE_OBJECT");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
        }

        @Test
        @DisplayName("Should return unclassified for plain class without markers")
        void shouldReturnUnclassifiedForPlainClass() throws IOException {
            writeSource(
                    "com/example/Utils.java",
                    """
                    package com.example;
                    public class Utils {
                        public static void doSomething() {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode utils = graph.typeNode("com.example.Utils").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(utils, query);

            assertThat(result.isUnclassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.UNCLASSIFIED);
        }
    }

    // =========================================================================
    // Heuristic Classification
    // =========================================================================

    @Nested
    @DisplayName("Heuristic Classification")
    class HeuristicClassificationTest {

        @Test
        @DisplayName("Should classify type used in Repository as AGGREGATE_ROOT")
        void shouldClassifyRepositoryDominantAsAggregateRoot() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                        void save(Order order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.matchedCriteria()).isEqualTo("repository-dominant");
        }

        @Test
        @DisplayName("Should classify class with id field as ENTITY")
        void shouldClassifyClassWithIdAsEntity() throws IOException {
            writeSource(
                    "com/example/Customer.java",
                    """
                    package com.example;
                    public class Customer {
                        private String id;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode customer = graph.typeNode("com.example.Customer").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(customer, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("ENTITY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.matchedCriteria()).isEqualTo("has-identity");
        }

        @Test
        @DisplayName("Should classify record with *Id name as IDENTIFIER")
        void shouldClassifyRecordIdAsIdentifier() throws IOException {
            writeSource(
                    "com/example/OrderId.java",
                    """
                    package com.example;
                    public record OrderId(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(orderId, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("IDENTIFIER");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.matchedCriteria()).isEqualTo("record-single-id");
        }

        @Test
        @DisplayName("Should classify immutable record without id as VALUE_OBJECT")
        void shouldClassifyImmutableRecordAsValueObject() throws IOException {
            writeSource(
                    "com/example/Address.java",
                    """
                    package com.example;
                    public record Address(String street, String city, String zip) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode address = graph.typeNode("com.example.Address").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(address, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("VALUE_OBJECT");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.matchedCriteria()).isEqualTo("immutable-no-id");
        }
    }

    // =========================================================================
    // Tie-Break and Priority
    // =========================================================================

    @Nested
    @DisplayName("Tie-Break and Priority")
    class TieBreakTest {

        @Test
        @DisplayName("Explicit annotation should win over heuristic")
        void explicitAnnotationShouldWinOverHeuristic() throws IOException {
            // Type has @Entity but also has id field (which would match has-identity)
            writeSource(
                    "com/example/Customer.java",
                    """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Entity;
                    @Entity
                    public class Customer {
                        private String id;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode customer = graph.typeNode("com.example.Customer").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(customer, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("ENTITY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-entity");
            assertThat(result.matchedPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("Higher priority should win when both are heuristics")
        void higherPriorityShouldWin() throws IOException {
            // OrderId record: matches both record-single-id (80) and immutable-no-id (60)
            // But record-single-id targets IDENTIFIER while immutable-no-id targets VALUE_OBJECT
            // record-single-id should win due to higher priority
            writeSource(
                    "com/example/OrderId.java",
                    """
                    package com.example;
                    public record OrderId(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(orderId, query);

            assertThat(result.kind()).isEqualTo("IDENTIFIER");
            assertThat(result.matchedPriority()).isEqualTo(80);
        }

        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            // Classify multiple times and ensure same result
            for (int i = 0; i < 5; i++) {
                ApplicationGraph graph = buildGraph();
                TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
                GraphQuery query = graph.query();

                ClassificationResult result = classifier.classify(order, query);

                assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
                assertThat(result.matchedCriteria()).isEqualTo("repository-dominant");
            }
        }
    }

    // =========================================================================
    // Conflict Detection
    // =========================================================================

    @Nested
    @DisplayName("Conflict Detection")
    class ConflictDetectionTest {

        @Test
        @DisplayName("Should detect conflicts but classify with winner")
        void shouldDetectConflictsButClassifyWithWinner() throws IOException {
            // Order used in repository (AGGREGATE_ROOT) but also has id field (ENTITY)
            // AGGREGATE_ROOT and ENTITY are compatible, so should classify
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
            // Should have conflict with ENTITY from has-identity
            assertThat(result.conflicts()).isNotEmpty();
            assertThat(result.conflicts()).extracting(Conflict::competingKind).contains("ENTITY");
        }

        @Test
        @DisplayName("Should report conflicts without failing for compatible kinds")
        void shouldReportConflictsForCompatibleKinds() throws IOException {
            // @AggregateRoot + id field -> AGGREGATE_ROOT wins, but ENTITY is compatible
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Order {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("AGGREGATE_ROOT");
            assertThat(result.status()).isEqualTo(ClassificationStatus.CLASSIFIED);
        }
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("Should handle interface types")
        void shouldHandleInterfaceTypes() throws IOException {
            writeSource(
                    "com/example/OrderService.java",
                    """
                    package com.example;
                    public interface OrderService {
                        void processOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode service = graph.typeNode("com.example.OrderService").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(service, query);

            // Interfaces typically don't match domain criteria
            assertThat(result.isUnclassified()).isTrue();
        }

        @Test
        @DisplayName("Should handle enum types")
        void shouldHandleEnumTypes() throws IOException {
            writeSource(
                    "com/example/OrderStatus.java",
                    """
                    package com.example;
                    public enum OrderStatus {
                        PENDING, CONFIRMED, SHIPPED, DELIVERED
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode status = graph.typeNode("com.example.OrderStatus").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(status, query);

            // Enums don't match typical domain criteria
            assertThat(result.isUnclassified()).isTrue();
        }

        @Test
        @DisplayName("Should handle empty class")
        void shouldHandleEmptyClass() throws IOException {
            writeSource(
                    "com/example/Empty.java",
                    """
                    package com.example;
                    public class Empty {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode empty = graph.typeNode("com.example.Empty").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(empty, query);

            assertThat(result.isUnclassified()).isTrue();
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
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().count());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
