package com.example.enterprise.shipping.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in ShippingAggregate3.
 */
public class ShippingItem3 {
    private final ShippingItem3Id id;
    private final String description;
    private ShippingAmount3 amount;
    private final Instant createdAt;

    public ShippingItem3(ShippingItem3Id id, String description, ShippingAmount3 amount) {
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

    public void updateAmount(ShippingAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public ShippingItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public ShippingAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
