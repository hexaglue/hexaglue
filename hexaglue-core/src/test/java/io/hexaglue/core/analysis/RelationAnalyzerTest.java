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

class RelationAnalyzerTest {

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

    @Nested
    @DisplayName("ONE_TO_MANY Relations")
    class OneToManyRelationsTest {

        @Test
        @DisplayName("should detect ONE_TO_MANY relation for collection of entities")
        void shouldDetectOneToManyRelation() throws IOException {
            writeSource(
                    "com/example/LineItem.java",
                    """
                    package com.example;
                    public class LineItem {
                        private String id;
                        private String productName;
                    }
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    import java.util.List;
                    public class Order {
                        private String id;
                        private List<LineItem> items;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.LineItem", "ENTITY"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation itemsRelation = relations.get(0);
            assertThat(itemsRelation.propertyName()).isEqualTo("items");
            assertThat(itemsRelation.kind()).isEqualTo(RelationKind.ONE_TO_MANY);
            assertThat(itemsRelation.targetTypeFqn()).isEqualTo("com.example.LineItem");
        }

        @Test
        @DisplayName("should detect mappedBy for bidirectional relation")
        void shouldDetectMappedByForBidirectionalRelation() throws IOException {
            writeSource(
                    "com/example/LineItem.java",
                    """
                    package com.example;
                    public class LineItem {
                        private String id;
                        private Order order;
                    }
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    import java.util.List;
                    public class Order {
                        private String id;
                        private List<LineItem> items;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.LineItem", "ENTITY"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation itemsRelation = relations.get(0);
            assertThat(itemsRelation.mappedBy()).isEqualTo("order");
            assertThat(itemsRelation.isBidirectional()).isTrue();
        }
    }

    @Nested
    @DisplayName("EMBEDDED Relations")
    class EmbeddedRelationsTest {

        @Test
        @DisplayName("should detect EMBEDDED relation for value object field")
        void shouldDetectEmbeddedRelation() throws IOException {
            writeSource(
                    "com/example/Money.java",
                    """
                    package com.example;
                    import java.math.BigDecimal;
                    public record Money(BigDecimal amount, String currency) {}
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                        private Money total;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.Money", "VALUE_OBJECT"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation totalRelation = relations.get(0);
            assertThat(totalRelation.propertyName()).isEqualTo("total");
            assertThat(totalRelation.kind()).isEqualTo(RelationKind.EMBEDDED);
            assertThat(totalRelation.targetTypeFqn()).isEqualTo("com.example.Money");
        }

        @Test
        @DisplayName("should detect ELEMENT_COLLECTION for collection of value objects")
        void shouldDetectElementCollectionRelation() throws IOException {
            writeSource(
                    "com/example/Tag.java",
                    """
                    package com.example;
                    public record Tag(String name) {}
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    import java.util.Set;
                    public class Order {
                        private String id;
                        private Set<Tag> tags;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(
                    graph,
                    Map.of(
                            "com.example.Order", "AGGREGATE_ROOT",
                            "com.example.Tag", "VALUE_OBJECT"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).hasSize(1);
            DomainRelation tagsRelation = relations.get(0);
            assertThat(tagsRelation.propertyName()).isEqualTo("tags");
            assertThat(tagsRelation.kind()).isEqualTo(RelationKind.ELEMENT_COLLECTION);
            assertThat(tagsRelation.targetTypeFqn()).isEqualTo("com.example.Tag");
        }
    }

    @Nested
    @DisplayName("MANY_TO_ONE Relations")
    class ManyToOneRelationsTest {

        @Test
        @DisplayName("should detect MANY_TO_ONE relation to another aggregate")
        void shouldDetectManyToOneToAggregate() throws IOException {
            writeSource(
                    "com/example/Customer.java",
                    """
                    package com.example;
                    public class Customer {
                        private String id;
                        private String name;
                    }
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
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
            assertThat(customerRelation.propertyName()).isEqualTo("customer");
            assertThat(customerRelation.kind()).isEqualTo(RelationKind.MANY_TO_ONE);
            assertThat(customerRelation.targetTypeFqn()).isEqualTo("com.example.Customer");
        }
    }

    @Nested
    @DisplayName("Skip Non-Relations")
    class SkipNonRelationsTest {

        @Test
        @DisplayName("should skip identity fields")
        void shouldSkipIdentityFields() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(graph, Map.of("com.example.Order", "AGGREGATE_ROOT"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).isEmpty();
        }

        @Test
        @DisplayName("should skip primitive type fields")
        void shouldSkipPrimitiveFields() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                        private int quantity;
                        private String name;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationContext context = buildContext(graph, Map.of("com.example.Order", "AGGREGATE_ROOT"));

            TypeNode orderType = graph.typeNode("com.example.Order").orElseThrow();
            List<DomainRelation> relations = analyzer.analyzeRelations(orderType, graph.query(), context);

            assertThat(relations).isEmpty();
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
