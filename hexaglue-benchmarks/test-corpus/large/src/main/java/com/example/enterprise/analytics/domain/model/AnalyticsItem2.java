package com.example.enterprise.analytics.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in AnalyticsAggregate2.
 */
public class AnalyticsItem2 {
    private final AnalyticsItem2Id id;
    private final String description;
    private AnalyticsAmount2 amount;
    private final Instant createdAt;

    public AnalyticsItem2(AnalyticsItem2Id id, String description, AnalyticsAmount2 amount) {
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

    public void updateAmount(AnalyticsAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public AnalyticsItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public AnalyticsAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
