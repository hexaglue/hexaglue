package com.example.enterprise.warehouse.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for warehouse bounded context.
 */
public class WarehouseAggregate2 {
    private final WarehouseAggregate2Id id;
    private final String name;
    private final List<WarehouseItem2> items;
    private WarehouseStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public WarehouseAggregate2(WarehouseAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("WarehouseAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = WarehouseStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(WarehouseItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != WarehouseStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = WarehouseStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != WarehouseStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = WarehouseStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public WarehouseAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<WarehouseItem2> getItems() { return Collections.unmodifiableList(items); }
    public WarehouseStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
