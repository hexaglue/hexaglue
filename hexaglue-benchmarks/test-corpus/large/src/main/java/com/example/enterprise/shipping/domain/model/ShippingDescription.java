package com.example.enterprise.shipping.domain.model;

/**
 * Value Object representing a description in shipping context.
 */
public record ShippingDescription(String value) {
    public ShippingDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static ShippingDescription empty() {
        return new ShippingDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
