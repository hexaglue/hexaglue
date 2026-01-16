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
 * Integration tests for the classification system.
 *
 * <p>These tests verify that the domain and port classifiers work together
 * correctly on realistic scenarios like Order/OrderRepository.
 */
class ClassificationIntegrationTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private DomainClassifier domainClassifier;
    private PortClassifier portClassifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        domainClassifier = new DomainClassifier();
        portClassifier = new PortClassifier();
    }

    // =========================================================================
    // Order/OrderRepository Scenario
    // =========================================================================

    @Nested
    @DisplayName("Order/OrderRepository Scenario")
    class OrderRepositoryScenarioTest {

        @Test
        @DisplayName("Order with repository should be classified as AGGREGATE_ROOT")
        void orderWithRepositoryShouldBeAggregateRoot() throws IOException {
            // Setup: Order class with id field + OrderRepository interface
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private String customerName;
                        private double total;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                        void save(Order order);
                        java.util.List<Order> findAll();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            // Classify Order
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            ClassificationResult orderResult = domainClassifier.classify(order, query);

            assertThat(orderResult.isClassified()).isTrue();
            assertThat(orderResult.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(orderResult.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(orderResult.matchedCriteria()).isEqualTo("repository-dominant");
            assertThat(orderResult.target()).isEqualTo(ClassificationTarget.DOMAIN);

            // Verify evidence mentions the repository
            assertThat(orderResult.evidence()).isNotEmpty();
            assertThat(orderResult.justification()).containsIgnoringCase("repository");
        }

        @Test
        @DisplayName("OrderRepository should be classified as REPOSITORY port")
        void orderRepositoryShouldBeRepositoryPort() throws IOException {
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
                        void save(Order order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            // Classify OrderRepository
            TypeNode repo = graph.typeNode("com.example.OrderRepository").orElseThrow();
            ClassificationResult repoResult = portClassifier.classify(repo, query);

            assertThat(repoResult.isClassified()).isTrue();
            assertThat(repoResult.kind()).isEqualTo(PortKind.REPOSITORY.name());
            assertThat(repoResult.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            // signature-based-driven (priority 70) wins over naming-repository (priority 50)
            assertThat(repoResult.matchedCriteria()).isEqualTo("signature-based-driven");
            assertThat(repoResult.portDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(repoResult.target()).isEqualTo(ClassificationTarget.PORT);
        }

        @Test
        @DisplayName("Order with repository should be AGGREGATE_ROOT without conflicts")
        void orderShouldBeAggregateRootWithoutConflicts() throws IOException {
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

            // AGGREGATE_ROOT via repository-dominant - no conflicts expected
            // (has-identity criteria was removed as weak heuristic)
            assertThat(result.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(result.hasConflicts()).isFalse();
            assertThat(result.matchedCriteria()).isEqualTo("repository-dominant");
        }
    }

    // =========================================================================
    // CoffeeShop Scenario
    // =========================================================================

    @Nested
    @DisplayName("CoffeeShop Scenario")
    class CoffeeShopScenarioTest {

        @Test
        @DisplayName("Complete coffee shop domain should be classified correctly")
        void coffeeShopDomainClassification() throws IOException {
            // Order aggregate root - explicit annotation
            writeSource("com/coffeeshop/order/Order.java", """
                    package com.coffeeshop.order;
                    import java.util.List;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Order {
                        private OrderId id;
                        private List<LineItem> items;
                        private Location location;
                    }
                    """);

            // OrderId - should be IDENTIFIER (record-single-id criteria retained)
            writeSource("com/coffeeshop/order/OrderId.java", """
                    package com.coffeeshop.order;
                    public record OrderId(String value) {}
                    """);

            // LineItem - explicit @Entity annotation (collection-element-entity removed)
            writeSource("com/coffeeshop/order/LineItem.java", """
                    package com.coffeeshop.order;
                    import org.jmolecules.ddd.annotation.Entity;
                    @Entity
                    public class LineItem {
                        private String id;
                        private String productName;
                        private int quantity;
                    }
                    """);

            // Location - explicit @ValueObject annotation (embedded-value-object removed)
            writeSource("com/coffeeshop/order/Location.java", """
                    package com.coffeeshop.order;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    @ValueObject
                    public record Location(String store, String table) {}
                    """);

            // Orders repository - explicit @Repository annotation
            writeSource("com/coffeeshop/order/OrderRepository.java", """
                    package com.coffeeshop.order;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface OrderRepository {
                        Order findById(OrderId id);
                        void save(Order order);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.coffeeshop");
            GraphQuery query = graph.query();

            // Classify Order - should be AGGREGATE_ROOT (explicit annotation)
            TypeNode order = graph.typeNode("com.coffeeshop.order.Order").orElseThrow();
            ClassificationResult orderResult = domainClassifier.classify(order, query);
            assertThat(orderResult.kind()).isEqualTo(ElementKind.AGGREGATE_ROOT.name());
            assertThat(orderResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(orderResult.matchedCriteria()).isEqualTo("explicit-aggregate-root");

            // Classify OrderId - should be IDENTIFIER (record with *Id name)
            TypeNode orderId = graph.typeNode("com.coffeeshop.order.OrderId").orElseThrow();
            ClassificationResult orderIdResult = domainClassifier.classify(orderId, query);
            assertThat(orderIdResult.kind()).isEqualTo(ElementKind.IDENTIFIER.name());
            assertThat(orderIdResult.matchedCriteria()).isEqualTo("record-single-id");

            // Classify LineItem - should be ENTITY (explicit annotation)
            TypeNode lineItem = graph.typeNode("com.coffeeshop.order.LineItem").orElseThrow();
            ClassificationResult lineItemResult = domainClassifier.classify(lineItem, query);
            assertThat(lineItemResult.kind()).isEqualTo(ElementKind.ENTITY.name());
            assertThat(lineItemResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(lineItemResult.matchedCriteria()).isEqualTo("explicit-entity");

            // Classify Location - should be VALUE_OBJECT (explicit annotation)
            TypeNode location = graph.typeNode("com.coffeeshop.order.Location").orElseThrow();
            ClassificationResult locationResult = domainClassifier.classify(location, query);
            assertThat(locationResult.kind()).isEqualTo(ElementKind.VALUE_OBJECT.name());
            assertThat(locationResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(locationResult.matchedCriteria()).isEqualTo("explicit-value-object");

            // Classify OrderRepository - should be REPOSITORY port (explicit annotation)
            TypeNode orderRepo =
                    graph.typeNode("com.coffeeshop.order.OrderRepository").orElseThrow();
            ClassificationResult orderRepoResult = portClassifier.classify(orderRepo, query);
            assertThat(orderRepoResult.kind()).isEqualTo(PortKind.REPOSITORY.name());
            assertThat(orderRepoResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(orderRepoResult.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }
    }

    // =========================================================================
    // Hexagonal Architecture Scenario
    // =========================================================================

    @Nested
    @DisplayName("Hexagonal Architecture Scenario")
    class HexagonalArchitectureTest {

        @Test
        @DisplayName("Ports in and out should be classified with correct direction")
        void portsInOutClassification() throws IOException {
            // Domain
            writeSource("com/example/domain/Order.java", """
                    package com.example.domain;
                    public class Order {
                        private String id;
                    }
                    """);

            // Driving port (in) - explicit @PrimaryPort annotation (naming criteria removed)
            writeSource("com/example/ports/in/PlaceOrderUseCase.java", """
                    package com.example.ports.in;
                    import org.jmolecules.architecture.hexagonal.PrimaryPort;
                    @PrimaryPort
                    public interface PlaceOrderUseCase {
                        void forOrder(Object order);
                    }
                    """);

            // Driven port (out) - explicit @Repository annotation
            writeSource("com/example/ports/out/OrderRepository.java", """
                    package com.example.ports.out;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface OrderRepository {
                        Object findById(String id);
                        void save(Object order);
                    }
                    """);

            // Gateway - explicit @SecondaryPort annotation (naming criteria removed)
            writeSource("com/example/infrastructure/PaymentGateway.java", """
                    package com.example.infrastructure;
                    import org.jmolecules.architecture.hexagonal.SecondaryPort;
                    @SecondaryPort
                    public interface PaymentGateway {
                        void charge(Object payment);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            // PlaceOrderUseCase - DRIVING (explicit annotation)
            TypeNode useCase =
                    graph.typeNode("com.example.ports.in.PlaceOrderUseCase").orElseThrow();
            ClassificationResult useCaseResult = portClassifier.classify(useCase, query);
            assertThat(useCaseResult.kind()).isEqualTo(PortKind.USE_CASE.name());
            assertThat(useCaseResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(useCaseResult.portDirection()).isEqualTo(PortDirection.DRIVING);

            // OrderRepository - DRIVEN (explicit annotation)
            TypeNode repo =
                    graph.typeNode("com.example.ports.out.OrderRepository").orElseThrow();
            ClassificationResult repoResult = portClassifier.classify(repo, query);
            assertThat(repoResult.kind()).isEqualTo(PortKind.REPOSITORY.name());
            assertThat(repoResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(repoResult.portDirection()).isEqualTo(PortDirection.DRIVEN);

            // PaymentGateway - DRIVEN (explicit annotation)
            TypeNode gateway =
                    graph.typeNode("com.example.infrastructure.PaymentGateway").orElseThrow();
            ClassificationResult gatewayResult = portClassifier.classify(gateway, query);
            assertThat(gatewayResult.kind()).isEqualTo(PortKind.GATEWAY.name());
            assertThat(gatewayResult.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(gatewayResult.portDirection()).isEqualTo(PortDirection.DRIVEN);
        }
    }

    // =========================================================================
    // Explicit Annotations Scenario
    // =========================================================================

    @Nested
    @DisplayName("Explicit Annotations Scenario")
    class ExplicitAnnotationsTest {

        @Test
        @DisplayName("Explicit annotations should win over heuristics")
        void explicitAnnotationsOverrideHeuristics() throws IOException {
            // Entity with @ValueObject annotation - annotation should win
            writeSource("com/example/Money.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    @ValueObject
                    public class Money {
                        private String id;  // Doesn't trigger any criteria (has-identity removed)
                        private int amount;
                        private String currency;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode money = graph.typeNode("com.example.Money").orElseThrow();
            ClassificationResult result = domainClassifier.classify(money, query);

            // @ValueObject (EXPLICIT, priority 100) is the only match
            // has-identity criteria was removed as weak heuristic
            assertThat(result.kind()).isEqualTo(ElementKind.VALUE_OBJECT.name());
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-value-object");

            // No conflicts expected (has-identity removed)
            assertThat(result.hasConflicts()).isFalse();
        }

        @Test
        @DisplayName("@Repository annotation should classify as REPOSITORY port")
        void repositoryAnnotationClassification() throws IOException {
            writeSource("com/example/CustomerStore.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Repository;
                    @Repository
                    public interface CustomerStore {
                        Object find(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode store = graph.typeNode("com.example.CustomerStore").orElseThrow();
            ClassificationResult result = portClassifier.classify(store, query);

            assertThat(result.kind()).isEqualTo(PortKind.REPOSITORY.name());
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.matchedCriteria()).isEqualTo("explicit-repository");
        }
    }

    // =========================================================================
    // Evidence Verification
    // =========================================================================

    @Nested
    @DisplayName("Evidence Verification")
    class EvidenceVerificationTest {

        @Test
        @DisplayName("Repository-dominant should provide evidence with repository reference")
        void repositoryDominantEvidence() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
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

            assertThat(result.evidence()).isNotEmpty();

            // Should have evidence mentioning the repository relationship
            boolean hasRelationshipEvidence =
                    result.evidence().stream().anyMatch(e -> e.type() == EvidenceType.RELATIONSHIP);
            assertThat(hasRelationshipEvidence).isTrue();

            // Note: STRUCTURE evidence was provided by has-identity criteria (now removed)
            // Only RELATIONSHIP evidence expected from repository-dominant
        }

        @Test
        @DisplayName("Naming criteria should provide naming evidence")
        void namingCriteriaEvidence() throws IOException {
            writeSource("com/example/ProductId.java", """
                    package com.example;
                    public record ProductId(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode productId = graph.typeNode("com.example.ProductId").orElseThrow();
            ClassificationResult result = domainClassifier.classify(productId, query);

            assertThat(result.evidence()).isNotEmpty();

            boolean hasNamingEvidence = result.evidence().stream().anyMatch(e -> e.type() == EvidenceType.NAMING);
            assertThat(hasNamingEvidence).isTrue();
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
        return buildGraph("com.example");
    }

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, basePackage);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of(basePackage, 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }
}
