package com.example.enterprise.shipping.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for shipping bounded context.
 */
public class ShippingAggregate1 {
    private final ShippingAggregate1Id id;
    private final String name;
    private final List<ShippingItem1> items;
    private ShippingStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public ShippingAggregate1(ShippingAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("ShippingAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = ShippingStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(ShippingItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != ShippingStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = ShippingStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != ShippingStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = ShippingStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public ShippingAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<ShippingItem1> getItems() { return Collections.unmodifiableList(items); }
    public ShippingStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
