package com.example.enterprise.ordering.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in OrderingAggregate2.
 */
public class OrderingItem2 {
    private final OrderingItem2Id id;
    private final String description;
    private OrderingAmount2 amount;
    private final Instant createdAt;

    public OrderingItem2(OrderingItem2Id id, String description, OrderingAmount2 amount) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public void updateAmount(OrderingAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public OrderingItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public OrderingAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
