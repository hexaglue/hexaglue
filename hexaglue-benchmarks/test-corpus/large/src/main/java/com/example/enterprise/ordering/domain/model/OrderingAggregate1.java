package com.example.enterprise.ordering.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for ordering bounded context.
 */
public class OrderingAggregate1 {
    private final OrderingAggregate1Id id;
    private final String name;
    private final List<OrderingItem1> items;
    private OrderingStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public OrderingAggregate1(OrderingAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("OrderingAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = OrderingStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderingItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != OrderingStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = OrderingStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != OrderingStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = OrderingStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public OrderingAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<OrderingItem1> getItems() { return Collections.unmodifiableList(items); }
    public OrderingStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
