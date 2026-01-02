package com.ecommerce.domain.customer;

import java.util.regex.Pattern;

/**
 * Email value object with validation.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }
}
