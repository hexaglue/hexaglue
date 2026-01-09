package com.example.enterprise.catalog.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in CatalogAggregate3.
 */
public class CatalogItem3 {
    private final CatalogItem3Id id;
    private final String description;
    private CatalogAmount3 amount;
    private final Instant createdAt;

    public CatalogItem3(CatalogItem3Id id, String description, CatalogAmount3 amount) {
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

    public void updateAmount(CatalogAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public CatalogItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public CatalogAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
