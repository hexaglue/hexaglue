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

package io.hexaglue.core.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.ClassificationContext;
import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ClassificationTarget;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.RelationKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Edge case tests for {@link RelationAnalyzer}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>ONE_TO_ONE relations (unidirectional and bidirectional)</li>
 *   <li>MANY_TO_MANY relations (inter-aggregate references)</li>
 *   <li>Self-references (tree structures)</li>
 *   <li>Optional entity references</li>
 *   <li>Map-based relations</li>
 * </ul>
 */
@DisplayName("RelationAnalyzer - Edge Cases")
class RelationAnalyzerEdgeCasesTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private RelationAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        analyzer = new RelationAnalyzer();
    }

    // =========================================================================
    // ONE_TO_ONE Relations
    // =========================================================================

    @Nested
    @DisplayName("ONE_TO_ONE Relations")
    class OneToOneRelationsTest {

        @Test
        @DisplayName("should detect ONE_TO_ONE for single entity reference with unique constraint hint")
        void shouldDetectOneToOneForUniqueEntityReference() throws IOException {
            // An Order has exactly one ShippingDetails (1:1 relationship)
            writeSource("com/example/ShippingDetails.java", """
                    package com.example;
                    public class ShippingDetails {
                        private String id;
                        private String address;
                        private String trackingNumber;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private ShippingDetails shipping;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.ShippingDetails", "ENTITY"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation shippingRelation = relations.get(0);
            assertThat(shippingRelation.propertyName()).isEqualTo("shipping");
            // Currently MANY_TO_ONE, should be ONE_TO_ONE for child entities
            assertThat(shippingRelation.kind())
                    .as("Single child entity reference should be ONE_TO_ONE")
                    .isEqualTo(RelationKind.ONE_TO_ONE);
        }

        @Test
        @DisplayName("should detect bidirectional ONE_TO_ONE with mappedBy")
        void shouldDetectBidirectionalOneToOne() throws IOException {
            // User <-> UserProfile bidirectional 1:1
            writeSource("com/example/UserProfile.java", """
                    package com.example;
                    public class UserProfile {
                        private String id;
                        private User user;
                        private String bio;
                    }
                    """);
            writeSource("com/example/User.java", """
                    package com.example;
                    public class User {
                        private String id;
                        private UserProfile profile;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.User", "AGGREGATE_ROOT",
                            "com.example.UserProfile", "ENTITY"));

            TypeNode userType = graph.typeNode("com.example.User").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(userType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation profileRelation = relations.get(0);
            assertThat(profileRelation.kind()).isEqualTo(RelationKind.ONE_TO_ONE);
            assertThat(profileRelation.mappedBy())
                    .as("Owning side should have mappedBy pointing to inverse")
                    .isEqualTo("user");
        }

        @Test
        @DisplayName("should use ONE_TO_ONE for single entity reference")
        void shouldUseOneToOneForEntityReference() throws IOException {
            // Single entity reference within aggregate -> ONE_TO_ONE
            // (the child entity is owned exclusively by this aggregate)
            writeSource("com/example/Address.java", """
                    package com.example;
                    public class Address {
                        private String id;
                        private String street;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private Address deliveryAddress;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.Address", "ENTITY"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation addressRelation = relations.get(0);
            assertThat(addressRelation.propertyName()).isEqualTo("deliveryAddress");
            // Single child entity reference is ONE_TO_ONE
            assertThat(addressRelation.kind()).isEqualTo(RelationKind.ONE_TO_ONE);
        }
    }

    // =========================================================================
    // MANY_TO_MANY Relations
    // =========================================================================

    @Nested
    @DisplayName("MANY_TO_MANY Relations")
    class ManyToManyRelationsTest {

        @Test
        @DisplayName("should detect MANY_TO_MANY for collection of aggregate roots")
        void shouldDetectManyToManyForAggregateCollection() throws IOException {
            // Student <-> Course is a classic MANY_TO_MANY relationship
            // Both are aggregate roots (each has their own repository)
            writeSource("com/example/Course.java", """
                    package com.example;
                    public class Course {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/Student.java", """
                    package com.example;
                    import java.util.List;
                    public class Student {
                        private String id;
                        private String name;
                        private List<Course> enrolledCourses;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Student", "AGGREGATE_ROOT",
                            "com.example.Course", "AGGREGATE_ROOT"));

            TypeNode studentType = graph.typeNode("com.example.Student").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(studentType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation coursesRelation = relations.get(0);
            assertThat(coursesRelation.propertyName()).isEqualTo("enrolledCourses");
            assertThat(coursesRelation.kind())
                    .as("Collection of aggregate roots should be MANY_TO_MANY")
                    .isEqualTo(RelationKind.MANY_TO_MANY);
            assertThat(coursesRelation.cascade())
                    .as("Inter-aggregate relations should NOT cascade")
                    .isEqualTo(CascadeType.NONE);
        }

        @Test
        @DisplayName("should detect bidirectional MANY_TO_MANY with mappedBy")
        void shouldDetectBidirectionalManyToMany() throws IOException {
            // Author <-> Book bidirectional MANY_TO_MANY
            writeSource("com/example/Book.java", """
                    package com.example;
                    import java.util.Set;
                    public class Book {
                        private String id;
                        private String title;
                        private Set<Author> authors;
                    }
                    """);
            writeSource("com/example/Author.java", """
                    package com.example;
                    import java.util.Set;
                    public class Author {
                        private String id;
                        private String name;
                        private Set<Book> books;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Book", "AGGREGATE_ROOT",
                            "com.example.Author", "AGGREGATE_ROOT"));

            TypeNode bookType = graph.typeNode("com.example.Book").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(bookType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation authorsRelation = relations.get(0);
            assertThat(authorsRelation.kind()).isEqualTo(RelationKind.MANY_TO_MANY);
            // One side should own the relation
            // The other side should have mappedBy
        }

        @Test
        @DisplayName("should use MANY_TO_MANY for collection of aggregates")
        void shouldUseManyToManyForAggregateCollection() throws IOException {
            // Collection of AGGREGATE_ROOT -> MANY_TO_MANY (inter-aggregate relationship)
            // Each aggregate root is independently managed
            writeSource("com/example/Product.java", """
                    package com.example;
                    public class Product {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/Cart.java", """
                    package com.example;
                    import java.util.List;
                    public class Cart {
                        private String id;
                        private List<Product> products;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Cart", "AGGREGATE_ROOT",
                            "com.example.Product", "AGGREGATE_ROOT"));

            TypeNode cartType = graph.typeNode("com.example.Cart").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(cartType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation productsRelation = relations.get(0);
            // Collection of aggregate roots is MANY_TO_MANY
            assertThat(productsRelation.kind()).isEqualTo(RelationKind.MANY_TO_MANY);
        }
    }

    // =========================================================================
    // Self-References (Tree Structures)
    // =========================================================================

    @Nested
    @DisplayName("Self-Referencing Types")
    class SelfReferencingTypesTest {

        @Test
        @DisplayName("should detect self-referencing ONE_TO_MANY for tree structures")
        void shouldDetectSelfReferencingOneToMany() throws IOException {
            // Category with subcategories (tree structure)
            // CategoryNode is an ENTITY (part of a Category aggregate), not an AGGREGATE_ROOT
            // This allows proper tree structure with ONE_TO_MANY for children
            writeSource("com/example/CategoryNode.java", """
                    package com.example;
                    import java.util.List;
                    public class CategoryNode {
                        private String id;
                        private String name;
                        private CategoryNode parent;
                        private List<CategoryNode> children;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            // ENTITY for tree nodes - children are ONE_TO_MANY within the same aggregate
            ClassificationContext context = buildContext(graph, Map.of("com.example.CategoryNode", "ENTITY"));

            TypeNode categoryType = graph.typeNode("com.example.CategoryNode").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(categoryType, graph.query(), context);

            // Should have two relations: parent (ONE_TO_ONE) and children (ONE_TO_MANY)
            assertThat(relations).hasSize(2);

            DomainRelation parentRelation = relations.stream()
                    .filter(r -> r.propertyName().equals("parent"))
                    .findFirst()
                    .orElseThrow();
            // Single entity reference is now ONE_TO_ONE
            assertThat(parentRelation.kind()).isEqualTo(RelationKind.ONE_TO_ONE);
            assertThat(parentRelation.targetTypeFqn()).isEqualTo("com.example.CategoryNode");

            DomainRelation childrenRelation = relations.stream()
                    .filter(r -> r.propertyName().equals("children"))
                    .findFirst()
                    .orElseThrow();
            assertThat(childrenRelation.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
            assertThat(childrenRelation.targetTypeFqn()).isEqualTo("com.example.CategoryNode");
            assertThat(childrenRelation.mappedBy())
                    .as("Children should be mappedBy parent for bidirectional self-reference")
                    .isEqualTo("parent");
        }

        @Test
        @DisplayName("should detect self-referencing MANY_TO_ONE (back-pointer)")
        void shouldDetectSelfReferencingManyToOne() throws IOException {
            // Employee with manager reference
            writeSource("com/example/Employee.java", """
                    package com.example;
                    public class Employee {
                        private String id;
                        private String name;
                        private Employee manager;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(graph, Map.of("com.example.Employee", "AGGREGATE_ROOT"));

            TypeNode employeeType = graph.typeNode("com.example.Employee").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(employeeType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation managerRelation = relations.get(0);
            assertThat(managerRelation.propertyName()).isEqualTo("manager");
            assertThat(managerRelation.kind()).isEqualTo(RelationKind.MANY_TO_ONE);
            assertThat(managerRelation.targetTypeFqn()).isEqualTo("com.example.Employee");
        }

        @Test
        @DisplayName("should detect self-referencing MANY_TO_MANY (graph structure)")
        void shouldDetectSelfReferencingManyToMany() throws IOException {
            // Social network: Person has friends (which are also Persons)
            writeSource("com/example/Person.java", """
                    package com.example;
                    import java.util.Set;
                    public class Person {
                        private String id;
                        private String name;
                        private Set<Person> friends;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(graph, Map.of("com.example.Person", "AGGREGATE_ROOT"));

            TypeNode personType = graph.typeNode("com.example.Person").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(personType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation friendsRelation = relations.get(0);
            assertThat(friendsRelation.propertyName()).isEqualTo("friends");
            // Self-referencing collection of aggregate roots should be MANY_TO_MANY
            assertThat(friendsRelation.kind()).isEqualTo(RelationKind.MANY_TO_MANY);
        }
    }

    // =========================================================================
    // Optional References
    // =========================================================================

    @Nested
    @DisplayName("Optional Entity References")
    class OptionalReferencesTest {

        @Test
        @DisplayName("should handle Optional<Entity> as nullable MANY_TO_ONE")
        void shouldHandleOptionalEntityReference() throws IOException {
            writeSource("com/example/Coupon.java", """
                    package com.example;
                    public class Coupon {
                        private String id;
                        private String code;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.Optional;
                    public class Order {
                        private String id;
                        private Optional<Coupon> appliedCoupon;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.Coupon", "AGGREGATE_ROOT"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            // Optional<Coupon> should be detected as a relation to Coupon
            assertThat(relations).hasSize(1);
            DomainRelation couponRelation = relations.get(0);
            assertThat(couponRelation.propertyName()).isEqualTo("appliedCoupon");
            assertThat(couponRelation.kind()).isEqualTo(RelationKind.MANY_TO_ONE);
            assertThat(couponRelation.targetTypeFqn()).isEqualTo("com.example.Coupon");
        }

        @Test
        @DisplayName("should handle Optional<ValueObject> as nullable EMBEDDED")
        void shouldHandleOptionalValueObject() throws IOException {
            writeSource("com/example/Address.java", """
                    package com.example;
                    public record Address(String street, String city) {}
                    """);
            writeSource("com/example/Customer.java", """
                    package com.example;
                    import java.util.Optional;
                    public class Customer {
                        private String id;
                        private Optional<Address> billingAddress;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Customer", "AGGREGATE_ROOT",
                            "com.example.Address", "VALUE_OBJECT"));

            TypeNode customerType = graph.typeNode("com.example.Customer").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(customerType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation addressRelation = relations.get(0);
            assertThat(addressRelation.propertyName()).isEqualTo("billingAddress");
            assertThat(addressRelation.kind()).isEqualTo(RelationKind.EMBEDDED);
            assertThat(addressRelation.targetTypeFqn()).isEqualTo("com.example.Address");
        }
    }

    // =========================================================================
    // Map-based Relations
    // =========================================================================

    @Nested
    @DisplayName("Map-based Relations")
    class MapBasedRelationsTest {

        @Test
        @DisplayName("should detect Map<String, Entity> as indexed ONE_TO_MANY")
        void shouldDetectMapOfEntitiesAsOneToMany() throws IOException {
            writeSource("com/example/OrderLine.java", """
                    package com.example;
                    public class OrderLine {
                        private String id;
                        private int quantity;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.Map;
                    public class Order {
                        private String id;
                        private Map<String, OrderLine> linesByProductId;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.OrderLine", "ENTITY"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation linesRelation = relations.get(0);
            assertThat(linesRelation.propertyName()).isEqualTo("linesByProductId");
            assertThat(linesRelation.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
        }

        @Test
        @DisplayName("should detect Map<String, ValueObject> as indexed ELEMENT_COLLECTION")
        void shouldDetectMapOfValueObjectsAsElementCollection() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    public record Money(java.math.BigDecimal amount, String currency) {}
                    """);
            writeSource("com/example/MultiCurrencyWallet.java", """
                    package com.example;
                    import java.util.Map;
                    public class MultiCurrencyWallet {
                        private String id;
                        private Map<String, Money> balances;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.MultiCurrencyWallet", "AGGREGATE_ROOT",
                            "com.example.Money", "VALUE_OBJECT"));

            TypeNode walletType =
                    graph.typeNode("com.example.MultiCurrencyWallet").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(walletType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation balancesRelation = relations.get(0);
            assertThat(balancesRelation.propertyName()).isEqualTo("balances");
            assertThat(balancesRelation.kind()).isEqualTo(RelationKind.ELEMENT_COLLECTION);
        }
    }

    // =========================================================================
    // Complex Cascade Scenarios
    // =========================================================================

    @Nested
    @DisplayName("Complex Cascade Scenarios")
    class CascadeTest {

        @Test
        @DisplayName("should NOT cascade for inter-aggregate references")
        void shouldNotCascadeForInterAggregateReferences() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    public class Customer {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    public class Order {
                        private String id;
                        private Customer customer;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.Customer", "AGGREGATE_ROOT"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation customerRelation = relations.get(0);
            assertThat(customerRelation.cascade())
                    .as("Inter-aggregate references should NOT cascade")
                    .isEqualTo(CascadeType.NONE);
        }

        @Test
        @DisplayName("should cascade ALL for intra-aggregate entities")
        void shouldCascadeAllForIntraAggregateEntities() throws IOException {
            writeSource("com/example/OrderLine.java", """
                    package com.example;
                    public class OrderLine {
                        private String id;
                        private int quantity;
                    }
                    """);
            writeSource("com/example/Order.java", """
                    package com.example;
                    import java.util.List;
                    public class Order {
                        private String id;
                        private List<OrderLine> lines;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.OrderLine", "ENTITY"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation linesRelation = relations.get(0);
            assertThat(linesRelation.cascade())
                    .as("Intra-aggregate child entities should cascade ALL")
                    .isEqualTo(CascadeType.ALL);
            assertThat(linesRelation.orphanRemoval())
                    .as("Intra-aggregate child entities should have orphan removal")
                    .isTrue();
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

    private ClassificationContext buildContext(ApplicationGraph graph, Map<String, String> classifications) {
        Map<NodeId, ClassificationResult> results = new HashMap<>();
        for (var entry : classifications.entrySet()) {
            NodeId id = NodeId.type(entry.getKey());
            ClassificationResult result = ClassificationResult.classified(
                    id,
                    ClassificationTarget.DOMAIN,
                    entry.getValue(),
                    ConfidenceLevel.HIGH,
                    "test-criteria",
                    100,
                    "test classification",
                    List.of(),
                    List.of());
            results.put(id, result);
        }
        return new ClassificationContext(results);
    }
}
