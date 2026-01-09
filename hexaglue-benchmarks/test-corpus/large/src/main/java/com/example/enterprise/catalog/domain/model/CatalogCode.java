package com.example.enterprise.catalog.domain.model;

/**
 * Value Object representing a code in catalog context.
 */
public record CatalogCode(String value) {
    public CatalogCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
