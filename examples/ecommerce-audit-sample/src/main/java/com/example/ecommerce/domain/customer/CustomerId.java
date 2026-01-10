package com.example.ecommerce.domain.customer;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the identity of a Customer.
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
