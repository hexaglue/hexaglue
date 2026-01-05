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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for IrExporter.
 */
class IrExporterTest {

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
    // Basic Export Tests
    // =========================================================================

    @Nested
    @DisplayName("Basic Export")
    class BasicExportTest {

        @Test
        @DisplayName("should export empty snapshot for no classifications")
        void exportEmptySnapshot() throws IOException {
            writeSource("com/example/Dummy.java", """
                    package com.example;
                    public class Dummy {}
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = List.of();

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.domain().types()).isEmpty();
            assertThat(snapshot.ports().ports()).isEmpty();
            assertThat(snapshot.metadata()).isNotNull();
            assertThat(snapshot.metadata().typeCount()).isEqualTo(0);
            assertThat(snapshot.metadata().portCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should export aggregate root with identity")
        void exportAggregateRootWithIdentity() throws IOException {
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
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            // Verify domain types
            assertThat(snapshot.domain().types()).hasSize(1);
            DomainType order = snapshot.domain().types().get(0);
            assertThat(order.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(order.simpleName()).isEqualTo("Order");
            assertThat(order.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
            assertThat(order.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(order.construct()).isEqualTo(JavaConstruct.CLASS);

            // Verify identity
            assertThat(order.hasIdentity()).isTrue();
            Identity identity = order.identity().orElseThrow();
            assertThat(identity.fieldName()).isEqualTo("id");
            assertThat(identity.type().qualifiedName()).isEqualTo("java.lang.String");

            // Verify properties
            assertThat(order.properties()).hasSize(3);
            assertThat(order.properties())
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("id", "customerName", "total");

            // Verify metadata
            assertThat(snapshot.metadata().typeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should export repository port with methods")
        void exportRepositoryPort() throws IOException {
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
                        java.util.List<Order> findAll();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            // Verify ports
            assertThat(snapshot.ports().ports()).hasSize(1);
            Port port = snapshot.ports().ports().get(0);
            assertThat(port.qualifiedName()).isEqualTo("com.example.OrderRepository");
            assertThat(port.simpleName()).isEqualTo("OrderRepository");
            assertThat(port.kind()).isEqualTo(PortKind.REPOSITORY);
            assertThat(port.direction()).isEqualTo(PortDirection.DRIVEN);

            // Verify methods
            assertThat(port.methods()).hasSize(3);
            assertThat(port.methods())
                    .extracting(PortMethod::name)
                    .containsExactlyInAnyOrder("findById", "save", "findAll");

            // Verify managed types
            assertThat(port.managedTypes()).contains("com.example.Order");

            // Verify metadata
            assertThat(snapshot.metadata().portCount()).isEqualTo(1);
        }
    }

    // =========================================================================
    // Domain Type Tests
    // =========================================================================

    @Nested
    @DisplayName("Domain Type Export")
    class DomainTypeExportTest {

        @Test
        @DisplayName("should export record as VALUE_OBJECT")
        void exportRecordValueObject() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    public record Money(java.math.BigDecimal amount, String currency) {}
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.domain().types()).hasSize(1);
            DomainType money = snapshot.domain().types().get(0);
            assertThat(money.qualifiedName()).isEqualTo("com.example.Money");
            assertThat(money.kind()).isEqualTo(DomainKind.VALUE_OBJECT);
            assertThat(money.construct()).isEqualTo(JavaConstruct.RECORD);
            assertThat(money.hasIdentity()).isFalse();

            // Verify properties for record components
            assertThat(money.properties()).hasSize(2);
            assertThat(money.properties())
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("amount", "currency");
        }

        @Test
        @DisplayName("should export identifier type with wrapped identity")
        void exportIdentifierWithWrappedIdentity() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public record OrderId(java.util.UUID value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.domain().types()).hasSize(1);
            DomainType orderId = snapshot.domain().types().get(0);
            assertThat(orderId.qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(orderId.kind()).isEqualTo(DomainKind.IDENTIFIER);
            assertThat(orderId.construct()).isEqualTo(JavaConstruct.RECORD);
        }

        @Test
        @DisplayName("should export entity with explicit annotation")
        void exportExplicitEntity() throws IOException {
            writeSource("com/example/LineItem.java", """
                    package com.example;
                    @org.jmolecules.ddd.annotation.Entity
                    public class LineItem {
                        private Long id;
                        private String productName;
                        private int quantity;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.domain().types()).hasSize(1);
            DomainType lineItem = snapshot.domain().types().get(0);
            assertThat(lineItem.kind()).isEqualTo(DomainKind.ENTITY);
            assertThat(lineItem.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);

            // Verify annotations are captured
            assertThat(lineItem.annotations()).contains("org.jmolecules.ddd.annotation.Entity");
        }
    }

