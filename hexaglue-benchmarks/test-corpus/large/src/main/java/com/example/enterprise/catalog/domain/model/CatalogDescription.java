package com.example.enterprise.catalog.domain.model;

/**
 * Value Object representing a description in catalog context.
 */
public record CatalogDescription(String value) {
    public CatalogDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static CatalogDescription empty() {
        return new CatalogDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
