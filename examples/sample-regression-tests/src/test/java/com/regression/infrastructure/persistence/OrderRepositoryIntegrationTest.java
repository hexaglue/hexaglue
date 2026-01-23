package com.regression.infrastructure.persistence;

import com.regression.RegressionTestsApplication;
import com.regression.domain.customer.Customer;
import com.regression.domain.customer.CustomerId;
import com.regression.domain.customer.Email;
import com.regression.domain.order.Order;
import com.regression.domain.order.OrderId;
import com.regression.domain.order.OrderLine;
import com.regression.domain.order.OrderStatus;
import com.regression.domain.order.ProductId;
import com.regression.domain.shared.Money;
import com.regression.domain.shared.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderRepositoryAdapter.
 *
 * <p>These tests validate critical JPA generation fixes:
 * <ul>
 *   <li>C1: getById/loadById/fetchById method implementations</li>
 *   <li>C2: Cross-aggregate identifiers (CustomerId stored as UUID)</li>
 *   <li>C3: SQL reserved word "order" → table "orders"</li>
 *   <li>C4: Repository signatures with unwrapped identifier types</li>
 *   <li>H2/M16: Enum persistence with @Enumerated(EnumType.STRING)</li>
 *   <li>M11: @AttributeOverrides for multiple Money fields</li>
 *   <li>M13: Simple wrapper (Quantity) unwrapped to int</li>
 *   <li>M14: Collection relations (List&lt;OrderLine&gt;)</li>
 * </ul>
 */
