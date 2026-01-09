package com.example.enterprise.analytics.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for analytics bounded context.
 */
public class AnalyticsAggregate3 {
    private final AnalyticsAggregate3Id id;
    private final String name;
    private final List<AnalyticsItem3> items;
    private AnalyticsStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public AnalyticsAggregate3(AnalyticsAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("AnalyticsAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = AnalyticsStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(AnalyticsItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != AnalyticsStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = AnalyticsStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != AnalyticsStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = AnalyticsStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public AnalyticsAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<AnalyticsItem3> getItems() { return Collections.unmodifiableList(items); }
    public AnalyticsStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
