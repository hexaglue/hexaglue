package com.example.ecommerce.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root representing an Order in the e-commerce domain.
 */
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> orderLines;
    private final Address shippingAddress;
    private OrderStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(
        OrderId id,
        CustomerId customerId,
        Address shippingAddress,
        List<OrderLine> orderLines
    ) {
        if (id == null) {
            throw new IllegalArgumentException("OrderId cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId cannot be null");
        }
        if (shippingAddress == null) {
            throw new IllegalArgumentException("Shipping address cannot be null");
        }
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line item");
        }

        this.id = id;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.orderLines = new ArrayList<>(orderLines);
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only confirm pending orders");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Can only ship confirmed orders");
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }

    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Can only deliver shipped orders");
        }
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered orders");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public Money calculateTotal() {
        Money total = orderLines.get(0).calculateTotal();
        for (int i = 1; i < orderLines.size(); i++) {
            total = total.add(orderLines.get(i).calculateTotal());
        }
        return total;
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getOrderLines() {
        return Collections.unmodifiableList(orderLines);
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
