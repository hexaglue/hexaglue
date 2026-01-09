package com.example.enterprise.ordering.domain.model;

import java.util.UUID;

/**
 * Value Object representing a OrderingAggregate1 identifier.
 */
public record OrderingAggregate1Id(UUID value) {
    public OrderingAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("OrderingAggregate1Id cannot be null");
        }
    }

    public static OrderingAggregate1Id generate() {
        return new OrderingAggregate1Id(UUID.randomUUID());
    }

    public static OrderingAggregate1Id of(String value) {
        return new OrderingAggregate1Id(UUID.fromString(value));
    }
}
