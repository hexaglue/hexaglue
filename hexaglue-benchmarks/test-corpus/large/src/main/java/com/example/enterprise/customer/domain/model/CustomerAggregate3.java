package com.example.enterprise.customer.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root for customer bounded context.
 */
public class CustomerAggregate3 {
    private final CustomerAggregate3Id id;
    private final String name;
    private final List<CustomerItem3> items;
    private CustomerStatus3 status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CustomerAggregate3(CustomerAggregate3Id id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("CustomerAggregate3Id cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.status = CustomerStatus3.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(CustomerItem3 item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        this.items.add(item);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status != CustomerStatus3.PENDING) {
            throw new IllegalStateException("Can only activate pending items");
        }
        this.status = CustomerStatus3.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != CustomerStatus3.ACTIVE) {
            throw new IllegalStateException("Can only complete active items");
        }
        this.status = CustomerStatus3.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public CustomerAggregate3Id getId() { return id; }
    public String getName() { return name; }
    public List<CustomerItem3> getItems() { return Collections.unmodifiableList(items); }
    public CustomerStatus3 getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
