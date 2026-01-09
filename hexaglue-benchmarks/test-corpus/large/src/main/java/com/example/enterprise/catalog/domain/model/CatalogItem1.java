package com.example.enterprise.catalog.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in CatalogAggregate1.
 */
public class CatalogItem1 {
    private final CatalogItem1Id id;
    private final String description;
    private CatalogAmount1 amount;
    private final Instant createdAt;

    public CatalogItem1(CatalogItem1Id id, String description, CatalogAmount1 amount) {
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

    public void updateAmount(CatalogAmount1 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public CatalogItem1Id getId() { return id; }
    public String getDescription() { return description; }
    public CatalogAmount1 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
