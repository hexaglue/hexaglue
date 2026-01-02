package io.hexaglue.core.classification;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.spoon.SpoonFrontend;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.builder.GraphBuilder;
import io.hexaglue.core.graph.model.GraphMetadata;
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
 * Tests for {@link TwoPassClassifier}.
 */
class TwoPassClassifierTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private TwoPassClassifier classifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        classifier = new TwoPassClassifier();
    }

    @Nested
    @DisplayName("Two-Pass Classification")
    class TwoPassClassificationTest {

        @Test
        @DisplayName("should classify domain types in pass 1 and ports in pass 2")
        void shouldClassifyInTwoPasses() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                        private String customerName;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                        void save(Order order);
                    }
                    """);

            ApplicationGraph graph = buildGraph();

            ClassificationResults results = classifier.classify(graph);

            // Verify domain classifications
            assertThat(results.domainClassifications()).hasSize(1);
            ClassificationResult orderResult = results.domainClassifications().get(0);
            assertThat(orderResult.kind()).isEqualTo("AGGREGATE_ROOT");

            // Verify port classifications
            assertThat(results.portClassifications()).hasSize(1);
            ClassificationResult repoResult = results.portClassifications().get(0);
            assertThat(repoResult.kind()).isEqualTo("REPOSITORY");
        }

        @Test
        @DisplayName("should separate domain and port classifications")
        void shouldSeparateDomainAndPortClassifications() throws IOException {
            writeSource(
                    "com/example/Money.java",
                    """
                    package com.example;
                    public record Money(java.math.BigDecimal amount, String currency) {}
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
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);
            writeSource(
                    "com/example/CreateOrderUseCase.java",
                    """
                    package com.example;
                    public interface CreateOrderUseCase {
                        void execute(String customerId);
                    }
                    """);

            ApplicationGraph graph = buildGraph();

            ClassificationResults results = classifier.classify(graph);

            // Domain types: Order (AGGREGATE_ROOT), Money (VALUE_OBJECT)
            assertThat(results.domainClassifications())
                    .extracting(ClassificationResult::kind)
                    .containsExactlyInAnyOrder("AGGREGATE_ROOT", "VALUE_OBJECT");

            // Ports: OrderRepository (REPOSITORY), CreateOrderUseCase (USE_CASE)
            assertThat(results.portClassifications())
                    .extracting(ClassificationResult::kind)
                    .containsExactlyInAnyOrder("REPOSITORY", "USE_CASE");
        }
    }

    @Nested
    @DisplayName("Classification Context")
    class ClassificationContextTest {

        @Test
        @DisplayName("context should correctly identify aggregate roots")
        void contextShouldIdentifyAggregateRoots() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationResults results = classifier.classify(graph);

            // Build context from domain classifications
            ClassificationContext context = new ClassificationContext(results.domainClassifications().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            c -> io.hexaglue.core.graph.model.NodeId.type(getQualifiedName(c, graph)), c -> c)));

            // Verify context methods
            var orderId = io.hexaglue.core.graph.model.NodeId.type("com.example.Order");
            assertThat(context.isAggregate(orderId)).isTrue();
            assertThat(context.isEntity(orderId)).isTrue();
            assertThat(context.isValueObject(orderId)).isFalse();
            assertThat(context.getKind(orderId)).isEqualTo("AGGREGATE_ROOT");
        }

        private String getQualifiedName(ClassificationResult result, ApplicationGraph graph) {
            return graph.typeNode(result.subjectId())
                    .map(t -> t.qualifiedName())
                    .orElse("unknown");
        }
    }

    @Nested
    @DisplayName("Classification Results")
    class ClassificationResultsTest {

        @Test
        @DisplayName("should provide classified results list")
        void shouldProvideClassifiedResults() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(String id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationResults results = classifier.classify(graph);

            assertThat(results.classifiedResults()).hasSize(2);
            assertThat(results.size()).isGreaterThanOrEqualTo(2);
            assertThat(results.conflicts()).isEmpty();
        }

        @Test
        @DisplayName("should convert to list")
        void shouldConvertToList() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            ClassificationResults results = classifier.classify(graph);

            List<ClassificationResult> list = results.toList();
            assertThat(list).isNotEmpty();
            assertThat(results.stream().count()).isEqualTo(list.size());
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
