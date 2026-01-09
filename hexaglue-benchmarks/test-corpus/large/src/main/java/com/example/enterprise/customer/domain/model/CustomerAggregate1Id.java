package com.example.enterprise.customer.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CustomerAggregate1 identifier.
 */
public record CustomerAggregate1Id(UUID value) {
    public CustomerAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("CustomerAggregate1Id cannot be null");
        }
    }

    public static CustomerAggregate1Id generate() {
        return new CustomerAggregate1Id(UUID.randomUUID());
    }

    public static CustomerAggregate1Id of(String value) {
        return new CustomerAggregate1Id(UUID.fromString(value));
    }
}
