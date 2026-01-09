package com.example.enterprise.ordering.domain.model;

/**
 * Value Object representing a description in ordering context.
 */
public record OrderingDescription(String value) {
    public OrderingDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static OrderingDescription empty() {
        return new OrderingDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
