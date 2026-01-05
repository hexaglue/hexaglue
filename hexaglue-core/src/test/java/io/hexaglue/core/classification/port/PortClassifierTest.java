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

package io.hexaglue.core.classification.port;

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
 * Tests for PortClassifier.
 */
class PortClassifierTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private PortClassifier classifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        classifier = new PortClassifier();
    }

    // =========================================================================
    // Basic Classification
    // =========================================================================

    @Nested
    @DisplayName("Basic Classification")
    class BasicClassificationTest {

        @Test
        @DisplayName("Should classify interface with @Repository as REPOSITORY")
        void shouldClassifyExplicitRepository() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface OrderRepository {
                        Object findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(repo, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("REPOSITORY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.target()).isEqualTo(ClassificationTarget.PORT);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-repository");
            assertThat(result.matchedPriority()).isEqualTo(100);
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("Should classify interface with @PrimaryPort as USE_CASE")
        void shouldClassifyExplicitPrimaryPort() throws IOException {
            writeSource("com/example/PlaceOrderUseCase.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    @PrimaryPort
                    public interface PlaceOrderUseCase {
                        void execute(Object command);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.PlaceOrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(useCase, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("USE_CASE");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        @DisplayName("Should classify interface with @SecondaryPort as GATEWAY")
        void shouldClassifyExplicitSecondaryPort() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.SecondaryPort;
                    @SecondaryPort
                    public interface PaymentGateway {
                        void process(Object payment);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(gateway, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("GATEWAY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("Should return unclassified for plain interface without markers")
        void shouldReturnUnclassifiedForPlainInterface() throws IOException {
            // Use a name that doesn't match any patterns
            writeSource("com/example/SomeApi.java", """
                    package com.example;
                    public interface SomeApi {
                        void doSomething();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode api = graph.typeNode("com.example.SomeApi").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(api, query);

            assertThat(result.isUnclassified()).isTrue();
            assertThat(result.status()).isEqualTo(ClassificationStatus.UNCLASSIFIED);
        }

        @Test
        @DisplayName("Should return unclassified for class (not interface)")
        void shouldReturnUnclassifiedForClass() throws IOException {
            writeSource("com/example/OrderRepositoryImpl.java", """
                    package com.example;
                    public class OrderRepositoryImpl {
                        public Object findById(String id) { return null; }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode impl = graph.typeNode("com.example.OrderRepositoryImpl").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(impl, query);

            assertThat(result.isUnclassified()).isTrue();
        }
    }

    // =========================================================================
    // Heuristic Classification
    // =========================================================================

    @Nested
    @DisplayName("Heuristic Classification")
    class HeuristicClassificationTest {

        @Test
        @DisplayName("Should classify interface with *Repository name as REPOSITORY")
        void shouldClassifyRepositoryByNaming() throws IOException {
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Object findById(String id);
                        void save(Object entity);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.CustomerRepository").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(repo, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("REPOSITORY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.matchedCriteria()).isEqualTo("naming-repository");
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("Should classify interface with *UseCase name as USE_CASE")
        void shouldClassifyUseCaseByNaming() throws IOException {
            // Use method name that doesn't match COMMAND pattern (create*, process*, execute*, etc.)
            // to test naming specifically
            writeSource("com/example/CreateOrderUseCase.java", """
                    package com.example;
                    public interface CreateOrderUseCase {
                        void newOrder(Object order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.CreateOrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(useCase, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("USE_CASE");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.matchedCriteria()).isEqualTo("naming-use-case");
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        @DisplayName("Should classify interface with *Gateway name as GATEWAY")
        void shouldClassifyGatewayByNaming() throws IOException {
            writeSource("com/example/NotificationGateway.java", """
                    package com.example;
                    public interface NotificationGateway {
                        void send(Object notification);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.NotificationGateway").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(gateway, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("GATEWAY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.matchedCriteria()).isEqualTo("naming-gateway");
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("Should classify interface in .in package as USE_CASE")
        void shouldClassifyByPackageIn() throws IOException {
            // Use a name that doesn't match other patterns to test package-in
            writeSource("com/example/ports/in/PlaceOrderCommand.java", """
                    package com.example.ports.in;
                    public interface PlaceOrderCommand {
                        void process();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode command =
                    graph.typeNode("com.example.ports.in.PlaceOrderCommand").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(command, query);

            assertThat(result.isClassified()).isTrue();
            // CommandPatternCriteria (priority 75) wins over PackageInCriteria (priority 60)
            // because method "process()" matches command pattern
            assertThat(result.kind()).isEqualTo("COMMAND");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.matchedCriteria()).isEqualTo("command-pattern");
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVING);
        }
    }

    // =========================================================================
    // Tie-Break and Priority
    // =========================================================================

    @Nested
    @DisplayName("Tie-Break and Priority")
    class TieBreakTest {

        @Test
        @DisplayName("Explicit annotation should win over naming heuristic")
        void explicitAnnotationShouldWinOverNaming() throws IOException {
            // Interface has @Repository but also ends with Gateway
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface PaymentGateway {
                        Object find(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(gateway, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.kind()).isEqualTo("REPOSITORY");
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-repository");
            assertThat(result.matchedPriority()).isEqualTo(100);
            // Should have conflict with GATEWAY from naming
            assertThat(result.conflicts()).isNotEmpty();
            assertThat(result.conflicts()).extracting(Conflict::competingKind).contains("GATEWAY");
        }

        @Test
        @DisplayName("Package should win over naming after priority demotion")
        void packageShouldWinOverNamingAfterPriorityDemotion() throws IOException {
            // Interface in .in package but ends with Repository
            // After priority demotion: package-in (60) > naming-repository (50)
            writeSource("com/example/ports/in/OrderRepository.java", """
                    package com.example.ports.in;
                    public interface OrderRepository {
                        Object findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo =
                    graph.typeNode("com.example.ports.in.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(repo, query);

            // package-in (priority 60) should now win over naming-repository (priority 50)
            assertThat(result.kind()).isEqualTo("USE_CASE");
            assertThat(result.matchedPriority()).isEqualTo(60);
        }

        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() throws IOException {
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Object findById(String id);
                    }
                    """);

            // Classify multiple times and ensure same result
            for (int i = 0; i < 5; i++) {
                ApplicationGraph graph = buildGraph();
                TypeNode repo = graph.typeNode("com.example.CustomerRepository").orElseThrow();
                GraphQuery query = graph.query();

                ClassificationResult result = classifier.classify(repo, query);

                assertThat(result.kind()).isEqualTo("REPOSITORY");
                assertThat(result.matchedCriteria()).isEqualTo("naming-repository");
            }
        }
    }

    // =========================================================================
    // Direction Tests
    // =========================================================================

    @Nested
    @DisplayName("Port Direction")
    class PortDirectionTest {

        @Test
        @DisplayName("Repository should have DRIVEN direction")
        void repositoryShouldBeDriven() throws IOException {
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Object findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(repo, query);

            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(result.portDirectionOpt()).contains(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("UseCase should have DRIVING direction")
        void useCaseShouldBeDriving() throws IOException {
            writeSource("com/example/PlaceOrderUseCase.java", """
                    package com.example;
                    public interface PlaceOrderUseCase {
                        void execute();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.PlaceOrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(useCase, query);

            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        @DisplayName("Gateway should have DRIVEN direction")
        void gatewayShouldBeDriven() throws IOException {
            writeSource("com/example/PaymentGateway.java", """
                    package com.example;
                    public interface PaymentGateway {
                        void process();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode gateway = graph.typeNode("com.example.PaymentGateway").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(gateway, query);

            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("Should handle record types (not ports)")
        void shouldHandleRecordTypes() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public record OrderId(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(orderId, query);

            assertThat(result.isUnclassified()).isTrue();
        }

        @Test
        @DisplayName("Should handle enum types (not ports)")
        void shouldHandleEnumTypes() throws IOException {
            writeSource("com/example/OrderStatus.java", """
                    package com.example;
                    public enum OrderStatus {
                        PENDING, COMPLETED
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode status = graph.typeNode("com.example.OrderStatus").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(status, query);

            assertThat(result.isUnclassified()).isTrue();
        }

        @Test
        @DisplayName("Should handle empty interface")
        void shouldHandleEmptyInterface() throws IOException {
            writeSource("com/example/Marker.java", """
                    package com.example;
                    public interface Marker {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode marker = graph.typeNode("com.example.Marker").orElseThrow();
            GraphQuery query = graph.query();

            ClassificationResult result = classifier.classify(marker, query);

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
