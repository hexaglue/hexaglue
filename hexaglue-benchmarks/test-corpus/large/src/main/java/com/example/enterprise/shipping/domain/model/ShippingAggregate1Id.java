package com.example.enterprise.shipping.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ShippingAggregate1 identifier.
 */
public record ShippingAggregate1Id(UUID value) {
    public ShippingAggregate1Id {
        if (value == null) {
            throw new IllegalArgumentException("ShippingAggregate1Id cannot be null");
        }
    }

    public static ShippingAggregate1Id generate() {
        return new ShippingAggregate1Id(UUID.randomUUID());
    }

    public static ShippingAggregate1Id of(String value) {
        return new ShippingAggregate1Id(UUID.fromString(value));
    }
}
