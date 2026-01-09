package com.example.enterprise.inventory.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for inventory bounded context.
 */
public class InventoryAggregate3 {
    private final InventoryAggregate3Id id;
    private final String name;
    private final List<InventoryItem3> items;
    private InventoryStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public InventoryAggregate3(InventoryAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("InventoryAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = InventoryStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(InventoryItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != InventoryStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = InventoryStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != InventoryStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = InventoryStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public InventoryAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<InventoryItem3> getItems() { return Collections.unmodifiableList(items); }
    public InventoryStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
