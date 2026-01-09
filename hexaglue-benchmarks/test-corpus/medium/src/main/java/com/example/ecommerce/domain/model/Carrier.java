package com.example.ecommerce.domain.model;

/**
 * Value Object representing a shipping carrier.
 */
public record Carrier(String value) {
    public Carrier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Carrier cannot be null or blank");
        }
    }
}
