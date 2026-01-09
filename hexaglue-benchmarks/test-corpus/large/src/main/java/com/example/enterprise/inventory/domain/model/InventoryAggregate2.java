package com.example.enterprise.inventory.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for inventory bounded context.
 */
public class InventoryAggregate2 {
    private final InventoryAggregate2Id id;
    private final String name;
    private final List<InventoryItem2> items;
    private InventoryStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public InventoryAggregate2(InventoryAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("InventoryAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = InventoryStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(InventoryItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != InventoryStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = InventoryStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != InventoryStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = InventoryStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public InventoryAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<InventoryItem2> getItems() { return Collections.unmodifiableList(items); }
    public InventoryStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
