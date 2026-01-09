package com.example.enterprise.ordering.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for ordering bounded context.
 */
public class OrderingAggregate2 {
    private final OrderingAggregate2Id id;
    private final String name;
    private final List<OrderingItem2> items;
    private OrderingStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public OrderingAggregate2(OrderingAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("OrderingAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = OrderingStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderingItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != OrderingStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = OrderingStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != OrderingStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = OrderingStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public OrderingAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<OrderingItem2> getItems() { return Collections.unmodifiableList(items); }
    public OrderingStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
