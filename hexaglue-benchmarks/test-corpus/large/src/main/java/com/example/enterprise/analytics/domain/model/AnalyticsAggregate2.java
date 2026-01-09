package com.example.enterprise.analytics.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for analytics bounded context.
 */
public class AnalyticsAggregate2 {
    private final AnalyticsAggregate2Id id;
    private final String name;
    private final List<AnalyticsItem2> items;
    private AnalyticsStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public AnalyticsAggregate2(AnalyticsAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("AnalyticsAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = AnalyticsStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(AnalyticsItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != AnalyticsStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = AnalyticsStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != AnalyticsStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = AnalyticsStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public AnalyticsAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<AnalyticsItem2> getItems() { return Collections.unmodifiableList(items); }
    public AnalyticsStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
