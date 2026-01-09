package com.example.enterprise.payment.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for payment bounded context.
 */
public class PaymentAggregate3 {
    private final PaymentAggregate3Id id;
    private final String name;
    private final List<PaymentItem3> items;
    private PaymentStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public PaymentAggregate3(PaymentAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("PaymentAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = PaymentStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(PaymentItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != PaymentStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = PaymentStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != PaymentStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = PaymentStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public PaymentAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<PaymentItem3> getItems() { return Collections.unmodifiableList(items); }
    public PaymentStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
