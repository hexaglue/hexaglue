package com.example.enterprise.shipping.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for shipping bounded context.
 */
public class ShippingAggregate2 {
    private final ShippingAggregate2Id id;
    private final String name;
    private final List<ShippingItem2> items;
    private ShippingStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public ShippingAggregate2(ShippingAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("ShippingAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = ShippingStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(ShippingItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != ShippingStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = ShippingStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != ShippingStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = ShippingStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public ShippingAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<ShippingItem2> getItems() { return Collections.unmodifiableList(items); }
    public ShippingStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
