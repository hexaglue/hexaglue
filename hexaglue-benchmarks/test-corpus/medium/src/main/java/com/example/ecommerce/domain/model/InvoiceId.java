package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a InvoiceId identifier.
 */
public record InvoiceId(UUID value) {
    public InvoiceId {
        if (value == null) {
            throw new IllegalArgumentException("InvoiceId cannot be null");
        }
    }

    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
    }

    public static InvoiceId of(String value) {
        return new InvoiceId(UUID.fromString(value));
    }
}
