package com.example.enterprise.ordering.domain.model;

/**
 * Value Object representing a name in ordering context.
 */
public record OrderingName(String value) {
    public OrderingName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
