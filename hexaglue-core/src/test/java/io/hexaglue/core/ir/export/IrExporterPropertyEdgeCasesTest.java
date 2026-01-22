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
import io.hexaglue.core.frontend.CachedSpoonAnalyzer;
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
 * Edge case tests for IrExporter property handling.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Complex generic types (Map, nested generics)</li>
 *   <li>Array types</li>
 *   <li>Primitive wrapper types</li>
 *   <li>Collection nesting</li>
 * </ul>
 */
@DisplayName("IrExporter - Property Edge Cases")
class IrExporterPropertyEdgeCasesTest {

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
        CachedSpoonAnalyzer analyzer = new CachedSpoonAnalyzer();
        builder = new GraphBuilder(true, analyzer);
        domainClassifier = new DomainClassifier();
        portClassifier = new PortClassifier();
        exporter = new IrExporter();
    }

    // =========================================================================
    // Simple Collection Types
    // =========================================================================

    @Nested
    @DisplayName("Simple Collection Types")
    class SimpleCollectionTypesTests {

        @Test
        @DisplayName("should extract List<String> property with COLLECTION cardinality")
        void shouldExtractListStringProperty() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    import java.util.List;

                    @AggregateRoot
                    public class Order {
                        private String id;
                        private List<String> tags;
                    }
                    """);

            DomainType order = classifyAndExport("com.example.Order");

            DomainProperty tagsProperty = findProperty(order, "tags");
            assertThat(tagsProperty.cardinality())
                    .as("List property should have COLLECTION cardinality")
                    .isEqualTo(Cardinality.COLLECTION);
            assertThat(tagsProperty.type().qualifiedName()).isEqualTo("java.util.List");
        }

        @Test
        @DisplayName("should extract Set<UUID> property")
        void shouldExtractSetUuidProperty() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    import java.util.Set;
                    import java.util.UUID;

                    @AggregateRoot
                    public class Customer {
                        private UUID id;
                        private Set<UUID> linkedAccounts;
                    }
                    """);

            DomainType customer = classifyAndExport("com.example.Customer");

            DomainProperty linkedAccounts = findProperty(customer, "linkedAccounts");
            assertThat(linkedAccounts.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }
    }

    // =========================================================================
    // Nested Generic Types
    // =========================================================================

    @Nested
    @DisplayName("Nested Generic Types")
    class NestedGenericTypesTests {

        @Test
        @DisplayName("should extract Map<String, List<OrderId>> property")
        void shouldExtractMapWithListValue() throws IOException {
            writeSource("com/example/OrderId.java", """
                    package com.example;
                    import java.util.UUID;
                    public record OrderId(UUID value) {}
                    """);
            writeSource("com/example/OrderBatch.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    import java.util.Map;
                    import java.util.List;

                    @AggregateRoot
                    public class OrderBatch {
                        private String id;
                        private Map<String, List<OrderId>> ordersByRegion;
                    }
                    """);

            DomainType batch = classifyAndExport("com.example.OrderBatch");

            DomainProperty prop = findProperty(batch, "ordersByRegion");
            assertThat(prop.type().qualifiedName())
                    .as("Should extract Map type")
                    .isEqualTo("java.util.Map");
        }

        @Test
        @DisplayName("should extract Optional<Set<Money>> property")
        void shouldExtractOptionalWithNestedCollection() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    import java.math.BigDecimal;

                    @ValueObject
                    public record Money(BigDecimal amount, String currency) {}
                    """);
            writeSource("com/example/Wallet.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    import java.util.Optional;
                    import java.util.Set;

                    @AggregateRoot
                    public class Wallet {
                        private String id;
                        private Optional<Set<Money>> currencies;
                    }
                    """);

            DomainType wallet = classifyAndExport("com.example.Wallet");

            DomainProperty currencies = findProperty(wallet, "currencies");
            assertThat(currencies.cardinality())
                    .as("Optional property should have OPTIONAL cardinality")
                    .isEqualTo(Cardinality.OPTIONAL);
        }
    }

    // =========================================================================
    // Array Types
    // =========================================================================

    @Nested
    @DisplayName("Array Types")
    class ArrayTypesTests {

        @Test
        @DisplayName("should extract String[] property with COLLECTION cardinality")
        void shouldExtractStringArrayProperty() throws IOException {
            writeSource("com/example/Document.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Document {
                        private String id;
                        private String[] keywords;
                    }
                    """);

            DomainType document = classifyAndExport("com.example.Document");

            DomainProperty keywords = findProperty(document, "keywords");
            assertThat(keywords.cardinality())
                    .as("Array property should have COLLECTION cardinality")
                    .isEqualTo(Cardinality.COLLECTION);
        }

        @Test
        @DisplayName("should extract byte[] property")
        void shouldExtractByteArrayProperty() throws IOException {
            writeSource("com/example/Attachment.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Attachment {
                        private String id;
                        private byte[] content;
                    }
                    """);

            DomainType attachment = classifyAndExport("com.example.Attachment");

            DomainProperty content = findProperty(attachment, "content");
            assertThat(content.cardinality()).isEqualTo(Cardinality.COLLECTION);
            // byte[] is represented as "byte" with COLLECTION cardinality
            assertThat(content.type().qualifiedName()).isIn("byte[]", "byte");
        }

        @Test
        @DisplayName("should extract int[][] multi-dimensional array")
        void shouldExtractMultiDimensionalArray() throws IOException {
            writeSource("com/example/Matrix.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Matrix {
                        private String id;
                        private int[][] data;
                    }
                    """);

            DomainType matrix = classifyAndExport("com.example.Matrix");

            DomainProperty data = findProperty(matrix, "data");
            assertThat(data.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }
    }

    // =========================================================================
    // Primitive Wrapper Types
    // =========================================================================

    @Nested
    @DisplayName("Primitive Wrapper Types")
    class PrimitiveWrapperTypesTests {

        @Test
        @DisplayName("should extract Integer wrapper type")
        void shouldExtractIntegerProperty() throws IOException {
            writeSource("com/example/Product.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Product {
                        private Long id;
                        private Integer quantity;
                        private Boolean active;
                    }
                    """);

            DomainType product = classifyAndExport("com.example.Product");

            DomainProperty quantity = findProperty(product, "quantity");
            assertThat(quantity.type().qualifiedName()).isEqualTo("java.lang.Integer");
            assertThat(quantity.cardinality()).isEqualTo(Cardinality.SINGLE);

            DomainProperty active = findProperty(product, "active");
            assertThat(active.type().qualifiedName()).isEqualTo("java.lang.Boolean");
        }

        @Test
        @DisplayName("should extract primitive types correctly")
        void shouldExtractPrimitiveTypes() throws IOException {
            writeSource("com/example/Counter.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Counter {
                        private long id;
                        private int count;
                        private boolean enabled;
                        private double rate;
                    }
                    """);

            DomainType counter = classifyAndExport("com.example.Counter");

            assertThat(findProperty(counter, "count").type().qualifiedName()).isEqualTo("int");
            assertThat(findProperty(counter, "enabled").type().qualifiedName()).isEqualTo("boolean");
            assertThat(findProperty(counter, "rate").type().qualifiedName()).isEqualTo("double");
        }
    }

    // =========================================================================
    // Temporal Types
    // =========================================================================

    @Nested
    @DisplayName("Temporal Types")
    class TemporalTypesTests {

        @Test
        @DisplayName("should extract java.time types")
        void shouldExtractJavaTimeTypes() throws IOException {
            writeSource("com/example/Event.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;
                    import java.time.*;

                    @AggregateRoot
                    public class Event {
                        private String id;
                        private LocalDate eventDate;
                        private LocalDateTime createdAt;
                        private Instant timestamp;
                        private ZonedDateTime scheduledAt;
                        private Duration duration;
                    }
                    """);

            DomainType event = classifyAndExport("com.example.Event");

            assertThat(findProperty(event, "eventDate").type().qualifiedName()).isEqualTo("java.time.LocalDate");
            assertThat(findProperty(event, "createdAt").type().qualifiedName()).isEqualTo("java.time.LocalDateTime");
            assertThat(findProperty(event, "timestamp").type().qualifiedName()).isEqualTo("java.time.Instant");
            assertThat(findProperty(event, "scheduledAt").type().qualifiedName())
                    .isEqualTo("java.time.ZonedDateTime");
            assertThat(findProperty(event, "duration").type().qualifiedName()).isEqualTo("java.time.Duration");
        }
    }

    // =========================================================================
    // Special Field Modifiers
    // =========================================================================

    @Nested
    @DisplayName("Special Field Modifiers")
    class SpecialFieldModifiersTests {

        @Test
        @DisplayName("should NOT include static fields as properties")
        void shouldNotIncludeStaticFields() throws IOException {
            writeSource("com/example/Order.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Order {
                        private static final String TABLE_NAME = "orders";
                        private static int instanceCount = 0;
                        private String id;
                        private String description;
                    }
                    """);

            DomainType order = classifyAndExport("com.example.Order");

            // Static fields should NOT be included
            assertThat(order.properties())
                    .as("Static fields should not be exported as properties")
                    .extracting(DomainProperty::name)
                    .doesNotContain("TABLE_NAME", "instanceCount");

            // Instance fields should be included
            assertThat(order.properties()).extracting(DomainProperty::name).contains("id", "description");
        }

        @Test
        @DisplayName("should NOT include transient fields as properties")
        void shouldNotIncludeTransientFields() throws IOException {
            writeSource("com/example/Customer.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Customer {
                        private String id;
                        private String name;
                        private transient String cachedDisplayName;
                        private transient int hashCodeCache;
                    }
                    """);

            DomainType customer = classifyAndExport("com.example.Customer");

            // Transient fields should NOT be included
            assertThat(customer.properties())
                    .as("Transient fields should not be exported as properties")
                    .extracting(DomainProperty::name)
                    .doesNotContain("cachedDisplayName", "hashCodeCache");

            // Non-transient fields should be included
            assertThat(customer.properties()).extracting(DomainProperty::name).contains("id", "name");
        }

        @Test
        @DisplayName("should NOT include static transient fields")
        void shouldNotIncludeStaticTransientFields() throws IOException {
            writeSource("com/example/Product.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.AggregateRoot;

                    @AggregateRoot
                    public class Product {
                        private static transient Object lock = new Object();
                        private String id;
                        private String sku;
                    }
                    """);

            DomainType product = classifyAndExport("com.example.Product");

            assertThat(product.properties())
                    .as("Static transient fields should not be exported")
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("id", "sku");
        }

        @Test
        @DisplayName("should include final instance fields (immutable properties)")
        void shouldIncludeFinalInstanceFields() throws IOException {
            writeSource("com/example/Money.java", """
                    package com.example;
                    import org.jmolecules.ddd.annotation.ValueObject;
                    import java.math.BigDecimal;

                    @ValueObject
                    public final class Money {
                        private final BigDecimal amount;
                        private final String currency;

                        public Money(BigDecimal amount, String currency) {
                            this.amount = amount;
                            this.currency = currency;
                        }
                    }
                    """);

            DomainType money = classifyAndExport("com.example.Money");

            // Final instance fields should be included
            assertThat(money.properties())
                    .as("Final instance fields should be exported as properties")
                    .extracting(DomainProperty::name)
                    .containsExactlyInAnyOrder("amount", "currency");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void writeSource(String path, String content) throws IOException {
        Path file = tempDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private ApplicationGraph buildGraph() {
        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");
        JavaSemanticModel model = frontend.build(input);
        GraphMetadata metadata =
                GraphMetadata.of("com.example", 17, (int) model.types().size());
        model = frontend.build(input);
        return builder.build(model, metadata);
    }

    private DomainType classifyAndExport(String fqn) {
        ApplicationGraph graph = buildGraph();
        GraphQuery query = graph.query();

        List<ClassificationResult> classifications = new ArrayList<>();
        for (TypeNode type : graph.typeNodes()) {
            ClassificationResult domainResult = domainClassifier.classify(type, query);
            if (domainResult.isClassified()) {
                classifications.add(domainResult);
                continue;
            }

            ClassificationResult portResult = portClassifier.classify(type, query);
            if (portResult.isClassified()) {
                classifications.add(portResult);
            }
        }

        IrSnapshot snapshot = exporter.export(graph, classifications);

        return snapshot.domain().types().stream()
                .filter(t -> t.qualifiedName().equals(fqn))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DomainType '" + fqn + "' not found"));
    }

    private DomainProperty findProperty(DomainType type, String name) {
        return type.properties().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Property '" + name + "' not found in " + type.simpleName()));
    }
}
