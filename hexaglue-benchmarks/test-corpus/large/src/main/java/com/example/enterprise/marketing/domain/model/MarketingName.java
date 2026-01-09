package com.example.enterprise.marketing.domain.model;

/**
 * Value Object representing a name in marketing context.
 */
public record MarketingName(String value) {
    public MarketingName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("Name cannot exceed 200 characters");
        }
    }
}
