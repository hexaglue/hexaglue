package com.example.enterprise.supplier.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for supplier bounded context.
 */
public class SupplierAggregate3 {
    private final SupplierAggregate3Id id;
    private final String name;
    private final List<SupplierItem3> items;
    private SupplierStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public SupplierAggregate3(SupplierAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("SupplierAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = SupplierStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(SupplierItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != SupplierStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = SupplierStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != SupplierStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = SupplierStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public SupplierAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<SupplierItem3> getItems() { return Collections.unmodifiableList(items); }
    public SupplierStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
