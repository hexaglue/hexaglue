package com.example.enterprise.customer.domain.model;

/**
 * Value Object representing a name in customer context.
 */
public record CustomerName(String value) {
    public CustomerName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
