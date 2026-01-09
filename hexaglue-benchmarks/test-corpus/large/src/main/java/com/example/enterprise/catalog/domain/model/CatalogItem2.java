package com.example.enterprise.catalog.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in CatalogAggregate2.
 */
public class CatalogItem2 {
    private final CatalogItem2Id id;
    private final String description;
    private CatalogAmount2 amount;
    private final Instant createdAt;

    public CatalogItem2(CatalogItem2Id id, String description, CatalogAmount2 amount) {
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

    public void updateAmount(CatalogAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public CatalogItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public CatalogAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
