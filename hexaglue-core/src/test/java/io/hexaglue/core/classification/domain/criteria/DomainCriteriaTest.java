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

package io.hexaglue.core.classification.domain.criteria;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for domain classification criteria.
 */
class DomainCriteriaTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
    }

    // =========================================================================
    // Explicit Annotation Criteria
    // =========================================================================

    @Nested
    class ExplicitAggregateRootCriteriaTest {

        @Test
        void shouldMatchTypeWithAggregateRootAnnotation() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    @AggregateRoot
                    public class Order {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitAggregateRootCriteria criteria = new ExplicitAggregateRootCriteria();
            MatchResult result = criteria.evaluate(order, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("@AggregateRoot");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(criteria.priority()).isEqualTo(100);
        }

        @Test
        void shouldNotMatchTypeWithoutAnnotation() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitAggregateRootCriteria criteria = new ExplicitAggregateRootCriteria();
            MatchResult result = criteria.evaluate(order, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class ExplicitEntityCriteriaTest {

        @Test
        void shouldMatchTypeWithEntityAnnotation() throws IOException {
            writeSource("com/example/LineItem.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.Entity;
                    @Entity
                    public class LineItem {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode lineItem = graph.typeNode("com.example.LineItem").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitEntityCriteria criteria = new ExplicitEntityCriteria();
            MatchResult result = criteria.evaluate(lineItem, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.ENTITY);
        }
    }

    @Nested
    class ExplicitValueObjectCriteriaTest {

        @Test
        void shouldMatchTypeWithValueObjectAnnotation() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    @ValueObject
                    public record Money(int amount, String currency) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode money = graph.typeNode("com.example.Money").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitValueObjectCriteria criteria = new ExplicitValueObjectCriteria();
            MatchResult result = criteria.evaluate(money, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.VALUE_OBJECT);
        }
    }

    @Nested
    class ExplicitIdentifierCriteriaTest {

        @Test
        void shouldMatchTypeImplementingIdentifierInterface() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    import org.jmolecules.ddd.types.Identifier;
                    public record OrderId(String value) implements Identifier {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            ExplicitIdentifierCriteria criteria = new ExplicitIdentifierCriteria();
            MatchResult result = criteria.evaluate(orderId, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.EXPLICIT);
            assertThat(result.justification()).contains("Identifier");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.IDENTIFIER);
        }
    }

    // =========================================================================
    // Heuristic Criteria
    // =========================================================================

    @Nested
    class RepositoryDominantCriteriaTest {

        @Test
        void shouldMatchTypeUsedInRepositoryWithIdField() throws IOException {
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
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            RepositoryDominantCriteria criteria = new RepositoryDominantCriteria();
            MatchResult result = criteria.evaluate(order, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("repository");
            assertThat(result.justification()).contains("id");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.AGGREGATE_ROOT);
            assertThat(criteria.priority()).isEqualTo(80);
        }

        @Test
        void shouldNotMatchTypeWithoutIdField() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String name;
                    }
                    """);
            writeSource("com/example/OrderRepository.java", """
                    package com.example;
                    public interface OrderRepository {
                        Order findByName(String name);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            RepositoryDominantCriteria criteria = new RepositoryDominantCriteria();
            MatchResult result = criteria.evaluate(order, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchTypeNotInRepository() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource("com/example/OrderService.java", """
                    package com.example;
                    public class OrderService {
                        public Order process(Order order) { return order; }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            RepositoryDominantCriteria criteria = new RepositoryDominantCriteria();
            MatchResult result = criteria.evaluate(order, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class RecordSingleIdCriteriaTest {

        @Test
        void shouldMatchRecordWithSingleComponentAndIdSuffix() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public record OrderId(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            RecordSingleIdCriteria criteria = new RecordSingleIdCriteria();
            MatchResult result = criteria.evaluate(orderId, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("OrderId");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.IDENTIFIER);
            assertThat(criteria.priority()).isEqualTo(80);
        }

        @Test
        void shouldMatchRecordWrappingUUID() throws IOException {
            writeSource("com/example/CustomerId.java", """
                    package com.example;
                    import java.util.UUID;
                    public record CustomerId(UUID value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode customerId = graph.typeNode("com.example.CustomerId").orElseThrow();
            GraphQuery query = graph.query();

            RecordSingleIdCriteria criteria = new RecordSingleIdCriteria();
            MatchResult result = criteria.evaluate(customerId, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("UUID");
        }

        @Test
        void shouldNotMatchRecordWithMultipleComponents() throws IOException {
            writeSource("com/example/CompoundId.java", """
                    package com.example;
                    public record CompoundId(String tenant, String id) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode compoundId = graph.typeNode("com.example.CompoundId").orElseThrow();
            GraphQuery query = graph.query();

            RecordSingleIdCriteria criteria = new RecordSingleIdCriteria();
            MatchResult result = criteria.evaluate(compoundId, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchRecordWithoutIdSuffix() throws IOException {
            writeSource("com/example/OrderNumber.java", """
                    package com.example;
                    public record OrderNumber(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderNumber = graph.typeNode("com.example.OrderNumber").orElseThrow();
            GraphQuery query = graph.query();

            RecordSingleIdCriteria criteria = new RecordSingleIdCriteria();
            MatchResult result = criteria.evaluate(orderNumber, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClass() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public class OrderId {
                        private final String value;
                        public OrderId(String value) { this.value = value; }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            RecordSingleIdCriteria criteria = new RecordSingleIdCriteria();
            MatchResult result = criteria.evaluate(orderId, query);

            assertThat(result.matched()).isFalse();
        }
    }

    // =========================================================================
    // Medium Heuristic Criteria
    // =========================================================================

    @Nested
    class ContainedEntityCriteriaTest {

        @Test
        void shouldMatchClassWithIdFieldContainedInAggregateViaCollection() throws IOException {
            writeSource("com/example/OrderLine.java", """
                    package com.example;
                    public class OrderLine {
                        private Long id;
                        private String productName;
                        private int quantity;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.List;
                    public class Order {
                        private Long id;
                        private List<OrderLine> lines;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderLine = graph.typeNode("com.example.OrderLine").orElseThrow();
            GraphQuery query = graph.query();

            ContainedEntityCriteria criteria = new ContainedEntityCriteria();
            MatchResult result = criteria.evaluate(orderLine, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("Order");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.ENTITY);
            assertThat(criteria.priority()).isEqualTo(70);
        }

        @Test
        void shouldMatchClassWithTypedIdFieldContainedInAggregate() throws IOException {
            writeSource("com/example/StockMovement.java", """
                    package com.example;
                    public class StockMovement {
                        private Long stockMovementId;
                        private String type;
                    }
                    """);
            writeSource("com/example/Inventory.java", """
                    package com.example;
                    import java.util.List;
                    public class Inventory {
                        private Long id;
                        private List<StockMovement> movements;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode movement = graph.typeNode("com.example.StockMovement").orElseThrow();
            GraphQuery query = graph.query();

            ContainedEntityCriteria criteria = new ContainedEntityCriteria();
            MatchResult result = criteria.evaluate(movement, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.HIGH);
            assertThat(result.justification()).contains("Inventory");
        }

        @Test
        void shouldNotMatchClassWithoutIdField() throws IOException {
            writeSource("com/example/LineItem.java", """
                    package com.example;
                    public class LineItem {
                        private String productName;
                        private int quantity;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.List;
                    public class Order {
                        private Long id;
                        private List<LineItem> items;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode lineItem = graph.typeNode("com.example.LineItem").orElseThrow();
            GraphQuery query = graph.query();

            ContainedEntityCriteria criteria = new ContainedEntityCriteria();
            MatchResult result = criteria.evaluate(lineItem, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchInterface() throws IOException {
            writeSource("com/example/Identifiable.java", """
                    package com.example;
                    public interface Identifiable {
                        Long getId();
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode iface = graph.typeNode("com.example.Identifiable").orElseThrow();
            GraphQuery query = graph.query();

            ContainedEntityCriteria criteria = new ContainedEntityCriteria();
            MatchResult result = criteria.evaluate(iface, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClassNotContainedInAnyAggregate() throws IOException {
            writeSource("com/example/StandaloneEntity.java", """
                    package com.example;
                    public class StandaloneEntity {
                        private Long id;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode entity = graph.typeNode("com.example.StandaloneEntity").orElseThrow();
            GraphQuery query = graph.query();

            ContainedEntityCriteria criteria = new ContainedEntityCriteria();
            MatchResult result = criteria.evaluate(entity, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchWhenContainerHasNoIdentity() throws IOException {
            writeSource("com/example/Item.java", """
                    package com.example;
                    public class Item {
                        private Long id;
                        private String name;
                    }
                    """);
            writeSource("com/example/Bag.java", """
                    package com.example;
                    import java.util.List;
                    public class Bag {
                        private String label;
                        private List<Item> items;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode item = graph.typeNode("com.example.Item").orElseThrow();
            GraphQuery query = graph.query();

            ContainedEntityCriteria criteria = new ContainedEntityCriteria();
            MatchResult result = criteria.evaluate(item, query);

            assertThat(result.matched()).isFalse();
        }
    }

    // =========================================================================
    // Naming Heuristic Criteria
    // =========================================================================

    @Nested
    class DomainEventNamingCriteriaTest {

        @Test
        void shouldMatchClassEndingWithEvent() throws IOException {
            writeSource("com/example/OrderCreatedEvent.java", """
                    package com.example;
                    public class OrderCreatedEvent {
                        private String orderId;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode event = graph.typeNode("com.example.OrderCreatedEvent").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(event, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.justification()).contains("OrderCreatedEvent");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.DOMAIN_EVENT);
            assertThat(criteria.priority()).isEqualTo(68);
        }

        @Test
        void shouldMatchRecordEndingWithEvent() throws IOException {
            writeSource("com/example/PaymentReceivedEvent.java", """
                    package com.example;
                    public record PaymentReceivedEvent(String paymentId, double amount) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode event = graph.typeNode("com.example.PaymentReceivedEvent").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(event, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        }

        @Test
        void shouldNotMatchInterfaceEndingWithEvent() throws IOException {
            writeSource("com/example/ApplicationEvent.java", """
                    package com.example;
                    public interface ApplicationEvent {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode iface = graph.typeNode("com.example.ApplicationEvent").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(iface, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchEnumEndingWithEvent() throws IOException {
            writeSource("com/example/SystemEvent.java", """
                    package com.example;
                    public enum SystemEvent { STARTED, STOPPED }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode enumType = graph.typeNode("com.example.SystemEvent").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(enumType, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClassNamedExactlyEvent() throws IOException {
            writeSource("com/example/Event.java", """
                    package com.example;
                    public class Event {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode event = graph.typeNode("com.example.Event").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(event, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClassNamedDomainEvent() throws IOException {
            writeSource("com/example/DomainEvent.java", """
                    package com.example;
                    public class DomainEvent {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode event = graph.typeNode("com.example.DomainEvent").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(event, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClassNotEndingWithEvent() throws IOException {
            writeSource("com/example/OrderCreated.java", """
                    package com.example;
                    public class OrderCreated {
                        private String orderId;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode type = graph.typeNode("com.example.OrderCreated").orElseThrow();
            GraphQuery query = graph.query();

            DomainEventNamingCriteria criteria = new DomainEventNamingCriteria();
            MatchResult result = criteria.evaluate(type, query);

            assertThat(result.matched()).isFalse();
        }
    }

    // =========================================================================
    // Domain Record Value Object Criteria
    // =========================================================================

    @Nested
    class DomainRecordValueObjectCriteriaTest {

        @Test
        void shouldMatchRecordWithoutIdentityReferencedByOtherType() throws IOException {
            writeSource("com/example/ShippingRate.java", """
                    package com.example;
                    public record ShippingRate(double amount, String currency) {}
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private ShippingRate shippingRate;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode shippingRate = graph.typeNode("com.example.ShippingRate").orElseThrow();
            GraphQuery query = graph.query();

            DomainRecordValueObjectCriteria criteria = new DomainRecordValueObjectCriteria();
            MatchResult result = criteria.evaluate(shippingRate, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.justification()).contains("ShippingRate");
            assertThat(criteria.targetKind()).isEqualTo(ElementKind.VALUE_OBJECT);
            assertThat(criteria.priority()).isEqualTo(65);
        }

        @Test
        void shouldNotMatchRecordEndingWithId() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    public record OrderId(String value) {}
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private OrderId orderId;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderId = graph.typeNode("com.example.OrderId").orElseThrow();
            GraphQuery query = graph.query();

            DomainRecordValueObjectCriteria criteria = new DomainRecordValueObjectCriteria();
            MatchResult result = criteria.evaluate(orderId, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchRecordEndingWithEvent() throws IOException {
            writeSource("com/example/OrderCreatedEvent.java", """
                    package com.example;
                    public record OrderCreatedEvent(String orderId) {}
                    """);
            writeSource("com/example/EventHandler.java", """
                    package com.example;
                    public class EventHandler {
                        public void handle(OrderCreatedEvent event) {}
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode event = graph.typeNode("com.example.OrderCreatedEvent").orElseThrow();
            GraphQuery query = graph.query();

            DomainRecordValueObjectCriteria criteria = new DomainRecordValueObjectCriteria();
            MatchResult result = criteria.evaluate(event, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchRecordWithIdentityField() throws IOException {
            writeSource("com/example/OrderLine.java", """
                    package com.example;
                    public record OrderLine(Long id, String product, int quantity) {}
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.List;
                    public class Order {
                        private List<OrderLine> lines;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orderLine = graph.typeNode("com.example.OrderLine").orElseThrow();
            GraphQuery query = graph.query();

            DomainRecordValueObjectCriteria criteria = new DomainRecordValueObjectCriteria();
            MatchResult result = criteria.evaluate(orderLine, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClass() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    public class Money {
                        private final double amount;
                        private final String currency;
                        public Money(double amount, String currency) {
                            this.amount = amount;
                            this.currency = currency;
                        }
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private Money total;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode money = graph.typeNode("com.example.Money").orElseThrow();
            GraphQuery query = graph.query();

            DomainRecordValueObjectCriteria criteria = new DomainRecordValueObjectCriteria();
            MatchResult result = criteria.evaluate(money, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchOrphanedRecord() throws IOException {
            writeSource("com/example/Orphan.java", """
                    package com.example;
                    public record Orphan(String value) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode orphan = graph.typeNode("com.example.Orphan").orElseThrow();
            GraphQuery query = graph.query();

            DomainRecordValueObjectCriteria criteria = new DomainRecordValueObjectCriteria();
            MatchResult result = criteria.evaluate(orphan, query);

            assertThat(result.matched()).isFalse();
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
