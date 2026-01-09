package com.example.enterprise.ordering.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in OrderingAggregate3.
 */
public class OrderingItem3 {
    private final OrderingItem3Id id;
    private final String description;
    private OrderingAmount3 amount;
    private final Instant createdAt;

    public OrderingItem3(OrderingItem3Id id, String description, OrderingAmount3 amount) {
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

    public void updateAmount(OrderingAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public OrderingItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public OrderingAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
