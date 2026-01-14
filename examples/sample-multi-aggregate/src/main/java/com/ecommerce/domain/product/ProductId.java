package com.ecommerce.domain.product;

import java.util.UUID;

/**
 * Product identifier.
 */
public record ProductId(UUID value) {
    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }
}
