package com.example.enterprise.payment.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for payment bounded context.
 */
public class PaymentAggregate1 {
    private final PaymentAggregate1Id id;
    private final String name;
    private final List<PaymentItem1> items;
    private PaymentStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentAggregate1(PaymentAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("PaymentAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = PaymentStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(PaymentItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != PaymentStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = PaymentStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != PaymentStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = PaymentStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public PaymentAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<PaymentItem1> getItems() { return Collections.unmodifiableList(items); }
    public PaymentStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
