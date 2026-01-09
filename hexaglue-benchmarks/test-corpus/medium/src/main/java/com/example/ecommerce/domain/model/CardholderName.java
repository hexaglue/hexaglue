package com.example.ecommerce.domain.model;

/**
 * Value Object representing a cardholder name.
 */
public record CardholderName(String value) {
    public CardholderName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CardholderName cannot be null or blank");
        }
    }
}
