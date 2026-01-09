package com.example.enterprise.payment.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in PaymentAggregate1.
 */
public class PaymentItem1 {
    private final PaymentItem1Id id;
    private final String description;
    private PaymentAmount1 amount;
    private final Instant createdAt;

    public PaymentItem1(PaymentItem1Id id, String description, PaymentAmount1 amount) {
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

    public void updateAmount(PaymentAmount1 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public PaymentItem1Id getId() { return id; }
    public String getDescription() { return description; }
    public PaymentAmount1 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
