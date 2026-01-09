package com.example.enterprise.payment.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for payment bounded context.
 */
public class PaymentAggregate2 {
    private final PaymentAggregate2Id id;
    private final String name;
    private final List<PaymentItem2> items;
    private PaymentStatus2 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentAggregate2(PaymentAggregate2Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("PaymentAggregate2Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = PaymentStatus2.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(PaymentItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != PaymentStatus2.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = PaymentStatus2.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != PaymentStatus2.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = PaymentStatus2.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public PaymentAggregate2Id getId() { return id; }
    public String getName() { return name; }
    public List<PaymentItem2> getItems() { return Collections.unmodifiableList(items); }
    public PaymentStatus2 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
