package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a Shipment identifier.
 */
public record ShipmentId(UUID value) {
    public ShipmentId {
        if (value == null) {
            throw new IllegalArgumentException("ShipmentId cannot be null");
        }
    }

    public static ShipmentId generate() {
        return new ShipmentId(UUID.randomUUID());
    }

    public static ShipmentId of(String value) {
        return new ShipmentId(UUID.fromString(value));
    }
}
