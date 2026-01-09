package com.example.ecommerce.domain.model;

import java.util.regex.Pattern;

/**
 * Value Object representing a Stock Keeping Unit identifier.
 */
public record SKU(String value) {
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z0-9]{8,12}$");

    public SKU {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (!SKU_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SKU format: " + value);
        }
    }
}
