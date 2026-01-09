package com.example.enterprise.warehouse.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in WarehouseAggregate1.
 */
public class WarehouseItem1 {
    private final WarehouseItem1Id id;
    private final String description;
    private WarehouseAmount1 amount;
    private final Instant createdAt;

    public WarehouseItem1(WarehouseItem1Id id, String description, WarehouseAmount1 amount) {
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

    public void updateAmount(WarehouseAmount1 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public WarehouseItem1Id getId() { return id; }
    public String getDescription() { return description; }
    public WarehouseAmount1 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
