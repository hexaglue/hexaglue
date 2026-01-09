package com.example.enterprise.customer.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CustomerAggregate2 identifier.
 */
public record CustomerAggregate2Id(UUID value) {
    public CustomerAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("CustomerAggregate2Id cannot be null");
        }
    }

    public static CustomerAggregate2Id generate() {
        return new CustomerAggregate2Id(UUID.randomUUID());
    }

    public static CustomerAggregate2Id of(String value) {
        return new CustomerAggregate2Id(UUID.fromString(value));
    }
}
