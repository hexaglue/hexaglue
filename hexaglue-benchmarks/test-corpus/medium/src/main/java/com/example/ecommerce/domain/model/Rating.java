package com.example.ecommerce.domain.model;

/**
 * Value Object representing a product rating.
 */
public record Rating(String value) {
    public Rating {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rating cannot be null or blank");
        }
    }
}
