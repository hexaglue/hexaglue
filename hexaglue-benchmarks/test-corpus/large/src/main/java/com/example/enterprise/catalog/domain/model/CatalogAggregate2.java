package com.example.enterprise.catalog.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for catalog bounded context.
 */
public class CatalogAggregate2 {
    private final CatalogAggregate2Id id;
    private final String name;
    private final List<CatalogItem2> items;
    private CatalogStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CatalogAggregate2(CatalogAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("CatalogAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = CatalogStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(CatalogItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != CatalogStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = CatalogStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != CatalogStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = CatalogStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public CatalogAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<CatalogItem2> getItems() { return Collections.unmodifiableList(items); }
    public CatalogStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
