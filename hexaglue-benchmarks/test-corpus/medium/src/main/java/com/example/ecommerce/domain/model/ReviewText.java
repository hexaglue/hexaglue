package com.example.ecommerce.domain.model;

/**
 * Value Object representing review text content.
 */
public record ReviewText(String value) {
    public ReviewText {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReviewText cannot be null or blank");
        }
    }
}
