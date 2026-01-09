package com.example.enterprise.ordering.domain.model;

/**
 * Value Object representing a code in ordering context.
 */
public record OrderingCode(String value) {
    public OrderingCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
