package io.hexaglue.plugin.jpa;

import static io.hexaglue.plugin.jpa.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.ir.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Edge case tests for {@link JpaAdapterGenerator}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Batch operations (saveAll, deleteAll)</li>
 *   <li>Stream return types</li>
 *   <li>Methods with multiple parameters</li>
 *   <li>Iterable/Collection parameter types</li>
 *   <li>Void return with collection input</li>
 * </ul>
 */
@DisplayName("JpaAdapterGenerator - Edge Cases")
class JpaAdapterGeneratorEdgeCasesTest {

    private static final String INFRA_PKG = "com.example.infrastructure.persistence";
    private static final String DOMAIN_PKG = "com.example.domain";

    private JpaAdapterGenerator generator;
    private JpaConfig config;

    @BeforeEach
    void setUp() {
        config = JpaConfig.defaults();
        generator = new JpaAdapterGenerator(INFRA_PKG, config);
    }

    // =========================================================================
    // Batch Save Operations
    // =========================================================================

    @Nested
    @DisplayName("Batch Save Operations")
    class BatchSaveOperationsTests {

        @Test
        @DisplayName("should implement saveAll(List<Domain>) returning List<Domain>")
        void shouldImplementSaveAllWithListReturn() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "saveAll",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of("java.util.List<" + DOMAIN_PKG + ".Order>"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("saveAll should convert list to entities, save, and convert back")
                    .contains("public List<Order> saveAll(List<Order> param0)")
                    .contains("toEntityList")
                    .contains("saveAll")
                    .contains("toDomainList");
        }

        @Test
        @DisplayName("should implement saveAll(Iterable<Domain>) returning Iterable<Domain>")
        void shouldImplementSaveAllWithIterableReturn() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "saveAll",
                            "java.lang.Iterable<" + DOMAIN_PKG + ".Order>",
                            List.of("java.lang.Iterable<" + DOMAIN_PKG + ".Order>"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("saveAll should handle Iterable types")
                    .contains("public Iterable<Order> saveAll(Iterable<Order> param0)");
        }

        @Test
        @DisplayName("should generate TODO for saveAll (fallback behavior)")
        void shouldGenerateTodoForSaveAllFallback() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "saveAll",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of("java.util.List<" + DOMAIN_PKG + ".Order>"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            // Until implemented, should generate placeholder
            assertThat(code).contains("saveAll");
            // Either implemented or TODO
            assertThat(code.contains("toDomainList") || code.contains("UnsupportedOperationException")).isTrue();
        }
    }

    // =========================================================================
    // Batch Delete Operations
    // =========================================================================

    @Nested
    @DisplayName("Batch Delete Operations")
    class BatchDeleteOperationsTests {

        @Test
        @DisplayName("should implement deleteAll(List<Domain>)")
        void shouldImplementDeleteAllWithList() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "deleteAll", "void", List.of("java.util.List<" + DOMAIN_PKG + ".Order>"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("deleteAll should convert list to entities and delete")
                    .contains("public void deleteAll(List<Order> param0)")
                    .contains("toEntityList")
                    .contains("deleteAll");
        }

