package com.coffeeshop.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order aggregate root.
 * Expected classification: AGGREGATE_ROOT (has identity, used in Orders repository)
 */
public class Order {

    private final OrderId id;
    private final String customerName;
    private final Location location;
    private final List<LineItem> items;
    private final Instant createdAt;
    private OrderStatus status;

    public Order(OrderId id, String customerName, Location location) {
        this.id = id;
        this.customerName = customerName;
        this.location = location;
        this.items = new ArrayList<>();
        this.createdAt = Instant.now();
        this.status = OrderStatus.PENDING;
    }

    public OrderId getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Location getLocation() {
        return location;
    }

    public List<LineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void addItem(LineItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot add items to a non-pending order");
        }
        items.add(item);
    }

    public BigDecimal totalAmount() {
        return items.stream()
                .map(LineItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void submit() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot submit an empty order");
        }
        this.status = OrderStatus.SUBMITTED;
    }

    public void complete() {
        if (status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Can only complete a submitted order");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
