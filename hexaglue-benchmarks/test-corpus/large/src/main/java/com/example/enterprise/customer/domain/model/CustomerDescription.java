package com.example.enterprise.customer.domain.model;

/**
 * Value Object representing a description in customer context.
 */
public record CustomerDescription(String value) {
    public CustomerDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static CustomerDescription empty() {
        return new CustomerDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
