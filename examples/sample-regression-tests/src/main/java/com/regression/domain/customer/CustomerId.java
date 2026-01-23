package com.regression.domain.customer;

import java.util.UUID;

/**
 * Identifier for the Customer aggregate.
 * <p>
 * Tests C2/C4: When used in Order aggregate (cross-aggregate reference),
 * should be stored as UUID column, not as @Embedded.
 */
public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId value cannot be null");
        }
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
