package com.example.enterprise.supplier.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for supplier bounded context.
 */
public class SupplierAggregate1 {
    private final SupplierAggregate1Id id;
    private final String name;
    private final List<SupplierItem1> items;
    private SupplierStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public SupplierAggregate1(SupplierAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("SupplierAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = SupplierStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(SupplierItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != SupplierStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = SupplierStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != SupplierStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = SupplierStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public SupplierAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<SupplierItem1> getItems() { return Collections.unmodifiableList(items); }
    public SupplierStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
