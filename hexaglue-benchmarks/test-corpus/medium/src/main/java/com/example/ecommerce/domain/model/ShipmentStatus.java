package com.example.ecommerce.domain.model;

/**
 * Value Object representing a shipment status.
 */
public record ShipmentStatus(String value) {
    public ShipmentStatus {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ShipmentStatus cannot be null or blank");
        }
    }
}
