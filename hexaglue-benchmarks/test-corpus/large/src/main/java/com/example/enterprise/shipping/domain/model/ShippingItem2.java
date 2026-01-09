package com.example.enterprise.shipping.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in ShippingAggregate2.
 */
public class ShippingItem2 {
    private final ShippingItem2Id id;
    private final String description;
    private ShippingAmount2 amount;
    private final Instant createdAt;

    public ShippingItem2(ShippingItem2Id id, String description, ShippingAmount2 amount) {
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

    public void updateAmount(ShippingAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public ShippingItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public ShippingAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
