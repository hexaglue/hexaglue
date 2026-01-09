package com.example.enterprise.warehouse.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in WarehouseAggregate3.
 */
public class WarehouseItem3 {
    private final WarehouseItem3Id id;
    private final String description;
    private WarehouseAmount3 amount;
    private final Instant createdAt;

    public WarehouseItem3(WarehouseItem3Id id, String description, WarehouseAmount3 amount) {
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

    public void updateAmount(WarehouseAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public WarehouseItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public WarehouseAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
