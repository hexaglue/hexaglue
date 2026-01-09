package com.example.enterprise.payment.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in PaymentAggregate2.
 */
public class PaymentItem2 {
    private final PaymentItem2Id id;
    private final String description;
    private PaymentAmount2 amount;
    private final Instant createdAt;

    public PaymentItem2(PaymentItem2Id id, String description, PaymentAmount2 amount) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public void updateAmount(PaymentAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public PaymentItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public PaymentAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
