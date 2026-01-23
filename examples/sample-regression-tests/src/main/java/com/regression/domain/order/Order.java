package com.regression.domain.order;

import com.regression.domain.customer.CustomerId;
import com.regression.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order aggregate root.
 * <p>
 * Tests multiple corrections:
 * - C2: CustomerId cross-aggregate reference (should be stored as UUID, not @Embedded)
 * - C3: "Order" is a SQL reserved word, table should be "orders"
 * - H2: OrderStatus enum should be classified as VALUE_OBJECT
 * - M3: primitiveField (boolean ordered) should have NON_NULL nullability
 * - M4: lines (List) should have COLLECTION cardinality
 * - M11: totalAmount and discount (both Money) require @AttributeOverrides
 * - M14: List<OrderLine> lines should appear as relation in diagrams
 */
public record Order(
        OrderId id,
        CustomerId customerId,
        List<OrderLine> lines,
        Money totalAmount,
        Money discount,
        OrderStatus status,
        boolean urgent,
        LocalDateTime createdAt) {

    public Order {
        if (id == null) {
            throw new IllegalArgumentException("Order id cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("customerId cannot be null");
        }
        if (lines == null) {
            lines = new ArrayList<>();
        } else {
            lines = new ArrayList<>(lines);
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("totalAmount cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }

    public static Order create(CustomerId customerId, String currency) {
        return new Order(
                OrderId.generate(),
                customerId,
                new ArrayList<>(),
                Money.of(java.math.BigDecimal.ZERO, currency),
                null,
                OrderStatus.DRAFT,
                false,
                LocalDateTime.now());
    }

    public Order addLine(OrderLine line) {
        var newLines = new ArrayList<>(this.lines);
        newLines.add(line);
        return recalculateTotal(new Order(id, customerId, newLines, totalAmount, discount, status, urgent, createdAt));
    }

    public Order removeLine(ProductId productId) {
        var newLines = new ArrayList<>(this.lines);
        newLines.removeIf(l -> l.productId().equals(productId));
        return recalculateTotal(new Order(id, customerId, newLines, totalAmount, discount, status, urgent, createdAt));
    }

    public Order applyDiscount(Money discount) {
        return new Order(id, customerId, lines, totalAmount, discount, status, urgent, createdAt);
    }

    public Order markAsUrgent() {
        return new Order(id, customerId, lines, totalAmount, discount, status, true, createdAt);
    }

    public Order confirm() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot confirm order without lines");
        }
        return new Order(id, customerId, lines, totalAmount, discount, OrderStatus.CONFIRMED, urgent, createdAt);
    }

    public Order ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot ship order that is not confirmed");
        }
        return new Order(id, customerId, lines, totalAmount, discount, OrderStatus.SHIPPED, urgent, createdAt);
    }

    public Order deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot deliver order that is not shipped");
        }
        return new Order(id, customerId, lines, totalAmount, discount, OrderStatus.DELIVERED, urgent, createdAt);
    }

    public Order cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }
        return new Order(id, customerId, lines, totalAmount, discount, OrderStatus.CANCELLED, urgent, createdAt);
    }

    private static Order recalculateTotal(Order order) {
        if (order.lines.isEmpty()) {
            return new Order(order.id, order.customerId, order.lines,
                    Money.of(java.math.BigDecimal.ZERO, order.totalAmount.currency()),
                    order.discount, order.status, order.urgent, order.createdAt);
        }
        var total = order.lines.stream()
                .map(OrderLine::totalPrice)
                .reduce(Money::add)
                .orElse(Money.of(java.math.BigDecimal.ZERO, order.totalAmount.currency()));
        return new Order(order.id, order.customerId, order.lines, total, order.discount, order.status, order.urgent, order.createdAt);
    }
}
