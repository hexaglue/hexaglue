package com.example.enterprise.marketing.domain.model;

/**
 * Value Object representing a description in marketing context.
 */
public record MarketingDescription(String value) {
    public MarketingDescription {
        if (value != null && value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }

    public static MarketingDescription empty() {
        return new MarketingDescription("");
    }

    public boolean isEmpty() {
        return value == null || value.isBlank();
    }
}
