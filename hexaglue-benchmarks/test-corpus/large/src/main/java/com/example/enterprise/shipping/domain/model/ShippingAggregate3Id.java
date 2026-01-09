package com.example.enterprise.shipping.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ShippingAggregate3 identifier.
 */
public record ShippingAggregate3Id(UUID value) {
    public ShippingAggregate3Id {
        if (value == null) {
            throw new IllegalArgumentException("ShippingAggregate3Id cannot be null");
        }
    }

    public static ShippingAggregate3Id generate() {
        return new ShippingAggregate3Id(UUID.randomUUID());
    }

    public static ShippingAggregate3Id of(String value) {
        return new ShippingAggregate3Id(UUID.fromString(value));
    }
}
