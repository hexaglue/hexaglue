package com.example.enterprise.marketing.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for marketing bounded context.
 */
public class MarketingAggregate1 {
    private final MarketingAggregate1Id id;
    private final String name;
    private final List<MarketingItem1> items;
    private MarketingStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public MarketingAggregate1(MarketingAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("MarketingAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = MarketingStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(MarketingItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != MarketingStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = MarketingStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != MarketingStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = MarketingStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public MarketingAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<MarketingItem1> getItems() { return Collections.unmodifiableList(items); }
    public MarketingStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
