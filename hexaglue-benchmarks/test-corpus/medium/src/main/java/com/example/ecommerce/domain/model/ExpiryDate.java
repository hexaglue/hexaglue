package com.example.ecommerce.domain.model;

/**
 * Value Object representing a card expiry date.
 */
public record ExpiryDate(String value) {
    public ExpiryDate {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ExpiryDate cannot be null or blank");
        }
    }
}
