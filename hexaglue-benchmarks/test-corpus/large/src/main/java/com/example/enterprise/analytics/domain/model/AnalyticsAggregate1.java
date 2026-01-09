package com.example.enterprise.analytics.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for analytics bounded context.
 */
public class AnalyticsAggregate1 {
    private final AnalyticsAggregate1Id id;
    private final String name;
    private final List<AnalyticsItem1> items;
    private AnalyticsStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public AnalyticsAggregate1(AnalyticsAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("AnalyticsAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = AnalyticsStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(AnalyticsItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != AnalyticsStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = AnalyticsStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != AnalyticsStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = AnalyticsStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public AnalyticsAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<AnalyticsItem1> getItems() { return Collections.unmodifiableList(items); }
    public AnalyticsStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
