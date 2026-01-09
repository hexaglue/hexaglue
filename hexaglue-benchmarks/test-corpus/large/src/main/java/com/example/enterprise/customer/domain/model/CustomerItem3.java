package com.example.enterprise.customer.domain.model;

import java.time.Instant;

/**
 * Entity representing an item in CustomerAggregate3.
 */
public class CustomerItem3 {
    private final CustomerItem3Id id;
    private final String description;
    private CustomerAmount3 amount;
    private final Instant createdAt;

    public CustomerItem3(CustomerItem3Id id, String description, CustomerAmount3 amount) {
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

    public void updateAmount(CustomerAmount3 newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = newAmount;
    }

    public CustomerItem3Id getId() { return id; }
    public String getDescription() { return description; }
    public CustomerAmount3 getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