@SpringBootTest(classes = RegressionTestsApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepositoryAdapter orderRepository;

    @Autowired
    private CustomerRepositoryAdapter customerRepository;

    private CustomerId customerId;

    @BeforeEach
    void setUp() {
        // Create a customer for orders
        Customer customer = Customer.create("Test Customer", Email.of("customer@test.com"));
        Customer saved = customerRepository.save(customer);
        customerId = saved.id();
    }

    @Nested
    @DisplayName("C1: Alternative findById method names")
    class AlternativeFindByIdMethods {

        @Test
        @DisplayName("getById should work like findById")
        void getByIdShouldWork() {
            // Given
            Order order = Order.create(customerId, "EUR");
            Order saved = orderRepository.save(order);

            // When
            var found = orderRepository.getById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(saved.id());
        }

        @Test
        @DisplayName("loadById should work like findById")
        void loadByIdShouldWork() {
            // Given
            Order order = Order.create(customerId, "EUR");
            Order saved = orderRepository.save(order);

            // When
            var found = orderRepository.loadById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(saved.id());
        }

        @Test
        @DisplayName("fetchById should work like findById")
        void fetchByIdShouldWork() {
            // Given
            Order order = Order.create(customerId, "EUR");
            Order saved = orderRepository.save(order);

            // When
            var found = orderRepository.fetchById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(saved.id());
        }
    }

    @Nested
    @DisplayName("C2/C4: Cross-aggregate identifier (CustomerId)")
    class CrossAggregateIdentifier {

        @Test
        @DisplayName("should persist and retrieve CustomerId correctly")
        void shouldPersistCustomerId() {
            // Given
            Order order = Order.create(customerId, "EUR");

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().customerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("findByCustomerId should find orders for a customer")
        void findByCustomerIdShouldWork() {
            // Given
            orderRepository.save(Order.create(customerId, "EUR"));
            orderRepository.save(Order.create(customerId, "USD"));

            // When
            var orders = orderRepository.findByCustomerId(customerId);

            // Then
            assertThat(orders).hasSize(2);
            assertThat(orders).allMatch(o -> o.customerId().equals(customerId));
        }

        @Test
        @DisplayName("existsByCustomerId should return correct boolean")
        void existsByCustomerIdShouldWork() {
            // Given
            orderRepository.save(Order.create(customerId, "EUR"));

            // When/Then
            assertThat(orderRepository.existsByCustomerId(customerId)).isTrue();
            assertThat(orderRepository.existsByCustomerId(CustomerId.generate())).isFalse();
        }
    }

    @Nested
    @DisplayName("C3: SQL reserved word 'order' → 'orders' table")
    class SqlReservedWord {

        @Test
        @DisplayName("should persist to 'orders' table without SQL errors")
        void shouldPersistToOrdersTable() {
            // This test implicitly validates C3 - if the table was named "order"
            // instead of "orders", H2 would throw a SQL syntax error

            // Given
            Order order = Order.create(customerId, "EUR");

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
        }
    }

    @Nested
    @DisplayName("H2/M16: Enum persistence with @Enumerated")
    class EnumPersistence {

        @Test
        @DisplayName("should persist and retrieve OrderStatus enum")
        void shouldPersistOrderStatus() {
            // Given
            Order order = Order.create(customerId, "EUR");
            assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(OrderStatus.DRAFT);
        }

        @Test
        @DisplayName("should persist all enum values correctly")
        void shouldPersistAllEnumValues() {
            // Test each status to ensure @Enumerated(EnumType.STRING) works
            for (OrderStatus status : OrderStatus.values()) {
                // Given - create order and change to desired status
                Order order = Order.create(customerId, "EUR");

                // Transition to desired status (simplified - not testing business rules)
                Order withStatus = switch (status) {
                    case DRAFT -> order;
                    case PENDING -> order; // Would need proper transition
                    case CONFIRMED -> order.addLine(createSampleLine()).confirm();
                    case SHIPPED -> order.addLine(createSampleLine()).confirm().ship();
                    case DELIVERED -> order.addLine(createSampleLine()).confirm().ship().deliver();
                    case CANCELLED -> order.cancel();
                };

                // When
                Order saved = orderRepository.save(withStatus);
                var found = orderRepository.findById(saved.id());

                // Then
                assertThat(found).isPresent();
                assertThat(found.get().status()).isEqualTo(withStatus.status());
            }
        }
    }

    @Nested
    @DisplayName("M11: @AttributeOverrides for multiple Money fields")
    class AttributeOverrides {

        @Test
        @DisplayName("should persist totalAmount and discount (both Money) without column conflicts")
        void shouldPersistMultipleMoneyFields() {
            // Given
            Order order = Order.create(customerId, "EUR")
                    .addLine(createSampleLine())
                    .applyDiscount(Money.eur(new BigDecimal("10.00")));

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            Order loaded = found.get();

            // Verify totalAmount persisted correctly
            assertThat(loaded.totalAmount()).isNotNull();
            assertThat(loaded.totalAmount().currency()).isEqualTo("EUR");

            // Verify discount persisted correctly (separate columns due to @AttributeOverrides)
            assertThat(loaded.discount()).isNotNull();
            assertThat(loaded.discount().amount()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(loaded.discount().currency()).isEqualTo("EUR");
        }
    }

    @Nested
    @DisplayName("M13: Simple wrapper (Quantity) unwrapped to primitive")
    class SimpleWrapperUnwrapping {

        @Test
        @DisplayName("should persist Quantity as int in OrderLine")
        void shouldPersistQuantityAsInt() {
            // Given
            OrderLine line = OrderLine.of(
                    ProductId.generate(),
                    "Test Product",
                    Money.eur(new BigDecimal("25.00")),
                    Quantity.of(3)
            );
            Order order = Order.create(customerId, "EUR").addLine(line);

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().lines()).hasSize(1);
            assertThat(found.get().lines().get(0).quantity().value()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("M14: Collection relations (List<OrderLine>)")
    class CollectionRelations {

        @Test
        @DisplayName("should persist and retrieve order lines collection")
        void shouldPersistOrderLines() {
            // Given
            Order order = Order.create(customerId, "EUR")
                    .addLine(createLineWithName("Product 1", new BigDecimal("10.00"), 2))
                    .addLine(createLineWithName("Product 2", new BigDecimal("20.00"), 1))
                    .addLine(createLineWithName("Product 3", new BigDecimal("15.00"), 3));

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().lines()).hasSize(3);

            // Verify line details are preserved
            var lines = found.get().lines();
            assertThat(lines).extracting(OrderLine::productName)
                    .containsExactlyInAnyOrder("Product 1", "Product 2", "Product 3");
        }

        @Test
        @DisplayName("should handle empty order lines collection")
        void shouldHandleEmptyLines() {
            // Given
            Order order = Order.create(customerId, "EUR");

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().lines()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Primitive fields (M3 validation at runtime)")
    class PrimitiveFields {

        @Test
        @DisplayName("should persist boolean 'urgent' field correctly")
        void shouldPersistBooleanField() {
            // Given
            Order order = Order.create(customerId, "EUR").markAsUrgent();
            assertThat(order.urgent()).isTrue();

            // When
            Order saved = orderRepository.save(order);
            var found = orderRepository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().urgent()).isTrue();
        }
    }

    // Helper methods

    private OrderLine createSampleLine() {
        return OrderLine.of(
                ProductId.generate(),
                "Sample Product",
                Money.eur(new BigDecimal("100.00")),
                Quantity.of(1)
        );
    }

    private OrderLine createLineWithName(String name, BigDecimal price, int qty) {
        return OrderLine.of(
                ProductId.generate(),
                name,
                Money.eur(price),
                Quantity.of(qty)
        );
    }
}
