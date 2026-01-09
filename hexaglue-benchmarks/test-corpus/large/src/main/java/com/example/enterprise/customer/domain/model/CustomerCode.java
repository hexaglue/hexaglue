package com.example.enterprise.customer.domain.model;

/**
 * Value Object representing a code in customer context.
 */
public record CustomerCode(String value) {
    public CustomerCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
