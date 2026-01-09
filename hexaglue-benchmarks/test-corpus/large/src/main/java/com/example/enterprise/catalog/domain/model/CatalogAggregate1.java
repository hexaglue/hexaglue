package com.example.enterprise.catalog.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for catalog bounded context.
 */
public class CatalogAggregate1 {
    private final CatalogAggregate1Id id;
    private final String name;
    private final List<CatalogItem1> items;
    private CatalogStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CatalogAggregate1(CatalogAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("CatalogAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = CatalogStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(CatalogItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != CatalogStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = CatalogStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != CatalogStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = CatalogStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public CatalogAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<CatalogItem1> getItems() { return Collections.unmodifiableList(items); }
    public CatalogStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
