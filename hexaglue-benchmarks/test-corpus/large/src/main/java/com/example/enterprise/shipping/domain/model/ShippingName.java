package com.example.enterprise.shipping.domain.model;

/**
 * Value Object representing a name in shipping context.
 */
public record ShippingName(String value) {
    public ShippingName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
