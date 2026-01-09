package com.example.enterprise.marketing.domain.model;

/**
 * Value Object representing a code in marketing context.
 */
public record MarketingCode(String value) {
    public MarketingCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Code cannot exceed 50 characters");
        }
    }
}
