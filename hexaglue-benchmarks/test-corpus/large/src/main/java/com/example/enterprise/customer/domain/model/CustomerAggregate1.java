package com.example.enterprise.customer.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for customer bounded context.
 */
public class CustomerAggregate1 {
    private final CustomerAggregate1Id id;
    private final String name;
    private final List<CustomerItem1> items;
    private CustomerStatus1 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CustomerAggregate1(CustomerAggregate1Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("CustomerAggregate1Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = CustomerStatus1.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(CustomerItem1 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != CustomerStatus1.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = CustomerStatus1.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != CustomerStatus1.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = CustomerStatus1.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public CustomerAggregate1Id getId() { return id; }
    public String getName() { return name; }
    public List<CustomerItem1> getItems() { return Collections.unmodifiableList(items); }
    public CustomerStatus1 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
