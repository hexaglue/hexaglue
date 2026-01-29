package com.example.ecommerce.domain.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;
import java.util.regex.Matcher;

/**
 * Value object representing a validated email address in the customer domain.
 *
 * <p>Encapsulates email validation logic including format verification via regex,
 * automatic lowercase normalization, and trimming of whitespace. Provides access
 * to the local part and domain components of the email address.
 *
 * <p>This value object enforces that only syntactically valid email addresses
 * can exist in the domain model, following the self-validating pattern.
 *
 * <p>AUDIT VIOLATION: ddd:domain-purity.
 * This domain value object imports Jakarta Validation annotations,
 * which is an infrastructure concern. Domain objects should be pure
 * and not depend on framework-specific annotations.
 */
public class Email {

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // VIOLATION: Using Jakarta Validation annotations in domain
    @NotBlank(message = "Email cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Invalid email format")
    private final String value;

    public Email(String value) {
        Objects.requireNonNull(value, "Email cannot be null");
        String trimmed = value.trim().toLowerCase();
        if (!isValidEmail(trimmed)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        this.value = trimmed;
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        int atIndex = value.indexOf('@');
        return value.substring(atIndex + 1);
    }

    public String getLocalPart() {
        int atIndex = value.indexOf('@');
        return value.substring(0, atIndex);
    }

    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
