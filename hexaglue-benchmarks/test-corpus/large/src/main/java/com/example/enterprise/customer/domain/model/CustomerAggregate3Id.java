package com.example.enterprise.customer.domain.model;

import java.util.UUID;

/**
 * Value Object representing a CustomerAggregate3 identifier.
 */
public record CustomerAggregate3Id(UUID value) {
    public CustomerAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("CustomerAggregate3Id cannot be null");
        }
    }

    public static CustomerAggregate3Id generate() {
        return new CustomerAggregate3Id(UUID.randomUUID());
    }

    public static CustomerAggregate3Id of(String value) {
        return new CustomerAggregate3Id(UUID.fromString(value));
    }
}
