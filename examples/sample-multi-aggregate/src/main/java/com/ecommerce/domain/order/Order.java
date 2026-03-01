package com.ecommerce.domain.order;

import com.ecommerce.domain.customer.CustomerId;
import com.ecommerce.domain.product.ProductId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order aggregate root.
 */
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private final Instant createdAt;
    private OrderStatus status;
    private Address shippingAddress;

    public Order(OrderId id, CustomerId customerId) {
        this.id = id;
        this.customerId = customerId;
        this.lines = new ArrayList<>();
        this.createdAt = Instant.now();
        this.status = OrderStatus.DRAFT;
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void addLine(OrderLine line) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify a non-draft order");
        }
        lines.add(line);
    }

    public void removeLine(ProductId productId) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify a non-draft order");
        }
        lines.removeIf(line -> line.getProductId().equals(productId));
    }

    public void setShippingAddress(Address address) {
        this.shippingAddress = address;
    }

    public Money total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.euros(java.math.BigDecimal.ZERO), Money::add);
    }

    public void place() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot place an empty order");
        }
        if (shippingAddress == null) {
            throw new IllegalStateException("Shipping address is required");
        }
        this.status = OrderStatus.PLACED;
    }

    public void pay() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Order must be placed before payment");
        }
        this.status = OrderStatus.PAID;
    }

    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Order must be paid before shipping");
        }
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order must be shipped before delivery");
        }
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a shipped or delivered order");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
