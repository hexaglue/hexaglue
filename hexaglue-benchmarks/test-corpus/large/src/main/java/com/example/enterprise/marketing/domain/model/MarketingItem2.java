package com.example.enterprise.marketing.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in MarketingAggregate2.
 */
public class MarketingItem2 {
    private final MarketingItem2Id id;
    private final String description;
    private MarketingAmount2 amount;
    private final Instant createdAt;

    public MarketingItem2(MarketingItem2Id id, String description, MarketingAmount2 amount) {
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

    public void updateAmount(MarketingAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public MarketingItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public MarketingAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
