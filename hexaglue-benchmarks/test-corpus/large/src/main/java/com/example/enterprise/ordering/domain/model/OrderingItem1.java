package com.example.enterprise.ordering.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in OrderingAggregate1.
 */
public class OrderingItem1 {
    private final OrderingItem1Id id;
    private final String description;
    private OrderingAmount1 amount;
    private final Instant createdAt;

    public OrderingItem1(OrderingItem1Id id, String description, OrderingAmount1 amount) {
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

    public void updateAmount(OrderingAmount1 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public OrderingItem1Id getId() { return id; }
    public String getDescription() { return description; }
    public OrderingAmount1 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
