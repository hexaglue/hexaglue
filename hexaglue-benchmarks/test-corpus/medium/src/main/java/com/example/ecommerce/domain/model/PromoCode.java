package com.example.ecommerce.domain.model;

/**
 * Value Object representing Promotional code.
 */
public record PromoCode(String value) {
    public PromoCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PromoCode cannot be null or blank");
        }
    }
}
