package io.hexaglue.core.classification;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.core.classification.domain.DomainClassifier;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.core.graph.testing.TestGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Edge case tests for classification with inheritance hierarchies.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Abstract parent classes</li>
 *   <li>Concrete subclasses inheriting from domain types</li>
 *   <li>Interface inheritance hierarchies</li>
 *   <li>Classification of types with @MappedSuperclass</li>
 * </ul>
 */
@DisplayName("Classification - Inheritance Edge Cases")
class ClassificationInheritanceEdgeCasesTest {

    private static final String PKG = "com.example.domain";

    private DomainClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new DomainClassifier();
    }

    // =========================================================================
    // Abstract Classes
    // =========================================================================

    @Nested
    @DisplayName("Abstract Classes")
    class AbstractClassTests {

        @Test
        @DisplayName("should classify abstract entity with @Entity annotation")
        void shouldClassifyAbstractEntityWithAnnotation() {
            // abstract class BaseEntity { UUID id; } with @Entity
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".BaseEntity")
                    .asPublic()
                    .asAbstract()
                    .annotatedWith("org.jmolecules.ddd.annotation.Entity")
                    .withField("id", "java.util.UUID")
                    .build();

            GraphQuery query = graph.query();
            TypeNode baseEntity = query.type(PKG + ".BaseEntity").orElseThrow();

            ClassificationResult result = classifier.classify(baseEntity, query);

            assertThat(result.status())
                    .as("Abstract class with @Entity should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind())
                    .as("Should be classified as ENTITY")
                    .isEqualTo(DomainKind.ENTITY.name());
        }

        @Test
        @DisplayName("should classify abstract aggregate root with @AggregateRoot annotation")
        void shouldClassifyAbstractAggregateRoot() {
            // abstract class BaseAggregate { UUID id; } with @AggregateRoot
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".BaseAggregate")
                    .asPublic()
                    .asAbstract()
                    .annotatedWith("org.jmolecules.ddd.annotation.AggregateRoot")
                    .withField("id", "java.util.UUID")
                    .build();

            GraphQuery query = graph.query();
            TypeNode baseAggregate = query.type(PKG + ".BaseAggregate").orElseThrow();

            ClassificationResult result = classifier.classify(baseAggregate, query);

            assertThat(result.status())
                    .as("Abstract class with @AggregateRoot should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind())
                    .as("Should be classified as AGGREGATE_ROOT")
                    .isEqualTo(DomainKind.AGGREGATE_ROOT.name());
        }

        @Test
        @DisplayName("should NOT classify abstract class without identity or annotations")
        void shouldNotClassifyAbstractClassWithoutIdentity() {
            // abstract class AbstractService { void execute(); }
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".AbstractService")
                    .asPublic()
                    .asAbstract()
                    .withVoidMethod("execute")
                    .build();

            GraphQuery query = graph.query();
            TypeNode abstractService = query.type(PKG + ".AbstractService").orElseThrow();

            ClassificationResult result = classifier.classify(abstractService, query);

            // Should not classify - no annotations, no identity
            assertThat(result.status())
                    .as("Abstract class without annotations or identity should not be classified")
                    .isEqualTo(ClassificationStatus.UNCLASSIFIED);
        }
    }

    // =========================================================================
    // Subclass Inheritance
    // =========================================================================

    @Nested
    @DisplayName("Subclass Inheritance")
    class SubclassInheritanceTests {

        @Test
        @DisplayName("should classify subclass as AGGREGATE_ROOT when parent is AGGREGATE_ROOT")
        void shouldClassifySubclassAsAggregateRootWhenParentIsAggregateRoot() {
            // class SpecialOrder extends Order { }
            // where Order is classified as AGGREGATE_ROOT
            // In DDD, a subclass of an AggregateRoot is also an AggregateRoot (polymorphism)
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".Order")
                    .asPublic()
                    .annotatedWith("org.jmolecules.ddd.annotation.AggregateRoot")
                    .withField("id", "java.util.UUID")
                    .withClass(PKG + ".SpecialOrder")
                    .asPublic()
                    .extending(PKG + ".Order")
                    .build();

            GraphQuery query = graph.query();

            // First classify the parent
            TypeNode order = query.type(PKG + ".Order").orElseThrow();
            ClassificationResult parentResult = classifier.classify(order, query);
            assertThat(parentResult.status()).isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(parentResult.kind()).isEqualTo(DomainKind.AGGREGATE_ROOT.name());

            // Then classify the child - should inherit AGGREGATE_ROOT classification
            TypeNode specialOrder = query.type(PKG + ".SpecialOrder").orElseThrow();
            ClassificationResult childResult = classifier.classify(specialOrder, query);

            assertThat(childResult.status())
                    .as("Subclass should inherit parent's classification")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(childResult.kind())
                    .as("Subclass of AggregateRoot should also be AGGREGATE_ROOT")
                    .isEqualTo(DomainKind.AGGREGATE_ROOT.name());
        }

        @Test
        @DisplayName("should classify class implementing AggregateRoot interface")
        void shouldClassifyClassImplementingAggregateRootInterface() {
            // class Order implements AggregateRoot<Order, OrderId> { }
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".Order")
                    .asPublic()
                    .implementing("org.jmolecules.ddd.types.AggregateRoot")
                    .withField("id", "java.util.UUID")
                    .build();

            GraphQuery query = graph.query();
            TypeNode order = query.type(PKG + ".Order").orElseThrow();

            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.status())
                    .as("Class implementing AggregateRoot interface should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind())
                    .isEqualTo(DomainKind.AGGREGATE_ROOT.name());
        }
    }

    // =========================================================================
    // Value Object Inheritance
    // =========================================================================

    @Nested
    @DisplayName("Value Object Inheritance")
    class ValueObjectInheritanceTests {

        @Test
        @DisplayName("should classify record with @ValueObject annotation")
        void shouldClassifyRecordWithValueObjectAnnotation() {
            // record Money(BigDecimal amount, Currency currency) with @ValueObject
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withRecord(PKG + ".Money")
                    .annotatedWith("org.jmolecules.ddd.annotation.ValueObject")
                    .withFinalField("amount", "java.math.BigDecimal")
                    .withFinalField("currency", PKG + ".Currency")
                    .build();

            GraphQuery query = graph.query();
            TypeNode money = query.type(PKG + ".Money").orElseThrow();

            ClassificationResult result = classifier.classify(money, query);

            assertThat(result.status())
                    .as("Record with @ValueObject should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind())
                    .isEqualTo(DomainKind.VALUE_OBJECT.name());
        }

        @Test
        @DisplayName("should classify sealed value object hierarchy")
        void shouldClassifySealedValueObjectHierarchy() {
            // sealed interface PaymentMethod permits CreditCard, BankTransfer {}
            // record CreditCard(String number) implements PaymentMethod {}
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withInterface(PKG + ".PaymentMethod")
                    .annotatedWith("org.jmolecules.ddd.annotation.ValueObject")
                    .withRecord(PKG + ".CreditCard")
                    .implementing(PKG + ".PaymentMethod")
                    .withFinalField("number", "java.lang.String")
                    .build();

            GraphQuery query = graph.query();

            // CreditCard should inherit VALUE_OBJECT classification from PaymentMethod
            TypeNode creditCard = query.type(PKG + ".CreditCard").orElseThrow();
            ClassificationResult result = classifier.classify(creditCard, query);

            assertThat(result.status())
                    .as("Record implementing ValueObject interface should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind())
                    .isEqualTo(DomainKind.VALUE_OBJECT.name());
        }
    }

    // =========================================================================
    // Interface Hierarchies
    // =========================================================================

    @Nested
    @DisplayName("Interface Hierarchies")
    class InterfaceHierarchyTests {

        @Test
        @DisplayName("should NOT classify interfaces as domain types")
        void shouldNotClassifyInterfacesAsDomainTypes() {
            // interface OrderRepository {}
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withInterface(PKG + ".OrderRepository")
                    .build();

            GraphQuery query = graph.query();
            TypeNode orderRepository = query.type(PKG + ".OrderRepository").orElseThrow();

            ClassificationResult result = classifier.classify(orderRepository, query);

            // Interfaces are not domain types (they could be ports though)
            assertThat(result.status())
                    .as("Interfaces should not be classified as domain types by domain classifier")
                    .isEqualTo(ClassificationStatus.UNCLASSIFIED);
        }

        @Test
        @DisplayName("should classify enum with @ValueObject annotation")
        void shouldClassifyEnumWithValueObjectAnnotation() {
            // enum OrderStatus { PENDING, CONFIRMED, SHIPPED } with @ValueObject
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withEnum(PKG + ".OrderStatus")
                    .annotatedWith("org.jmolecules.ddd.annotation.ValueObject")
                    .build();

            GraphQuery query = graph.query();
            TypeNode orderStatus = query.type(PKG + ".OrderStatus").orElseThrow();

            ClassificationResult result = classifier.classify(orderStatus, query);

            assertThat(result.status())
                    .as("Enum with @ValueObject should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            assertThat(result.kind())
                    .isEqualTo(DomainKind.VALUE_OBJECT.name());
        }
    }

    // =========================================================================
    // Generic Type Hierarchies
    // =========================================================================

    @Nested
    @DisplayName("Generic Type Hierarchies")
    class GenericTypeHierarchyTests {

        @Test
        @DisplayName("should classify class extending AbstractEntity<ID>")
        void shouldClassifyClassExtendingAbstractEntity() {
            // class Order extends AbstractEntity<OrderId> {}
            // where AbstractEntity<ID> is a common base class with @Entity
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".AbstractEntity")
                    .asPublic()
                    .asAbstract()
                    .annotatedWith("org.jmolecules.ddd.annotation.Entity")
                    .withField("id", "java.util.UUID")
                    .withClass(PKG + ".Order")
                    .asPublic()
                    .extending(PKG + ".AbstractEntity")
                    .build();

            GraphQuery query = graph.query();

            // First classify parent
            TypeNode abstractEntity = query.type(PKG + ".AbstractEntity").orElseThrow();
            classifier.classify(abstractEntity, query);

            // Child should inherit classification
            TypeNode order = query.type(PKG + ".Order").orElseThrow();
            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.status())
                    .as("Class extending AbstractEntity<ID> should be classified as ENTITY")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
        }
    }

    // =========================================================================
    // Repository-Based Classification with Inheritance
    // =========================================================================

    @Nested
    @DisplayName("Repository-Based Classification with Inheritance")
    class RepositoryInheritanceTests {

        @Test
        @Disabled("TestGraphBuilder does not create TYPE_REFERENCE edges needed by RepositoryDominantCriteria. "
                + "See DomainClassifierTest.shouldClassifyRepositoryDominantAsAggregateRoot for working test.")
        @DisplayName("should classify aggregate root when referenced by repository")
        void shouldClassifyAggregateRootWhenReferencedByRepository() {
            // NOTE: This test requires real source compilation to create proper edges.
            // TestGraphBuilder's withMethod() doesn't create TYPE_REFERENCE edges.
            // The working test is in DomainClassifierTest.shouldClassifyRepositoryDominantAsAggregateRoot
            //
            // interface OrderRepository { Order save(Order order); }
            // class Order { UUID id; }
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".Order")
                    .asPublic()
                    .withField("id", "java.util.UUID")
                    .withInterface(PKG + ".OrderRepository")
                    .withMethod("save", PKG + ".Order", PKG + ".Order")
                    .withMethod("findById", "java.util.Optional<" + PKG + ".Order>", PKG + ".OrderId")
                    .build();

            GraphQuery query = graph.query();
            TypeNode order = query.type(PKG + ".Order").orElseThrow();

            ClassificationResult result = classifier.classify(order, query);

            assertThat(result.status())
                    .as("Class referenced by Repository should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
            // Repository in DDD is specifically for Aggregate Roots
            // RepositoryDominantCriteria (priority 80) > HasIdentityCriteria (priority 60)
            assertThat(result.kind())
                    .as("Type referenced by Repository should be AGGREGATE_ROOT (per DDD)")
                    .isEqualTo(DomainKind.AGGREGATE_ROOT.name());
        }

        @Test
        @Disabled("TODO: Implement subclass detection when parent is aggregate root via repository")
        @DisplayName("should classify subclass when parent is detected via repository")
        void shouldClassifySubclassWhenParentIsDetectedViaRepository() {
            // interface OrderRepository { Order save(Order order); }
            // class Order { UUID id; }
            // class SpecialOrder extends Order { }
            ApplicationGraph graph = TestGraphBuilder.create()
                    .withClass(PKG + ".Order")
                    .asPublic()
                    .withField("id", "java.util.UUID")
                    .withClass(PKG + ".SpecialOrder")
                    .asPublic()
                    .extending(PKG + ".Order")
                    .withInterface(PKG + ".OrderRepository")
                    .withMethod("save", PKG + ".Order", PKG + ".Order")
                    .build();

            GraphQuery query = graph.query();

            // First classify parent via repository
            TypeNode order = query.type(PKG + ".Order").orElseThrow();
            ClassificationResult parentResult = classifier.classify(order, query);
            assertThat(parentResult.status()).isEqualTo(ClassificationStatus.CLASSIFIED);

            // Then classify child - should inherit
            TypeNode specialOrder = query.type(PKG + ".SpecialOrder").orElseThrow();
            ClassificationResult childResult = classifier.classify(specialOrder, query);

            assertThat(childResult.status())
                    .as("Subclass of repository-managed type should be classified")
                    .isEqualTo(ClassificationStatus.CLASSIFIED);
        }
    }
}