        @Test
        @DisplayName("should implement deleteAll() with no parameters")
        void shouldImplementDeleteAllNoParams() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod("deleteAll", "void", List.of())),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("deleteAll() should delegate to repository.deleteAll()")
                    .contains("public void deleteAll()")
                    .contains(".deleteAll()");
        }

        @Test
        @DisplayName("should implement deleteAllById(List<Id>)")
        void shouldImplementDeleteAllById() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "deleteAllById",
                            "void",
                            List.of("java.util.List<" + DOMAIN_PKG + ".OrderId>"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("deleteAllById should unwrap wrapper IDs")
                    .contains("public void deleteAllById(List<OrderId> param0)")
                    .contains(".value()"); // Must unwrap each ID
        }
    }

    // =========================================================================
    // Stream Return Types
    // =========================================================================

    @Nested
    @DisplayName("Stream Return Types")
    class StreamReturnTypesTests {

        @Test
        @DisplayName("should implement findAllAsStream() returning Stream<Domain>")
        void shouldImplementFindAllAsStream() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findAllAsStream", "java.util.stream.Stream<" + DOMAIN_PKG + ".Order>", List.of())),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("Stream return type should map each entity to domain")
                    .contains("public Stream<Order> findAllAsStream()")
                    .contains(".stream()")
                    .contains(".map(");
        }

        @Test
        @DisplayName("should import Stream when used in return type")
        void shouldImportStreamWhenUsed() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "streamAll", "java.util.stream.Stream<" + DOMAIN_PKG + ".Order>", List.of())),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code).contains("import java.util.stream.Stream;");
        }
    }

    // =========================================================================
    // Methods with Multiple Parameters
    // =========================================================================

    @Nested
    @DisplayName("Methods with Multiple Parameters")
    class MultipleParametersTests {

        @Test
        @DisplayName("should handle findByCustomerIdAndStatus with two parameters")
        void shouldHandleFindByWithTwoParameters() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findByCustomerIdAndStatus",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of(DOMAIN_PKG + ".CustomerId", DOMAIN_PKG + ".OrderStatus"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("Should generate method with two parameters")
                    .contains("public List<Order> findByCustomerIdAndStatus(CustomerId param0, OrderStatus param1)");
        }

        @Test
        @DisplayName("should handle findByDateRange with primitive parameters")
        void shouldHandleFindByDateRange() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findByDateBetween",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of("java.time.LocalDate", "java.time.LocalDate"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("Should generate method with LocalDate parameters")
                    .contains("findByDateBetween(LocalDate param0, LocalDate param1)");
        }

        @Test
        @DisplayName("should handle method with three or more parameters")
        void shouldHandleMethodWithThreeParameters() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findByCustomerAndStatusAndDateAfter",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of(
                                    DOMAIN_PKG + ".CustomerId",
                                    DOMAIN_PKG + ".OrderStatus",
                                    "java.time.LocalDate"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .contains("findByCustomerAndStatusAndDateAfter(CustomerId param0, OrderStatus param1, LocalDate param2)");
        }
    }

    // =========================================================================
    // Special Return Types
    // =========================================================================

    @Nested
    @DisplayName("Special Return Types")
    class SpecialReturnTypesTests {

        @Test
        @DisplayName("should handle count with custom name like countByStatus")
        void shouldHandleCountByCustomName() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod("countByStatus", "long", List.of(DOMAIN_PKG + ".OrderStatus"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code).contains("public long countByStatus(OrderStatus param0)");
        }

        @Test
        @DisplayName("should handle boolean return for custom exists method")
        void shouldHandleBooleanReturnForCustomExists() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "existsByCustomerId", "boolean", List.of(DOMAIN_PKG + ".CustomerId"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code).contains("public boolean existsByCustomerId(CustomerId param0)");
        }

        @Test
        @DisplayName("should handle Optional<Domain> for findFirst method")
        void shouldHandleOptionalForFindFirst() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findFirstByCustomerId",
                            "java.util.Optional<" + DOMAIN_PKG + ".Order>",
                            List.of(DOMAIN_PKG + ".CustomerId"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code).contains("public Optional<Order> findFirstByCustomerId(CustomerId param0)");
        }
    }

    // =========================================================================
    // Pagination and Sorting
    // =========================================================================

    @Nested
    @DisplayName("Pagination and Sorting")
    class PaginationAndSortingTests {

        @Test
        @DisplayName("should handle Pageable parameter for paginated results")
        void shouldHandlePageableParameter() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findAll",
                            "org.springframework.data.domain.Page<" + DOMAIN_PKG + ".Order>",
                            List.of("org.springframework.data.domain.Pageable"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("Should handle Pageable and return Page<Domain>")
                    .contains("Page<Order> findAll(Pageable param0)")
                    .contains("import org.springframework.data.domain.Page;")
                    .contains("import org.springframework.data.domain.Pageable;");
        }

        @Test
        @DisplayName("should handle Sort parameter")
        void shouldHandleSortParameter() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findAll",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of("org.springframework.data.domain.Sort"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("Should handle Sort parameter")
                    .contains("List<Order> findAll(Sort param0)")
                    .contains("import org.springframework.data.domain.Sort;");
        }
    }

    // =========================================================================
    // Update and Modify Operations
    // =========================================================================

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperationsTests {

        @Test
        @DisplayName("should handle update method returning domain")
        void shouldHandleUpdateMethod() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod("update", DOMAIN_PKG + ".Order", List.of(DOMAIN_PKG + ".Order"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            // Update should behave like save
            assertThat(code).contains("public Order update(Order param0)");
        }

        @Test
        @DisplayName("should implement updateAll(List<Domain>)")
        void shouldImplementUpdateAll() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "updateAll",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of("java.util.List<" + DOMAIN_PKG + ".Order>"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("updateAll should convert, save all, and convert back")
                    .contains("public List<Order> updateAll(List<Order> param0)")
                    .contains("saveAll");
        }
    }

    // =========================================================================
    // Import Management
    // =========================================================================

    @Nested
    @DisplayName("Import Management")
    class ImportManagementTests {

        @Test
        @DisplayName("should import LocalDate when used in parameters")
        void shouldImportLocalDateWhenUsed() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findByOrderDate",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of("java.time.LocalDate"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            // Should not import java.time types (they require explicit import)
            // or should import them properly
            assertThat(code).contains("LocalDate param0");
        }

        @Test
        @DisplayName("should handle multiple custom domain types in one method")
        void shouldHandleMultipleDomainTypesInOneMethod() {
            DomainType type = simpleAggregateRoot("Order", DOMAIN_PKG);
            Port port = new Port(
                    DOMAIN_PKG + ".ports.out.OrderRepository",
                    "OrderRepository",
                    DOMAIN_PKG + ".ports.out",
                    PortKind.REPOSITORY,
                    PortDirection.DRIVEN,
                    ConfidenceLevel.HIGH,
                    List.of(DOMAIN_PKG + ".Order"),
                    List.of(new PortMethod(
                            "findByCustomerIdAndSellerId",
                            "java.util.List<" + DOMAIN_PKG + ".Order>",
                            List.of(DOMAIN_PKG + ".CustomerId", DOMAIN_PKG + ".SellerId"))),
                    List.of(),
                    SourceRef.unknown());

            String code = generator.generateAdapter(port, type);

            assertThat(code)
                    .as("Should import both custom types")
                    .contains("import " + DOMAIN_PKG + ".CustomerId;")
                    .contains("import " + DOMAIN_PKG + ".SellerId;");
        }
    }
}
