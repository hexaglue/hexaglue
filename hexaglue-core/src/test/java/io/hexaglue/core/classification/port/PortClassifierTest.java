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
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
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
    // Tie-Break and Priority
    // =========================================================================

    @Nested
    @DisplayName("Tie-Break and Priority")
    class TieBreakTest {

        @Test
        @DisplayName("Explicit annotation takes priority")
        void explicitAnnotationTakesPriority() throws IOException {
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
        }

        @Test
        @DisplayName("Classification should be deterministic")
        void classificationShouldBeDeterministic() throws IOException {
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
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
                assertThat(result.matchedCriteria()).isEqualTo("explicit-repository");
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

            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(result.portDirectionOpt()).contains(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("UseCase should have DRIVING direction")
        void useCaseShouldBeDriving() throws IOException {
            writeSource("com/example/PlaceOrderUseCase.java", """
                    package com.example;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    @PrimaryPort
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
                    import org.jmolecules.architecture.hexagonal.SecondaryPort;
                    @SecondaryPort
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
    // H1 Regression Tests - UseCases interfaces should be DRIVING, not DRIVEN
    // =========================================================================

    @Nested
    @DisplayName("H1 - UseCases Interfaces Classification")
    class UseCasesInterfacesTest {

        @Test
        @DisplayName("Interface with UseCases suffix in ports.in package should not be classified as DRIVEN")
        void interfaceWithUseCasesSuffixInPortsInPackageShouldNotBeDriven() throws IOException {
            // Given: An interface named TaskUseCases in ports.in package that uses aggregate-like types
            writeSource("com/example/domain/Task.java", """
                    package com.example.domain;
                    public class Task {
                        private String id;
                        private String name;
                        public String getId() { return id; }
                    }
                    """);

            writeSource("com/example/ports/in/TaskUseCases.java", """
                    package com.example.ports.in;
                    import com.example.domain.Task;
                    public interface TaskUseCases {
                        void createTask(String name);
                        Task getTask(String id);
                        void completeTask(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode taskUseCases =
                    graph.typeNode("com.example.ports.in.TaskUseCases").orElseThrow();
            GraphQuery query = graph.query();

            // When
            ClassificationResult result = classifier.classify(taskUseCases, query);

            // Then: Should NOT be classified as a DRIVEN port
            // The interface may be DRIVING (via command-pattern) or UNCLASSIFIED, but NOT DRIVEN
            if (result.isClassified()) {
                assertThat(result.portDirection())
                        .as("UseCases interface should be DRIVING, not DRIVEN")
                        .isNotEqualTo(PortDirection.DRIVEN);
            }
        }

        @Test
        @DisplayName("Interface with singular UseCase suffix should not be classified as DRIVEN")
        void interfaceWithUseCaseSuffixShouldNotBeDriven() throws IOException {
            // Given: An interface named PlaceOrderUseCase that uses aggregate-like types
            writeSource("com/example/domain/Order.java", """
                    package com.example.domain;
                    public class Order {
                        private String id;
                        public String getId() { return id; }
                    }
                    """);

            writeSource("com/example/PlaceOrderUseCase.java", """
                    package com.example;
                    import com.example.domain.Order;
                    public interface PlaceOrderUseCase {
                        Order execute(String customerId);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode useCase = graph.typeNode("com.example.PlaceOrderUseCase").orElseThrow();
            GraphQuery query = graph.query();

            // When
            ClassificationResult result = classifier.classify(useCase, query);

            // Then: Should NOT be classified as a DRIVEN port
            if (result.isClassified()) {
                assertThat(result.portDirection())
                        .as("UseCase interface should be DRIVING, not DRIVEN")
                        .isNotEqualTo(PortDirection.DRIVEN);
            }
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
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example", false, false);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
