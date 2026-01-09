package com.example.enterprise.customer.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in CustomerAggregate2.
 */
public class CustomerItem2 {
    private final CustomerItem2Id id;
    private final String description;
    private CustomerAmount2 amount;
    private final Instant createdAt;

    public CustomerItem2(CustomerItem2Id id, String description, CustomerAmount2 amount) {
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

    public void updateAmount(CustomerAmount2 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public CustomerItem2Id getId() { return id; }
    public String getDescription() { return description; }
    public CustomerAmount2 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
