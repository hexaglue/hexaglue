package com.example.ecommerce.domain.model;

/**
 * Value Object representing a credit card number.
 */
public record CardNumber(String value) {
    public CardNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CardNumber cannot be null or blank");
        }
    }
}
