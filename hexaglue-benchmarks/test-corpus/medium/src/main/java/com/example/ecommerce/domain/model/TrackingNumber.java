package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a shipment tracking number.
 */
public record TrackingNumber(String value) {
    public TrackingNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tracking number cannot be null or blank");
        }
    }

    public static TrackingNumber generate() {
        return new TrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
