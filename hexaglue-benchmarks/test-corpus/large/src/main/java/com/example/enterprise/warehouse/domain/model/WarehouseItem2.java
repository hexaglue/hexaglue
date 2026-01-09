package com.example.enterprise.warehouse.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in WarehouseAggregate2.
 */
public class WarehouseItem2 {
    private final WarehouseItem2Id id;
    private final String description;
    private WarehouseAmount2 amount;
    private final Instant createdAt;

    public WarehouseItem2(WarehouseItem2Id id, String description, WarehouseAmount2 amount) {
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

    public void updateAmount(WarehouseAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public WarehouseItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public WarehouseAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
