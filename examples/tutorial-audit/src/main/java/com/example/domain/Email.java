package com.example.domain;

/**
 * Email address value object.
 * Immutable - validates on construction.
 */
public record Email(String value) {

    public Email {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }
}
