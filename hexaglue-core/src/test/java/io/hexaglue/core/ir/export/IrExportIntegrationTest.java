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

package io.hexaglue.core.ir.export;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.spi.ir.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for the complete IR export pipeline.
 *
 * <p>These tests verify end-to-end scenarios from source code to IrSnapshot,
 * ensuring the entire classification and export pipeline works correctly.
 */
class IrExportIntegrationTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private DomainClassifier domainClassifier;
    private PortClassifier portClassifier;
    private IrExporter exporter;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        domainClassifier = new DomainClassifier();
        portClassifier = new PortClassifier();
        exporter = new IrExporter();
    }

    // =========================================================================
    // CoffeeShop Scenario
    // =========================================================================

    @Nested
    @DisplayName("CoffeeShop Scenario")
    class CoffeeShopScenarioTest {

        @Test
        @DisplayName("should classify and export complete CoffeeShop domain model")
        void exportCoffeeShopDomainModel() throws IOException {
            // Setup: Complete CoffeeShop domain with Order, LineItem, Location
            writeSource("com/example/domain/OrderId.java", """
                    package com.example.domain;
                    public record OrderId(java.util.UUID value) {}
                    """);
            writeSource("com/example/domain/Order.java", """
                    package com.example.domain;
                    import java.util.List;
                    public class Order {
                        private OrderId id;
                        private String customerName;
                        private Location location;
                        private List<LineItem> items;
                        private java.math.BigDecimal total;
                    }
                    """);
            writeSource("com/example/domain/LineItem.java", """
                    package com.example.domain;
                    public class LineItem {
                        private Long id;
                        private String productName;
                        private int quantity;
                        private java.math.BigDecimal price;
                    }
                    """);
            writeSource("com/example/domain/Location.java", """
                    package com.example.domain;
                    public record Location(String city, String street, String zipCode) {}
                    """);
            // Use "Repository" suffix so naming criteria matches before command pattern
            writeSource("com/example/domain/OrderRepository.java", """
                    package com.example.domain;
                    import java.util.List;
                    import java.util.Optional;
                    public interface OrderRepository {
                        Optional<Order> findById(OrderId id);
                        void save(Order order);
                        List<Order> findByCustomer(String customerName);
                        void delete(OrderId id);
                    }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example.domain");

            // Verify domain types
            assertThat(snapshot.domain().types()).hasSize(4);

            // Order should be AGGREGATE_ROOT
            DomainType order = findDomainType(snapshot, "Order");
            assertThat(order.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(order.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(order.hasIdentity()).isTrue();
            assertThat(order.identity().get().fieldName()).isEqualTo("id");
            assertThat(order.identity().get().type().qualifiedName()).isEqualTo("com.example.domain.OrderId");
            assertThat(order.identity().get().unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(order.properties()).hasSize(5);

            // OrderId should be IDENTIFIER
            DomainType orderId = findDomainType(snapshot, "OrderId");
            assertThat(orderId.kind()).isEqualTo(DomainKind.IDENTIFIER);
            assertThat(orderId.construct()).isEqualTo(JavaConstruct.RECORD);

            // LineItem should be ENTITY (has id field)
            DomainType lineItem = findDomainType(snapshot, "LineItem");
            assertThat(lineItem.kind()).isEqualTo(DomainKind.ENTITY);
            assertThat(lineItem.hasIdentity()).isTrue();

            // Location should be VALUE_OBJECT (record without id)
            DomainType location = findDomainType(snapshot, "Location");
            assertThat(location.kind()).isEqualTo(DomainKind.VALUE_OBJECT);
            assertThat(location.construct()).isEqualTo(JavaConstruct.RECORD);
            assertThat(location.hasIdentity()).isFalse();

            // Verify port
            assertThat(snapshot.ports().ports()).hasSize(1);
            Port orderRepo = snapshot.ports().ports().get(0);
            assertThat(orderRepo.simpleName()).isEqualTo("OrderRepository");
            assertThat(orderRepo.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(orderRepo.direction()).isEqualTo(PortDirection.DRIVEN);
            assertThat(orderRepo.methods()).hasSize(4);
            assertThat(orderRepo.managedTypes()).contains("com.example.domain.Order");
        }

        @Test
        @DisplayName("should export CoffeeShop with correct metadata")
        void exportCoffeeShopWithMetadata() throws IOException {
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

            IrSnapshot snapshot = analyzeAndExport("com.example");

            assertThat(snapshot.metadata().basePackage()).isEqualTo("com.example");
            assertThat(snapshot.metadata().typeCount()).isEqualTo(1);
            assertThat(snapshot.metadata().portCount()).isEqualTo(1);
            assertThat(snapshot.metadata().timestamp()).isNotNull();
            assertThat(snapshot.metadata().engineVersion()).isNotBlank();
        }
    }

    // =========================================================================
    // Hexagonal Architecture Scenario
    // =========================================================================

    @Nested
    @DisplayName("Hexagonal Architecture Scenario")
    class HexagonalArchitectureTest {

        @Test
        @DisplayName("should export complete hexagonal architecture with ports in/out")
        void exportHexagonalArchitecture() throws IOException {
            // Domain
            writeSource("com/example/domain/Product.java", """
                    package com.example.domain;
                    public class Product {
                        private String id;
                        private String name;
                        private java.math.BigDecimal price;
                    }
                    """);

            // Driving port (in) - use naming pattern for reliable classification
            // Use method name that doesn't match COMMAND pattern to test USE_CASE naming
            writeSource("com/example/port/in/CreateProductUseCase.java", """
                    package com.example.port.in;
                    import com.example.domain.Product;
                    public interface CreateProductUseCase {
                        Product newProduct(String name, java.math.BigDecimal price);
                    }
                    """);

            // Driven port (out) - repository with naming pattern
            writeSource("com/example/port/out/ProductRepository.java", """
                    package com.example.port.out;
                    import com.example.domain.Product;
                    import java.util.Optional;
                    public interface ProductRepository {
                        void save(Product product);
                        Optional<Product> findById(String id);
                    }
                    """);

            // Driven port - gateway with naming pattern (in infrastructure package to test naming)
            writeSource("com/example/infrastructure/NotificationGateway.java", """
                    package com.example.infrastructure;
                    import com.example.domain.Product;
                    public interface NotificationGateway {
                        void notifyProductCreated(Product product);
                    }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            // Verify domain
            assertThat(snapshot.domain().types()).hasSize(1);
            DomainType product = snapshot.domain().types().get(0);
            assertThat(product.simpleName()).isEqualTo("Product");
            assertThat(product.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);

            // Verify ports
            assertThat(snapshot.ports().ports()).hasSize(3);

            // Driving port (UseCase pattern)
            Port createUseCase = findPort(snapshot, "CreateProductUseCase");
            assertThat(createUseCase.kind()).isEqualTo(PortKind.USE_CASE);
            assertThat(createUseCase.direction()).isEqualTo(PortDirection.DRIVING);

            // Driven ports (Repository and Gateway patterns)
            Port repository = findPort(snapshot, "ProductRepository");
            assertThat(repository.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(repository.direction()).isEqualTo(PortDirection.DRIVEN);

            Port gateway = findPort(snapshot, "NotificationGateway");
            assertThat(gateway.kind()).isEqualTo(PortKind.GATEWAY);
            assertThat(gateway.direction()).isEqualTo(PortDirection.DRIVEN);
        }

        @Test
        @DisplayName("should correctly identify port directions from package and naming")
        void identifyPortDirections() throws IOException {
            writeSource("com/example/domain/Entity.java", """
                    package com.example.domain;
                    public class Entity { private String id; }
                    """);

            // Package .port.in with UseCase naming = DRIVING
            // Use method name that doesn't match COMMAND pattern
            writeSource("com/example/port/in/SomeUseCase.java", """
                    package com.example.port.in;
                    public interface SomeUseCase {
                        void forEntity(String entityId);
                    }
                    """);

            // Repository pattern = DRIVEN
            writeSource("com/example/port/out/EntityRepository.java", """
                    package com.example.port.out;
                    import com.example.domain.Entity;
                    public interface EntityRepository {
                        Entity findById(String id);
                    }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            // UseCase in .port.in should be DRIVING
            Port useCase = findPort(snapshot, "SomeUseCase");
            assertThat(useCase.kind()).isEqualTo(PortKind.USE_CASE);
            assertThat(useCase.direction()).isEqualTo(PortDirection.DRIVING);

            // Repository should be DRIVEN
            Port repository = findPort(snapshot, "EntityRepository");
            assertThat(repository.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(repository.direction()).isEqualTo(PortDirection.DRIVEN);
        }
    }

    // =========================================================================
    // Determinism Tests
    // =========================================================================

    @Nested
    @DisplayName("Export Determinism")
    class DeterminismTest {

        @Test
        @DisplayName("should produce identical snapshots for same input")
        void identicalSnapshotsForSameInput() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String id;
                        private String email;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Customer findById(String id);
                    }
                    """);

            // Export twice
            IrSnapshot snapshot1 = analyzeAndExport("com.example");
            IrSnapshot snapshot2 = analyzeAndExport("com.example");

            // Domain types should be in same order
            assertThat(snapshot1.domain().types())
                    .extracting(DomainType::qualifiedName)
                    .containsExactlyElementsOf(snapshot2.domain().types().stream()
                            .map(DomainType::qualifiedName)
                            .toList());

            // Ports should be in same order
            assertThat(snapshot1.ports().ports())
                    .extracting(Port::qualifiedName)
                    .containsExactlyElementsOf(snapshot2.ports().ports().stream()
                            .map(Port::qualifiedName)
                            .toList());

            // Counts should match
            assertThat(snapshot1.metadata().typeCount())
                    .isEqualTo(snapshot2.metadata().typeCount());
            assertThat(snapshot1.metadata().portCount())
                    .isEqualTo(snapshot2.metadata().portCount());
        }

        @Test
        @DisplayName("should sort domain types alphabetically by qualified name")
        void sortDomainTypesAlphabetically() throws IOException {
            // Create types in non-alphabetical order
            writeSource("com/example/Zebra.java", """
                    package com.example;
                    public class Zebra { private String id; }
                    """);
            writeSource("com/example/Apple.java", """
                    package com.example;
                    public class Apple { private String id; }
                    """);
            writeSource("com/example/Mango.java", """
                    package com.example;
                    public class Mango { private String id; }
                    """);
            writeSource("com/example/ZebraRepository.java", """
                    package com.example;
                    public interface ZebraRepository { Zebra findById(String id); }
                    """);
            writeSource("com/example/AppleRepository.java", """
                    package com.example;
                    public interface AppleRepository { Apple findById(String id); }
                    """);
            writeSource("com/example/MangoRepository.java", """
                    package com.example;
                    public interface MangoRepository { Mango findById(String id); }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            // Domain types should be sorted alphabetically
            assertThat(snapshot.domain().types())
                    .extracting(DomainType::qualifiedName)
                    .containsExactly("com.example.Apple", "com.example.Mango", "com.example.Zebra");

            // Ports should also be sorted alphabetically
            assertThat(snapshot.ports().ports())
                    .extracting(Port::qualifiedName)
                    .containsExactly(
                            "com.example.AppleRepository",
                            "com.example.MangoRepository",
                            "com.example.ZebraRepository");
        }
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("should handle types with no classification")
        void handleUnclassifiedTypes() throws IOException {
            // A utility class with no domain characteristics
            writeSource("com/example/Utils.java", """
                    package com.example;
                    public class Utils {
                        public static String format(String s) { return s; }
                    }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            // Utils should not appear in domain types (unclassified)
            assertThat(snapshot.domain().types()).isEmpty();
            assertThat(snapshot.ports().ports()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty project")
        void handleEmptyProject() throws IOException {
            // Create temp directory but no source files
            // (tempDir already exists)

            ApplicationGraph graph = buildGraph("com.example");
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.domain().types()).isEmpty();
            assertThat(snapshot.ports().ports()).isEmpty();
            assertThat(snapshot.metadata().typeCount()).isEqualTo(0);
            assertThat(snapshot.metadata().portCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should export type with jMolecules annotations")
        void exportJMoleculesAnnotatedType() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    @org.jmolecules.ddd.annotation.AggregateRoot
                    public class Customer {
                        private String id;
                        private String name;
                    }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            assertThat(snapshot.domain().types()).hasSize(1);
            DomainType customer = snapshot.domain().types().get(0);
            assertThat(customer.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(customer.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(customer.annotations()).contains("org.jmolecules.ddd.annotation.AggregateRoot");
        }
    }

    // =========================================================================
    // Complex Scenarios
    // =========================================================================

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenariosTest {

        @Test
        @DisplayName("should handle nested collections and optionals")
        void handleNestedCollectionsAndOptionals() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.*;
                    public class Order {
                        private String id;
                        private List<String> tags;
                        private Optional<String> notes;
                        private Set<String> categories;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            DomainType order = snapshot.domain().types().get(0);

            DomainProperty tags = findProperty(order, "tags");
            assertThat(tags.cardinality()).isEqualTo(Cardinality.COLLECTION);

            DomainProperty notes = findProperty(order, "notes");
            assertThat(notes.cardinality()).isEqualTo(Cardinality.OPTIONAL);

            DomainProperty categories = findProperty(order, "categories");
            assertThat(categories.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }

        @Test
        @DisplayName("should handle multiple aggregate roots")
        void handleMultipleAggregateRoots() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order { private String id; }
                    """);
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer { private String id; }
                    """);
            writeSource("com/example/Product.java", """
                    package com.example;
                    public class Product { private String id; }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository { Order findById(String id); }
                    """);
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository { Customer findById(String id); }
                    """);
            writeSource("com/example/ProductRepository.java", """
                    package com.example;
                    public interface ProductRepository { Product findById(String id); }
                    """);

            IrSnapshot snapshot = analyzeAndExport("com.example");

            assertThat(snapshot.domain().types()).hasSize(3);
            assertThat(snapshot.domain().types()).allMatch(t -> t.kind() == DomainKind.AGGREGATE_ROOT);

            assertThat(snapshot.ports().ports()).hasSize(3);
            assertThat(snapshot.ports().ports()).allMatch(p -> p.kind() == PortKind.REPOSITORY);
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

    private ApplicationGraph buildGraph(String basePackage) {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, basePackage);

        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of(basePackage, 17, (int) model.types().size());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }

    private List<ClassificationResult> classifyAll(ApplicationGraph graph) {
        GraphQuery query = graph.query();
        List<ClassificationResult> results = new ArrayList<>();

        for (TypeNode type : graph.typeNodes()) {
            ClassificationResult domainResult = domainClassifier.classify(type, query);
            if (domainResult.isClassified()) {
                results.add(domainResult);
                continue;
            }

            ClassificationResult portResult = portClassifier.classify(type, query);
            if (portResult.isClassified()) {
                results.add(portResult);
            }
        }

        return results;
    }

    private IrSnapshot analyzeAndExport(String basePackage) {
        ApplicationGraph graph = buildGraph(basePackage);
        List<ClassificationResult> classifications = classifyAll(graph);
        return exporter.export(graph, classifications);
    }

    private DomainType findDomainType(IrSnapshot snapshot, String simpleName) {
        return snapshot.domain().types().stream()
                .filter(t -> t.simpleName().equals(simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DomainType not found: " + simpleName));
    }

    private Port findPort(IrSnapshot snapshot, String simpleName) {
        return snapshot.ports().ports().stream()
                .filter(p -> p.simpleName().equals(simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Port not found: " + simpleName));
    }

    private DomainProperty findProperty(DomainType type, String name) {
        return type.properties().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Property not found: " + name));
    }
}
