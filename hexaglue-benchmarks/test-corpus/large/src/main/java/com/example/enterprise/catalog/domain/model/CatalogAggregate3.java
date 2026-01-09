package com.example.enterprise.catalog.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for catalog bounded context.
 */
public class CatalogAggregate3 {
    private final CatalogAggregate3Id id;
    private final String name;
    private final List<CatalogItem3> items;
    private CatalogStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CatalogAggregate3(CatalogAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("CatalogAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = CatalogStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(CatalogItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != CatalogStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = CatalogStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != CatalogStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = CatalogStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public CatalogAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<CatalogItem3> getItems() { return Collections.unmodifiableList(items); }
    public CatalogStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
