package com.example.ecommerce.domain.customer;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier value object for the {@link Customer} aggregate root.
 *
 * <p>Wraps a UUID to provide a strongly-typed identity that prevents accidental
 * misuse of raw UUIDs across different aggregate boundaries. Supports generation
 * of new identifiers and reconstitution from string representations.
 */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "Customer ID cannot be null");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId from(String value) {
        return new CustomerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
