package com.example.enterprise.ordering.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for ordering bounded context.
 */
public class OrderingAggregate3 {
    private final OrderingAggregate3Id id;
    private final String name;
    private final List<OrderingItem3> items;
    private OrderingStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public OrderingAggregate3(OrderingAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("OrderingAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = OrderingStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderingItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != OrderingStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = OrderingStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != OrderingStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = OrderingStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public OrderingAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<OrderingItem3> getItems() { return Collections.unmodifiableList(items); }
    public OrderingStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
