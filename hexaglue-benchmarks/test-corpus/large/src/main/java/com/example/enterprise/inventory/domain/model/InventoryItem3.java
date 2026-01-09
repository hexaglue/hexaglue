package com.example.enterprise.inventory.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in InventoryAggregate3.
 */
public class InventoryItem3 {
    private final InventoryItem3Id id;
    private final String description;
    private InventoryAmount3 amount;
    private final Instant createdAt;

    public InventoryItem3(InventoryItem3Id id, String description, InventoryAmount3 amount) {
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

    public void updateAmount(InventoryAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public InventoryItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public InventoryAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
