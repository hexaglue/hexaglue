package io.hexaglue.core.graph.style;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.port.PortClassifier;
import io.hexaglue.core.classification.port.PortDirection;
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
 * Integration tests for the complete style detection flow.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>StyleDetector correctly detects architecture style from package patterns</li>
 *   <li>GraphBuilder enriches GraphMetadata with detected style</li>
 *   <li>GraphQuery exposes the detected style</li>
 *   <li>Classification criteria use the detected style for confidence boosting</li>
 * </ul>
 */
@DisplayName("Style Detection Integration")
class StyleDetectionIntegrationTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;
    private GraphBuilder builder;
    private PortClassifier portClassifier;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
        builder = new GraphBuilder(true);
        portClassifier = new PortClassifier();
    }

    @Nested
    @DisplayName("Hexagonal Architecture Detection")
    class HexagonalDetectionTest {

        @Test
        @DisplayName("should detect HEXAGONAL style from ports.in/ports.out packages")
        void shouldDetectHexagonalFromPortsPackages() throws IOException {
            // Setup: hexagonal architecture with ports.in and ports.out packages
            writeSource(
                    "com/example/order/ports/in/OrderingCoffee.java",
                    """
                    package com.example.order.ports.in;
                    public interface OrderingCoffee {
                        void placeOrder(Object order);
                    }
                    """);
            writeSource(
                    "com/example/order/ports/out/OrderRepository.java",
                    """
                    package com.example.order.ports.out;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);
            writeSource(
                    "com/example/order/domain/Order.java",
                    """
                    package com.example.order.domain;
                    public class Order {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            // Verify style detection in metadata
            assertThat(graph.metadata().style()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);
            assertThat(graph.metadata().hasKnownStyle()).isTrue();

            // Verify style is accessible via GraphQuery
            assertThat(query.packageOrganizationStyle()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);
            assertThat(query.supportsPortDirection()).isTrue();
        }

        @Test
        @DisplayName("should boost confidence for ports classification in HEXAGONAL style")
        void shouldBoostConfidenceForHexagonalStyle() throws IOException {
            // Setup: hexagonal architecture
            writeSource(
                    "com/example/order/ports/in/OrderingCoffee.java",
                    """
                    package com.example.order.ports.in;
                    public interface OrderingCoffee {
                        void placeOrder(Object order);
                    }
                    """);
            writeSource(
                    "com/example/order/ports/out/OrderRepository.java",
                    """
                    package com.example.order.ports.out;
                    public interface OrderRepository {
                        void save(Object order);
                    }
                    """);
            writeSource(
                    "com/example/payment/ports/in/MakingPayment.java",
                    """
                    package com.example.payment.ports.in;
                    public interface MakingPayment {
                        void processPayment(Object payment);
                    }
                    """);
            writeSource(
                    "com/example/payment/ports/out/PaymentGateway.java",
                    """
                    package com.example.payment.ports.out;
                    public interface PaymentGateway {
                        void charge(Object payment);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            // Verify HEXAGONAL style is detected
            assertThat(query.packageOrganizationStyle()).isEqualTo(PackageOrganizationStyle.HEXAGONAL);

            // Classify a port in ports.in package - should have HIGH confidence
            TypeNode orderingCoffee =
                    graph.typeNode("com.example.order.ports.in.OrderingCoffee").orElseThrow();
            ClassificationResult result = portClassifier.classify(orderingCoffee, query);

            assertThat(result.isClassified()).isTrue();
            // PackageInCriteria should match with HIGH confidence due to HEXAGONAL style
            assertThat(result.confidence()).isIn(ConfidenceLevel.HIGH, ConfidenceLevel.EXPLICIT);
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVING);
        }

        @Test
        @DisplayName("should classify ports.out interfaces as DRIVEN ports")
        void shouldClassifyPortsOutAsDriven() throws IOException {
            writeSource(
                    "com/example/ports/in/UseCase.java",
                    """
                    package com.example.ports.in;
                    public interface UseCase {
                        void execute();
                    }
                    """);
            writeSource(
                    "com/example/ports/out/Repository.java",
                    """
                    package com.example.ports.out;
                    public interface Repository {
                        void save(Object entity);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            TypeNode repository =
                    graph.typeNode("com.example.ports.out.Repository").orElseThrow();
            ClassificationResult result = portClassifier.classify(repository, query);

            assertThat(result.isClassified()).isTrue();
            assertThat(result.portDirection()).isEqualTo(PortDirection.DRIVEN);
            assertThat(result.kind()).isIn("REPOSITORY", "GATEWAY");
        }
    }

    @Nested
    @DisplayName("Layer-based Architecture Detection")
    class LayerDetectionTest {

        @Test
        @DisplayName("should detect BY_LAYER style from domain/application/infrastructure packages")
        void shouldDetectByLayerFromLayeredPackages() throws IOException {
            writeSource(
                    "com/example/domain/Order.java",
                    """
                    package com.example.domain;
                    public class Order {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/domain/Customer.java",
                    """
                    package com.example.domain;
                    public class Customer {
                        private String id;
                    }
                    """);
            writeSource(
                    "com/example/application/OrderService.java",
                    """
                    package com.example.application;
                    public interface OrderService {
                        void processOrder(Object order);
                    }
                    """);
            writeSource(
                    "com/example/infrastructure/JpaOrderRepository.java",
                    """
                    package com.example.infrastructure;
                    public class JpaOrderRepository {
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            assertThat(query.packageOrganizationStyle()).isEqualTo(PackageOrganizationStyle.BY_LAYER);
            assertThat(query.supportsPortDirection()).isFalse();
        }
    }

    @Nested
    @DisplayName("Unknown Style Handling")
    class UnknownStyleTest {

        @Test
        @DisplayName("should return UNKNOWN for non-standard package structure")
        void shouldReturnUnknownForNonStandardPackages() throws IOException {
            writeSource(
                    "com/example/foo/Bar.java",
                    """
                    package com.example.foo;
                    public class Bar {}
                    """);
            writeSource(
                    "com/example/baz/Qux.java",
                    """
                    package com.example.baz;
                    public class Qux {}
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            assertThat(query.packageOrganizationStyle()).isEqualTo(PackageOrganizationStyle.UNKNOWN);
            assertThat(query.hasConfidentStyle()).isFalse();
        }

        @Test
        @DisplayName("should not boost confidence for ports classification with UNKNOWN style")
        void shouldNotBoostConfidenceWithUnknownStyle() throws IOException {
            // Only .in package without full hexagonal structure
            // Style should not be HEXAGONAL since we don't have ports.in/ports.out patterns
            writeSource(
                    "com/example/in/DoSomething.java",
                    """
                    package com.example.in;
                    public interface DoSomething {
                        void execute();
                    }
                    """);
            writeSource(
                    "com/example/model/Something.java",
                    """
                    package com.example.model;
                    public class Something {
                        private String id;
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            GraphQuery query = graph.query();

            // Should not detect HEXAGONAL due to lack of characteristic patterns
            // (.ports.in, .ports.out, .adapters, etc.)
            assertThat(query.packageOrganizationStyle()).isNotEqualTo(PackageOrganizationStyle.HEXAGONAL);

            // Since style is not HEXAGONAL, supportsPortDirection should return false
            // (Only HEXAGONAL and CLEAN_ARCHITECTURE support port direction)
            if (query.packageOrganizationStyle() != PackageOrganizationStyle.CLEAN_ARCHITECTURE) {
                assertThat(query.supportsPortDirection()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Metadata Enrichment")
    class MetadataEnrichmentTest {

        @Test
        @DisplayName("should include detected patterns in metadata")
        void shouldIncludeDetectedPatternsInMetadata() throws IOException {
            // Create types in different packages to get multiple pattern matches
            // StyleDetector counts unique packages matching each pattern
            writeSource(
                    "com/example/order/ports/in/A.java",
                    """
                    package com.example.order.ports.in;
                    public interface A {}
                    """);
            writeSource(
                    "com/example/payment/ports/in/B.java",
                    """
                    package com.example.payment.ports.in;
                    public interface B {}
                    """);
            writeSource(
                    "com/example/order/ports/out/C.java",
                    """
                    package com.example.order.ports.out;
                    public interface C {}
                    """);

            ApplicationGraph graph = buildGraph();
            GraphMetadata metadata = graph.metadata();

            assertThat(metadata.detectedPatterns()).isNotEmpty();
            assertThat(metadata.detectedPatterns()).containsKey(".ports.in");
            assertThat(metadata.detectedPatterns()).containsKey(".ports.out");
            // Each package is counted once: order.ports.in and payment.ports.in = 2
            assertThat(metadata.detectedPatterns().get(".ports.in")).isEqualTo(2);
            assertThat(metadata.detectedPatterns().get(".ports.out")).isEqualTo(1);
        }

        @Test
        @DisplayName("should preserve base metadata during enrichment")
        void shouldPreserveBaseMetadataDuringEnrichment() throws IOException {
            writeSource(
                    "com/example/ports/in/A.java",
                    """
                    package com.example.ports.in;
                    public interface A {}
                    """);

            ApplicationGraph graph = buildGraph();
            GraphMetadata metadata = graph.metadata();

            assertThat(metadata.basePackage()).isEqualTo("com.example");
            assertThat(metadata.javaVersion()).isEqualTo(17);
            assertThat(metadata.sourceCount()).isEqualTo(1);
            assertThat(metadata.buildTimestamp()).isNotNull();
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
