package com.example.enterprise.ordering.domain.model;

import java.util.UUID;

/**
 * Value Object representing a OrderingAggregate3 identifier.
 */
public record OrderingAggregate3Id(UUID value) {
    public OrderingAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("OrderingAggregate3Id cannot be null");
        }
    }

    public static OrderingAggregate3Id generate() {
        return new OrderingAggregate3Id(UUID.randomUUID());
    }

    public static OrderingAggregate3Id of(String value) {
        return new OrderingAggregate3Id(UUID.fromString(value));
    }
}
