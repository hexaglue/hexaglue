package com.example.ecommerce.domain.model;

/**
 * Value Object representing a phone number.
 */
public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank");
        }
        // Simple validation - in production would use libphonenumber
        String cleaned = value.replaceAll("[^0-9+]", "");
        if (cleaned.length() < 10) {
            throw new IllegalArgumentException("Phone number too short: " + value);
        }
    }
}
