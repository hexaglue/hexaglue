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

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
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
        builder = new GraphBuilder(true);
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
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
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
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.ENTITY);
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
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.VALUE_OBJECT);
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
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.IDENTIFIER);
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
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.AGGREGATE_ROOT);
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
    class HasIdentityCriteriaTest {

        @Test
        void shouldMatchClassWithIdField() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String id;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode customer = graph.typeNode("com.example.Customer").orElseThrow();
            GraphQuery query = graph.query();

            HasIdentityCriteria criteria = new HasIdentityCriteria();
            MatchResult result = criteria.evaluate(customer, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.justification()).contains("id");
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.ENTITY);
        }

        @Test
        void shouldMatchClassWithCustomIdField() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String customerId;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode customer = graph.typeNode("com.example.Customer").orElseThrow();
            GraphQuery query = graph.query();

            HasIdentityCriteria criteria = new HasIdentityCriteria();
            MatchResult result = criteria.evaluate(customer, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("customerId");
        }

        @Test
        void shouldNotMatchRecord() throws IOException {
            writeSource("com/example/Address.java", """
                    package com.example;
                    public record Address(String id, String street) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode address = graph.typeNode("com.example.Address").orElseThrow();
            GraphQuery query = graph.query();

            HasIdentityCriteria criteria = new HasIdentityCriteria();
            MatchResult result = criteria.evaluate(address, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchClassWithoutIdField() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode customer = graph.typeNode("com.example.Customer").orElseThrow();
            GraphQuery query = graph.query();

            HasIdentityCriteria criteria = new HasIdentityCriteria();
            MatchResult result = criteria.evaluate(customer, query);

            assertThat(result.matched()).isFalse();
        }
    }

    @Nested
    class ImmutableNoIdCriteriaTest {

        @Test
        void shouldMatchRecordWithoutIdField() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    public record Money(int amount, String currency) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode money = graph.typeNode("com.example.Money").orElseThrow();
            GraphQuery query = graph.query();

            ImmutableNoIdCriteria criteria = new ImmutableNoIdCriteria();
            MatchResult result = criteria.evaluate(money, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
            assertThat(result.justification()).contains("record");
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.VALUE_OBJECT);
        }

        @Test
        void shouldMatchImmutableClassWithoutIdField() throws IOException {
            writeSource("com/example/Address.java", """
                    package com.example;
                    public class Address {
                        private final String street;
                        private final String city;
                        public Address(String street, String city) {
                            this.street = street;
                            this.city = city;
                        }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode address = graph.typeNode("com.example.Address").orElseThrow();
            GraphQuery query = graph.query();

            ImmutableNoIdCriteria criteria = new ImmutableNoIdCriteria();
            MatchResult result = criteria.evaluate(address, query);

            assertThat(result.matched()).isTrue();
            assertThat(result.justification()).contains("private final");
        }

        @Test
        void shouldNotMatchTypeWithIdField() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    public record Order(String id, String name) {}
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode order = graph.typeNode("com.example.Order").orElseThrow();
            GraphQuery query = graph.query();

            ImmutableNoIdCriteria criteria = new ImmutableNoIdCriteria();
            MatchResult result = criteria.evaluate(order, query);

            assertThat(result.matched()).isFalse();
        }

        @Test
        void shouldNotMatchMutableClass() throws IOException {
            writeSource("com/example/MutableData.java", """
                    package com.example;
                    public class MutableData {
                        private String value;
                        public void setValue(String v) { this.value = v; }
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            TypeNode data = graph.typeNode("com.example.MutableData").orElseThrow();
            GraphQuery query = graph.query();

            ImmutableNoIdCriteria criteria = new ImmutableNoIdCriteria();
            MatchResult result = criteria.evaluate(data, query);

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
            assertThat(criteria.targetKind()).isEqualTo(DomainKind.IDENTIFIER);
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
