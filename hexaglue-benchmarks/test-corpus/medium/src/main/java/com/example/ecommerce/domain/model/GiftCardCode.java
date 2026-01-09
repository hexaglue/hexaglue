package com.example.ecommerce.domain.model;

/**
 * Value Object representing Gift card code.
 */
public record GiftCardCode(String value) {
    public GiftCardCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GiftCardCode cannot be null or blank");
        }
    }
}