    // =========================================================================
    // Property Extraction Tests
    // =========================================================================

    @Nested
    @DisplayName("Property Extraction")
    class PropertyExtractionTest {

        @Test
        @DisplayName("should extract collection property with correct cardinality")
        void extractCollectionProperty() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private java.util.List<String> items;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = snapshot.domain().types().get(0);
            DomainProperty itemsProp = order.properties().stream()
                    .filter(p -> p.name().equals("items"))
                    .findFirst()
                    .orElseThrow();

            assertThat(itemsProp.cardinality()).isEqualTo(Cardinality.COLLECTION);
            assertThat(itemsProp.type().unwrapElement().qualifiedName()).isEqualTo("java.lang.String");
        }

        @Test
        @DisplayName("should extract optional property with correct cardinality")
        void extractOptionalProperty() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String id;
                        private java.util.Optional<String> middleName;
                    }
                    """);
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Customer findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType customer = snapshot.domain().types().get(0);
            DomainProperty middleNameProp = customer.properties().stream()
                    .filter(p -> p.name().equals("middleName"))
                    .findFirst()
                    .orElseThrow();

            assertThat(middleNameProp.cardinality()).isEqualTo(Cardinality.OPTIONAL);
            assertThat(middleNameProp.type().unwrapElement().qualifiedName()).isEqualTo("java.lang.String");
            assertThat(middleNameProp.nullability()).isEqualTo(Nullability.NULLABLE);
        }

        @Test
        @DisplayName("should mark identity property correctly")
        void markIdentityProperty() throws IOException {
            writeSource("com/example/Product.java", """
                    package com.example;
                    public class Product {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/ProductRepository.java", """
                    package com.example;
                    public interface ProductRepository {
                        Product findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType product = snapshot.domain().types().get(0);

            DomainProperty idProp = product.properties().stream()
                    .filter(p -> p.name().equals("id"))
                    .findFirst()
                    .orElseThrow();
            assertThat(idProp.isIdentity()).isTrue();

            DomainProperty nameProp = product.properties().stream()
                    .filter(p -> p.name().equals("name"))
                    .findFirst()
                    .orElseThrow();
            assertThat(nameProp.isIdentity()).isFalse();
        }

        @Test
        @DisplayName("should extract primitive property as NON_NULL")
        void extractPrimitiveNonNull() throws IOException {
            writeSource("com/example/Counter.java", """
                    package com.example;
                    public class Counter {
                        private String id;
                        private int count;
                        private boolean active;
                    }
                    """);
            writeSource("com/example/CounterRepository.java", """
                    package com.example;
                    public interface CounterRepository {
                        Counter findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType counter = snapshot.domain().types().get(0);

            DomainProperty countProp = counter.properties().stream()
                    .filter(p -> p.name().equals("count"))
                    .findFirst()
                    .orElseThrow();
            assertThat(countProp.nullability()).isEqualTo(Nullability.NON_NULL);

            DomainProperty activeProp = counter.properties().stream()
                    .filter(p -> p.name().equals("active"))
                    .findFirst()
                    .orElseThrow();
            assertThat(activeProp.nullability()).isEqualTo(Nullability.NON_NULL);
        }

        @Test
        @DisplayName("should NOT include static fields in properties")
        void shouldNotIncludeStaticFields() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private static final String TABLE_NAME = "orders";
                        private static int instanceCount = 0;
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
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = snapshot.domain().types().get(0);
            assertThat(order.properties())
                    .as("Static fields should not be included in domain properties")
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("id", "customerName")
                    .doesNotContain("TABLE_NAME", "instanceCount");
        }

        @Test
        @DisplayName("should NOT include transient fields in properties")
        void shouldNotIncludeTransientFields() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String id;
                        private String name;
                        private transient String cachedDisplayName;
                        private transient java.time.Instant lastAccessTime;
                    }
                    """);
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Customer findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType customer = snapshot.domain().types().get(0);
            assertThat(customer.properties())
                    .as("Transient fields should not be included in domain properties")
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("id", "name")
                    .doesNotContain("cachedDisplayName", "lastAccessTime");
        }

