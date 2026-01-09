package com.example.enterprise.shipping.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for shipping bounded context.
 */
public class ShippingAggregate3 {
    private final ShippingAggregate3Id id;
    private final String name;
    private final List<ShippingItem3> items;
    private ShippingStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public ShippingAggregate3(ShippingAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("ShippingAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = ShippingStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(ShippingItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != ShippingStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = ShippingStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != ShippingStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = ShippingStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public ShippingAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<ShippingItem3> getItems() { return Collections.unmodifiableList(items); }
    public ShippingStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
