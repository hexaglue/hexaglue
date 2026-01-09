package com.example.enterprise.inventory.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in InventoryAggregate2.
 */
public class InventoryItem2 {
    private final InventoryItem2Id id;
    private final String description;
    private InventoryAmount2 amount;
    private final Instant createdAt;

    public InventoryItem2(InventoryItem2Id id, String description, InventoryAmount2 amount) {
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

    public void updateAmount(InventoryAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public InventoryItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public InventoryAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
