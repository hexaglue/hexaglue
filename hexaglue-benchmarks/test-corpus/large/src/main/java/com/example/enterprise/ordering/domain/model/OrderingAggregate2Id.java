package com.example.enterprise.ordering.domain.model;

import java.util.UUID;

/**
 * Value Object representing a OrderingAggregate2 identifier.
 */
public record OrderingAggregate2Id(UUID value) {
    public OrderingAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("OrderingAggregate2Id cannot be null");
        }
    }

    public static OrderingAggregate2Id generate() {
        return new OrderingAggregate2Id(UUID.randomUUID());
    }

    public static OrderingAggregate2Id of(String value) {
        return new OrderingAggregate2Id(UUID.fromString(value));
    }
}
