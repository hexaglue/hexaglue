package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.ProductReviewId;

/**
 * Exception thrown when ProductReview is not found.
 */
public class ProductReviewNotFoundException extends DomainException {
    private final ProductReviewId id;

    public ProductReviewNotFoundException(ProductReviewId id) {
        super("ProductReview not found: " + id);
        this.id = id;
    }

    public ProductReviewId getId() {
        return id;
    }
}
