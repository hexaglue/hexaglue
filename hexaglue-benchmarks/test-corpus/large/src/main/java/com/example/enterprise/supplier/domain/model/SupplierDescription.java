package com.example.enterprise.supplier.domain.model;

/**
 * Value Object representing a description in supplier context.
 */
public record SupplierDescription(String value) {
    public SupplierDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static SupplierDescription empty() {
        return new SupplierDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
