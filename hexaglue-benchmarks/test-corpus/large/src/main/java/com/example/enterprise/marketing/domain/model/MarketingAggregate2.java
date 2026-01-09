package com.example.enterprise.marketing.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for marketing bounded context.
 */
public class MarketingAggregate2 {
    private final MarketingAggregate2Id id;
    private final String name;
    private final List<MarketingItem2> items;
    private MarketingStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public MarketingAggregate2(MarketingAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("MarketingAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = MarketingStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(MarketingItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != MarketingStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = MarketingStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != MarketingStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = MarketingStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public MarketingAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<MarketingItem2> getItems() { return Collections.unmodifiableList(items); }
    public MarketingStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
