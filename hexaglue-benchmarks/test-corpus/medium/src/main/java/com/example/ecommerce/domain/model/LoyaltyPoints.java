package com.example.ecommerce.domain.model;

/**
 * Value Object representing Loyalty program points.
 */
public record LoyaltyPoints(String value) {
    public LoyaltyPoints {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LoyaltyPoints cannot be null or blank");
        }
    }
}
