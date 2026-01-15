package io.hexaglue.arch.integration.fixtures.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order aggregate root.
 *
 * <p>Represents a customer order with line items, shipping address, and total.</p>
 */
@AggregateRoot
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private Address shippingAddress;
    private OrderStatus status;
    private final Instant createdAt;

    public Order(OrderId id, CustomerId customerId) {
        this.id = id;
        this.customerId = customerId;
        this.lines = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public void addLine(String productId, int quantity, Money unitPrice) {
        lines.add(new OrderLine(productId, quantity, unitPrice));
    }

    public void setShippingAddress(Address address) {
        this.shippingAddress = address;
    }

    public void place() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot place empty order");
        }
        if (shippingAddress == null) {
            throw new IllegalStateException("Shipping address required");
        }
        this.status = OrderStatus.PLACED;
    }

    public Money total() {
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.euros(java.math.BigDecimal.ZERO), Money::add);
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    public OrderStatus getStatus() {
        return status;
    }
}
