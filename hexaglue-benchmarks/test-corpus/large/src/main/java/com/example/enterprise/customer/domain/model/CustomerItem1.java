package com.example.enterprise.customer.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in CustomerAggregate1.
 */
public class CustomerItem1 {
    private final CustomerItem1Id id;
    private final String description;
    private CustomerAmount1 amount;
    private final Instant createdAt;

    public CustomerItem1(CustomerItem1Id id, String description, CustomerAmount1 amount) {
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

    public void updateAmount(CustomerAmount1 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public CustomerItem1Id getId() { return id; }
    public String getDescription() { return description; }
    public CustomerAmount1 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
