package com.example.enterprise.supplier.domain.model;

/**
 * Value Object representing a name in supplier context.
 */
public record SupplierName(String value) {
    public SupplierName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
