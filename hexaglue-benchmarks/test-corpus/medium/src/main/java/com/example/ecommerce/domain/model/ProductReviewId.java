package com.example.ecommerce.domain.model;

import java.util.UUID;

/**
 * Value Object representing a ProductReview identifier.
 */
public record ProductReviewId(UUID value) {
    public ProductReviewId {
        if (value == null) {
            throw new IllegalArgumentException("ProductReviewId cannot be null");
        }
    }

    public static ProductReviewId generate() {
        return new ProductReviewId(UUID.randomUUID());
    }

    public static ProductReviewId of(String value) {
        return new ProductReviewId(UUID.fromString(value));
    }
}
