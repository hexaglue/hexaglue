package com.example.enterprise.analytics.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in AnalyticsAggregate3.
 */
public class AnalyticsItem3 {
    private final AnalyticsItem3Id id;
    private final String description;
    private AnalyticsAmount3 amount;
    private final Instant createdAt;

    public AnalyticsItem3(AnalyticsItem3Id id, String description, AnalyticsAmount3 amount) {
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

    public void updateAmount(AnalyticsAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public AnalyticsItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public AnalyticsAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
