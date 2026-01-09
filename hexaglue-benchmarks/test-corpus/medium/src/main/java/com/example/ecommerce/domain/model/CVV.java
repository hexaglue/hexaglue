package com.example.ecommerce.domain.model;

/**
 * Value Object representing a card CVV.
 */
public record CVV(String value) {
    public CVV {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CVV cannot be null or blank");
        }
    }
}
