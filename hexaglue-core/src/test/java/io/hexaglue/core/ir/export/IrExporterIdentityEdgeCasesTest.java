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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Edge case tests for identity detection and extraction in IrExporter.
 *
 * <p>These tests cover scenarios not present in the example applications,
 * ensuring robustness for:
 * <ul>
 *   <li>Composite identifiers (multi-property records)</li>
 *   <li>Inter-aggregate references (should NOT be marked as identity)</li>
 *   <li>Non-primitive wrapped identities</li>
 *   <li>Multiple ID-like fields in same type</li>
 * </ul>
 */
@DisplayName("IrExporter - Identity Edge Cases")
class IrExporterIdentityEdgeCasesTest {

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
    // Composite Identity Tests (Multi-property IDs)
    // =========================================================================

    @Nested
    @DisplayName("Composite Identifiers")
    class CompositeIdentifierTests {

        @Test
        @DisplayName("should NOT unwrap composite identifier with 2 properties")
        void shouldNotUnwrapCompositeIdentifierWithTwoProperties() throws IOException {
            writeSource(
                    "com/example/CompositeOrderId.java",
                    """
                    package com.example;
                    public record CompositeOrderId(String region, Long sequence) {}
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private CompositeOrderId id;
                        private String description;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(CompositeOrderId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = findDomainType(snapshot, "Order");
            assertThat(order.hasIdentity()).isTrue();

            Identity identity = order.identity().orElseThrow();
            assertThat(identity.fieldName()).isEqualTo("id");
            // Composite ID should NOT be unwrapped - type stays as CompositeOrderId
            assertThat(identity.type().qualifiedName()).isEqualTo("com.example.CompositeOrderId");
            assertThat(identity.unwrappedType().qualifiedName())
                    .as("Composite ID should NOT be unwrapped to a primitive type")
                    .isEqualTo("com.example.CompositeOrderId");
            assertThat(identity.wrapperKind())
                    .as("Composite ID should have NONE wrapper kind (not unwrappable)")
                    .isEqualTo(IdentityWrapperKind.NONE);
        }

        @Test
        @DisplayName("should NOT unwrap composite identifier with 3+ properties")
        void shouldNotUnwrapCompositeIdentifierWithThreeProperties() throws IOException {
            writeSource(
                    "com/example/TenantScopedId.java",
                    """
                    package com.example;
                    public record TenantScopedId(String tenantId, String region, Long sequence) {}
                    """);
            writeSource(
                    "com/example/TenantResource.java",
                    """
                    package com.example;
                    public class TenantResource {
                        private TenantScopedId id;
                        private String name;
                    }
                    """);
            writeSource(
                    "com/example/TenantResourceRepository.java",
                    """
                    package com.example;
                    public interface TenantResourceRepository {
                        TenantResource findById(TenantScopedId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType resource = findDomainType(snapshot, "TenantResource");
            Identity identity = resource.identity().orElseThrow();

            assertThat(identity.unwrappedType().qualifiedName())
                    .as("3-property composite ID should NOT be unwrapped")
                    .isEqualTo("com.example.TenantScopedId");
            assertThat(identity.isWrapped()).isFalse();
        }

        @Test
        @Disabled("TODO: Implement - Composite ID as @EmbeddedId strategy")
        @DisplayName("should mark composite identifier strategy as COMPOSITE")
        void shouldMarkCompositeIdentifierAsCompositeStrategy() throws IOException {
            // This test documents that composite IDs should have a COMPOSITE strategy
            // Currently IdentityStrategy doesn't have COMPOSITE - may need to add it
        }
    }

    // =========================================================================
    // Inter-Aggregate Reference Tests
    // =========================================================================

    @Nested
    @DisplayName("Inter-Aggregate References")
    class InterAggregateReferenceTests {

        @Test
        @DisplayName("should NOT mark customerId as identity field")
        void shouldNotMarkCustomerIdAsIdentity() throws IOException {
            writeSource(
                    "com/example/CustomerId.java",
                    """
                    package com.example;
                    public record CustomerId(java.util.UUID value) {}
                    """);
            writeSource(
                    "com/example/OrderId.java",
                    """
                    package com.example;
                    public record OrderId(java.util.UUID value) {}
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private OrderId id;
                        private CustomerId customerId;
                        private String description;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(OrderId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = findDomainType(snapshot, "Order");

            // Verify identity is 'id', not 'customerId'
            Identity identity = order.identity().orElseThrow();
            assertThat(identity.fieldName())
                    .as("Identity should be 'id', not 'customerId'")
                    .isEqualTo("id");

            // Verify customerId is a regular property, NOT identity
            DomainProperty customerIdProp = order.properties().stream()
                    .filter(p -> p.name().equals("customerId"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("customerId property should exist"));

            assertThat(customerIdProp.isIdentity())
                    .as("customerId should NOT be marked as identity")
                    .isFalse();
        }

        @Test
        @DisplayName("should handle multiple *Id fields correctly")
        void shouldHandleMultipleIdFieldsCorrectly() throws IOException {
            writeSource(
                    "com/example/OrderId.java",
                    """
                    package com.example;
                    public record OrderId(java.util.UUID value) {}
                    """);
            writeSource(
                    "com/example/CustomerId.java",
                    """
                    package com.example;
                    public record CustomerId(java.util.UUID value) {}
                    """);
            writeSource(
                    "com/example/ProductId.java",
                    """
                    package com.example;
                    public record ProductId(java.util.UUID value) {}
                    """);
            writeSource(
                    "com/example/OrderLine.java",
                    """
                    package com.example;
                    public class OrderLine {
                        private Long id;
                        private OrderId orderId;
                        private ProductId productId;
                        private int quantity;
                    }
                    """);
            writeSource(
                    "com/example/OrderLineRepository.java",
                    """
                    package com.example;
                    public interface OrderLineRepository {
                        OrderLine findById(Long id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType orderLine = findDomainType(snapshot, "OrderLine");

            // Only 'id' should be marked as identity
            Identity identity = orderLine.identity().orElseThrow();
            assertThat(identity.fieldName()).isEqualTo("id");

            // orderId and productId should NOT be marked as identity
            for (String propName : List.of("orderId", "productId")) {
                DomainProperty prop = orderLine.properties().stream()
                        .filter(p -> p.name().equals(propName))
                        .findFirst()
                        .orElseThrow();
                assertThat(prop.isIdentity())
                        .as(propName + " should NOT be marked as identity")
                        .isFalse();
            }
        }

        @Test
        @DisplayName("should prefer 'id' field over other *Id fields")
        void shouldPreferIdFieldOverOtherIdFields() throws IOException {
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private java.util.UUID transactionId;
                        private String id;
                        private java.util.UUID correlationId;
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
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = findDomainType(snapshot, "Order");
            Identity identity = order.identity().orElseThrow();

            assertThat(identity.fieldName())
                    .as("Should prefer field named exactly 'id'")
                    .isEqualTo("id");
        }
    }

    // =========================================================================
    // Non-Primitive Wrapped Identity Tests
    // =========================================================================

    @Nested
    @DisplayName("Non-Primitive Wrapped Identities")
    class NonPrimitiveWrappedIdentityTests {

        @Test
        @DisplayName("should NOT unwrap identity wrapping a non-primitive type")
        void shouldNotUnwrapNonPrimitiveWrappedIdentity() throws IOException {
            // Example: record OrderId(Money value) - Money is a value object, not primitive
            // This should NOT be unwrapped because Money can't be directly persisted
            writeSource(
                    "com/example/Money.java",
                    """
                    package com.example;
                    public record Money(java.math.BigDecimal amount, String currency) {}
                    """);
            writeSource(
                    "com/example/OrderId.java",
                    """
                    package com.example;
                    public record OrderId(Money value) {}
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private OrderId id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(OrderId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = findDomainType(snapshot, "Order");
            Identity identity = order.identity().orElseThrow();

            // Should NOT unwrap to Money - Money is not a valid JPA ID type
            assertThat(identity.unwrappedType().qualifiedName())
                    .as("Should not unwrap to non-primitive type")
                    .isEqualTo("com.example.OrderId");
        }

        @Test
        @DisplayName("should unwrap identity wrapping UUID")
        void shouldUnwrapIdentityWrappingUuid() throws IOException {
            writeSource(
                    "com/example/OrderId.java",
                    """
                    package com.example;
                    public record OrderId(java.util.UUID value) {}
                    """);
            writeSource(
                    "com/example/Order.java",
                    """
                    package com.example;
                    public class Order {
                        private OrderId id;
                    }
                    """);
            writeSource(
                    "com/example/OrderRepository.java",
                    """
                    package com.example;
                    public interface OrderRepository {
                        Order findById(OrderId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType order = findDomainType(snapshot, "Order");
            Identity identity = order.identity().orElseThrow();

            assertThat(identity.type().qualifiedName()).isEqualTo("com.example.OrderId");
            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(identity.isWrapped()).isTrue();
            assertThat(identity.wrapperKind()).isEqualTo(IdentityWrapperKind.RECORD);
        }

        @Test
        @DisplayName("should unwrap identity wrapping Long")
        void shouldUnwrapIdentityWrappingLong() throws IOException {
            writeSource(
                    "com/example/ProductId.java",
                    """
                    package com.example;
                    public record ProductId(Long value) {}
                    """);
            writeSource(
                    "com/example/Product.java",
                    """
                    package com.example;
                    public class Product {
                        private ProductId id;
                        private String name;
                    }
                    """);
            writeSource(
                    "com/example/ProductRepository.java",
                    """
                    package com.example;
                    public interface ProductRepository {
                        Product findById(ProductId id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType product = findDomainType(snapshot, "Product");
            Identity identity = product.identity().orElseThrow();

            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.lang.Long");
            assertThat(identity.strategy()).isEqualTo(IdentityStrategy.AUTO);
        }

        @Test
        @DisplayName("should unwrap identity wrapping String")
        void shouldUnwrapIdentityWrappingString() throws IOException {
            writeSource(
                    "com/example/Sku.java",
                    """
                    package com.example;
                    public record Sku(String value) {}
                    """);
            writeSource(
                    "com/example/Product.java",
                    """
                    package com.example;
                    public class Product {
                        private Sku id;
                        private String name;
                    }
                    """);
            writeSource(
                    "com/example/ProductRepository.java",
                    """
                    package com.example;
                    public interface ProductRepository {
                        Product findById(Sku id);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType product = findDomainType(snapshot, "Product");
            Identity identity = product.identity().orElseThrow();

            assertThat(identity.unwrappedType().qualifiedName()).isEqualTo("java.lang.String");
            assertThat(identity.strategy()).isEqualTo(IdentityStrategy.ASSIGNED);
        }
    }

    // =========================================================================
    // Explicit @Identity Annotation Tests
    // =========================================================================

    @Nested
    @DisplayName("Explicit @Identity Annotation")
    class ExplicitIdentityAnnotationTests {

        @Test
        @DisplayName("should respect @Identity annotation on non-id field")
        void shouldRespectIdentityAnnotationOnNonIdField() throws IOException {
            writeSource(
                    "com/example/Transaction.java",
                    """
                    package com.example;
                    public class Transaction {
                        @org.jmolecules.ddd.annotation.Identity
                        private String transactionRef;
                        private String description;
                    }
                    """);
            writeSource(
                    "com/example/TransactionRepository.java",
                    """
                    package com.example;
                    public interface TransactionRepository {
                        Transaction findById(String transactionRef);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType transaction = findDomainType(snapshot, "Transaction");
            Identity identity = transaction.identity().orElseThrow();

            assertThat(identity.fieldName())
                    .as("Should use @Identity annotated field as identity")
                    .isEqualTo("transactionRef");
        }

        @Test
        @DisplayName("should prefer @Identity annotation over 'id' field")
        void shouldPreferIdentityAnnotationOverIdField() throws IOException {
            writeSource(
                    "com/example/Document.java",
                    """
                    package com.example;
                    public class Document {
                        private Long id;
                        @org.jmolecules.ddd.annotation.Identity
                        private String documentNumber;
                    }
                    """);
            writeSource(
                    "com/example/DocumentRepository.java",
                    """
                    package com.example;
                    public interface DocumentRepository {
                        Document findById(String documentNumber);
                    }
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType document = findDomainType(snapshot, "Document");
            Identity identity = document.identity().orElseThrow();

            assertThat(identity.fieldName())
                    .as("@Identity annotation should take precedence over 'id' field naming")
                    .isEqualTo("documentNumber");
        }
    }

    // =========================================================================
    // No Identity Tests
    // =========================================================================

    @Nested
    @DisplayName("Types Without Identity")
    class NoIdentityTests {

        @Test
        @DisplayName("VALUE_OBJECT should have no identity")
        void valueObjectShouldHaveNoIdentity() throws IOException {
            writeSource(
                    "com/example/Money.java",
                    """
                    package com.example;
                    public record Money(java.math.BigDecimal amount, String currency) {}
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            DomainType money = findDomainType(snapshot, "Money");
            assertThat(money.kind()).isEqualTo(DomainKind.VALUE_OBJECT);
            assertThat(money.hasIdentity()).isFalse();
        }

        @Test
        @DisplayName("DOMAIN_EVENT should have no identity")
        void domainEventShouldHaveNoIdentity() throws IOException {
            // Domain events typically don't have identity in the DDD sense
            writeSource(
                    "com/example/OrderPlacedEvent.java",
                    """
                    package com.example;
                    public record OrderPlacedEvent(
                        java.util.UUID eventId,
                        java.util.UUID orderId,
                        java.time.Instant occurredAt
                    ) {}
                    """);

            ApplicationGraph graph = buildGraph();
            List<ClassificationResult> classifications = classifyAll(graph);
            IrSnapshot snapshot = exporter.export(graph, classifications);

            Optional<DomainType> event = snapshot.domain().types().stream()
                    .filter(t -> t.simpleName().equals("OrderPlacedEvent"))
                    .findFirst();

            if (event.isPresent()) {
                assertThat(event.get().kind()).isEqualTo(DomainKind.DOMAIN_EVENT);
                assertThat(event.get().hasIdentity()).isFalse();
            }
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

    private DomainType findDomainType(IrSnapshot snapshot, String simpleName) {
        return snapshot.domain().types().stream()
                .filter(t -> t.simpleName().equals(simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DomainType '" + simpleName + "' not found. " +
                        "Available types: " + snapshot.domain().types().stream()
                                .map(DomainType::simpleName)
                                .toList()));
    }
}
