package com.example.enterprise.shipping.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ShippingAggregate2 identifier.
 */
public record ShippingAggregate2Id(UUID value) {
    public ShippingAggregate2Id {
        if (value == null) {
            throw new IllegalArgumentException("ShippingAggregate2Id cannot be null");
        }
    }

    public static ShippingAggregate2Id generate() {
        return new ShippingAggregate2Id(UUID.randomUUID());
    }

    public static ShippingAggregate2Id of(String value) {
        return new ShippingAggregate2Id(UUID.fromString(value));
    }
}