        @Test
        @DisplayName("should NOT include static transient fields in properties")
        void shouldNotIncludeStaticTransientFields() throws IOException {
            writeSource("com/example/Product.java", """
                    package com.example;
                    public class Product {
                        private static final String CATEGORY = "default";
                        private transient String computedHash;
                        private static transient java.util.Map<String, Product> cache;
                        private String id;
                        private String name;
                        private double price;
                    }
                    """);
            writeSource("com/example/ProductRepository.java", """
                    package com.example;
                    public interface ProductRepository {
                        Product findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType product = snapshot.domain().types().get(0);
            assertThat(product.properties())
                    .as("Static and transient fields should not be included")
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("id", "name", "price")
                    .doesNotContain("CATEGORY", "computedHash", "cache");
        }
    }

    // =========================================================================
    // Identity Extraction Tests
    // =========================================================================

    @Nested
    @DisplayName("Identity Extraction")
    class IdentityExtractionTest {

        @Test
        @DisplayName("should detect UUID identity as UUID strategy")
        void detectUuidAsUuidStrategy() throws IOException {
            writeSource("com/example/Entity.java", """
                    package com.example;
                    public class Entity {
                        private java.util.UUID id;
                    }
                    """);
            writeSource("com/example/EntityRepository.java", """
                    package com.example;
                    public interface EntityRepository {
                        Entity findById(java.util.UUID id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType entity = snapshot.domain().types().get(0);
            assertThat(entity.hasIdentity()).isTrue();
            Identity identity = entity.identity().orElseThrow();
            assertThat(identity.type().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(identity.strategy()).isEqualTo(IdentityStrategy.UUID);
            assertThat(identity.strategy().isGenerated()).isTrue();
        }

        @Test
        @DisplayName("should detect Long identity as AUTO strategy")
        void detectLongAsAutoStrategy() throws IOException {
            writeSource("com/example/Entity.java", """
                    package com.example;
                    public class Entity {
                        private Long id;
                    }
                    """);
            writeSource("com/example/EntityRepository.java", """
                    package com.example;
                    public interface EntityRepository {
                        Entity findById(Long id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType entity = snapshot.domain().types().get(0);
            Identity identity = entity.identity().orElseThrow();
            assertThat(identity.type().qualifiedName()).isEqualTo("java.lang.Long");
            assertThat(identity.strategy()).isEqualTo(IdentityStrategy.AUTO);
            assertThat(identity.strategy().requiresGeneratedValue()).isTrue();
        }

        @Test
        @DisplayName("should unwrap wrapped identity type")
        void unwrapWrappedIdentity() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public record OrderId(java.util.UUID value) {}
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private OrderId id;
                        private String description;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(OrderId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            Optional<DomainType> orderOpt = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("Order"))
                    .findFirst();

            assertThat(orderOpt).isPresent();
            DomainType order = orderOpt.get();
            assertThat(order.hasIdentity()).isTrue();
            Identity identity = order.identity().orElseThrow();
            assertThat(identity.fieldName()).isEqualTo("id");
            assertThat(identity.type().qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(identity.isWrapped()).isTrue();
            assertThat(identity.strategy()).isEqualTo(IdentityStrategy.UUID);
        }
    }

    // =========================================================================
    // Identity Wrapper Kind Tests
    // =========================================================================

    @Nested
    @DisplayName("Identity Wrapper Kind Detection")
    class IdentityWrapperKindTest {

        @Test
        @DisplayName("should detect RECORD wrapper kind for record identity type")
        void detectRecordWrapperKind() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public record OrderId(java.util.UUID value) {}
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private OrderId id;
                        private String description;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(OrderId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("Order"))
                    .findFirst()
                    .orElseThrow();

            Identity identity = order.identity().orElseThrow();
            assertThat(identity.wrapperKind()).isEqualTo(IdentityWrapperKind.RECORD);
            assertThat(identity.isWrapped()).isTrue();
            assertThat(identity.type().qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should detect CLASS wrapper kind for class identity type")
        void detectClassWrapperKind() throws IOException {
            writeSource("com/example/CustomerId.java", """
                    package com.example;
                    public class CustomerId {
                        private final java.util.UUID value;
                        public CustomerId(java.util.UUID value) { this.value = value; }
                        public java.util.UUID getValue() { return value; }
                    }
                    """);
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private CustomerId id;
                        private String name;
                    }
                    """);
            writeSource("com/example/CustomerRepository.java", """
                    package com.example;
                    public interface CustomerRepository {
                        Customer findById(CustomerId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType customer = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("Customer"))
                    .findFirst()
                    .orElseThrow();

            Identity identity = customer.identity().orElseThrow();
            assertThat(identity.wrapperKind()).isEqualTo(IdentityWrapperKind.CLASS);
            assertThat(identity.isWrapped()).isTrue();
            assertThat(identity.type().qualifiedName()).isEqualTo("com.example.CustomerId");
            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should detect NONE wrapper kind for direct UUID identity")
        void detectNoneWrapperKindForUuid() throws IOException {
            writeSource("com/example/Product.java", """
                    package com.example;
                    public class Product {
                        private java.util.UUID id;
                        private String name;
                    }
                    """);
            writeSource("com/example/ProductRepository.java", """
                    package com.example;
                    public interface ProductRepository {
                        Product findById(java.util.UUID id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType product = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("Product"))
                    .findFirst()
                    .orElseThrow();

            Identity identity = product.identity().orElseThrow();
            assertThat(identity.wrapperKind()).isEqualTo(IdentityWrapperKind.NONE);
            assertThat(identity.isWrapped()).isFalse();
            assertThat(identity.type().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("should detect NONE wrapper kind for direct Long identity")
        void detectNoneWrapperKindForLong() throws IOException {
            writeSource("com/example/Item.java", """
                    package com.example;
                    public class Item {
                        private Long id;
                        private String description;
                    }
                    """);
            writeSource("com/example/ItemRepository.java", """
                    package com.example;
                    public interface ItemRepository {
                        Item findById(Long id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType item = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("Item"))
                    .findFirst()
                    .orElseThrow();

            Identity identity = item.identity().orElseThrow();
            assertThat(identity.wrapperKind()).isEqualTo(IdentityWrapperKind.NONE);
            assertThat(identity.isWrapped()).isFalse();
            assertThat(identity.type().qualifiedName()).isEqualTo("java.lang.Long");
            assertThat(identity.strategy()).isEqualTo(IdentityStrategy.AUTO);
        }

        @Test
        @DisplayName("should provide TypeRef with full information")
        void identityTypeRefHasFullInformation() throws IOException {
            writeSource("com/example/TaskId.java", """
                    package com.example;
                    public record TaskId(java.util.UUID value) {}
                    """);
            writeSource("com/example/Task.java", """
                    package com.example;
                    public class Task {
                        private TaskId id;
                    }
                    """);
            writeSource("com/example/TaskRepository.java", """
                    package com.example;
                    public interface TaskRepository {
                        Task findById(TaskId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType task = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("Task"))
                    .findFirst()
                    .orElseThrow();

            Identity identity = task.identity().orElseThrow();

            // Verify TypeRef has full information
            io.hexaglue.spi.ir.TypeRef typeRef = identity.type();
            assertThat(typeRef.qualifiedName()).isEqualTo("com.example.TaskId");
            assertThat(typeRef.simpleName()).isEqualTo("TaskId");
            assertThat(typeRef.packageName()).isEqualTo("com.example");
            assertThat(typeRef.requiresImport()).isTrue();

            io.hexaglue.spi.ir.TypeRef unwrappedRef = identity.unwrappedType();
            assertThat(unwrappedRef.qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(unwrappedRef.simpleName()).isEqualTo("UUID");
        }
    }

    // =========================================================================
    // Port Export Tests
    // =========================================================================

    @Nested
    @DisplayName("Port Export")
    class PortExportTest {

        @Test
        @DisplayName("should export use case port as DRIVING")
        void exportUseCaseAsDriving() throws IOException {
            // Use method name that doesn't match COMMAND pattern to test USE_CASE naming
            writeSource("com/example/port/in/CreateOrderUseCase.java", """
                    package com.example.port.in;
                    public interface CreateOrderUseCase {
                        void newOrder(String customerId);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example");
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.ports().ports()).hasSize(1);
            Port port = snapshot.ports().ports().get(0);
            assertThat(port.kind()).isEqualTo(PortKind.USE_CASE);
            assertThat(port.direction()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        @DisplayName("should export method parameter types correctly")
        void exportMethodParameters() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        void save(Order order);
                        void deleteById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            Port port = snapshot.ports().ports().get(0);

            PortMethod saveMethod = port.methods().stream()
                    .filter(m -> m.name().equals("save"))
                    .findFirst()
                    .orElseThrow();
            assertThat(saveMethod.parameters()).containsExactly("com.example.Order");
            assertThat(saveMethod.returnType()).isEqualTo("void");

            PortMethod deleteMethod = port.methods().stream()
                    .filter(m -> m.name().equals("deleteById"))
                    .findFirst()
                    .orElseThrow();
            assertThat(deleteMethod.parameters()).containsExactly("java.lang.String");
        }
    }

    // =========================================================================
    // Metadata Tests
    // =========================================================================

    @Nested
    @DisplayName("Metadata")
    class MetadataTest {

        @Test
        @DisplayName("should infer base package from types")
        void inferBasePackage() throws IOException {
            writeSource("com/example/domain/Order.java", """
                    package com.example.domain;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource("com/example/domain/OrderRepository.java", """
                    package com.example.domain;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph("com.example.domain");
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.metadata().basePackage()).isEqualTo("com.example.domain");
        }

        @Test
        @DisplayName("should have timestamp and version")
        void hasTimestampAndVersion() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            assertThat(snapshot.metadata().timestamp()).isNotNull();
            assertThat(snapshot.metadata().engineVersion()).isNotBlank();
        }
    }

    // =========================================================================
    // Filtering Tests
    // =========================================================================

    @Nested
    @DisplayName("Classification Filtering")
    class FilteringTest {

        @Test
        @DisplayName("should skip UNCLASSIFIED types")
        void skipUnclassifiedTypes() throws IOException {
            // A class without an id field and without a repository
            writeSource("com/example/Helper.java", """
                    package com.example;
                    public class Helper {
                        public static void doSomething() {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);

            IrSnapshot snapshot = exporter.export(graph, classifications);

            // Helper should not appear in domain types (unclassified)
            assertThat(snapshot.domain().types()).isEmpty();
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
                GraphMetadata.of(basePackage, 17, (int) model.types().count());

        model = frontend.build(input);
        return builder.build(model, metadata);
    }

    private List<ClassificationResult> classifyAll(ApplicationGraph graph) {
        GraphQuery query = graph.query();
        List<ClassificationResult> results = new ArrayList<>();

        for (TypeNode type : graph.typeNodes()) {
            // Try domain classification first
            ClassificationResult domainResult = domainClassifier.classify(type, query);
            if (domainResult.isClassified()) {
                results.add(domainResult);
                continue;
            }

            // Then try port classification
            ClassificationResult portResult = portClassifier.classify(type, query);
            if (portResult.isClassified()) {
                results.add(portResult);
            }
        }

        return results;
    }
}
