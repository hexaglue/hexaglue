package com.example.enterprise.shipping.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in ShippingAggregate1.
 */
public class ShippingItem1 {
    private final ShippingItem1Id id;
    private final String description;
    private ShippingAmount1 amount;
    private final Instant createdAt;

    public ShippingItem1(ShippingItem1Id id, String description, ShippingAmount1 amount) {
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

    public void updateAmount(ShippingAmount1 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public ShippingItem1Id getId() { return id; }
    public String getDescription() { return description; }
    public ShippingAmount1 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
