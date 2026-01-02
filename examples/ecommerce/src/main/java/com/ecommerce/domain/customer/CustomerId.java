package com.ecommerce.domain.customer;

import java.util.UUID;

/**
 * Customer identifier.
 */
public record CustomerId(UUID value) {
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }
}
