package com.example.enterprise.shipping.domain.model;

/**
 * Value Object representing a code in shipping context.
 */
public record ShippingCode(String value) {
    public ShippingCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
