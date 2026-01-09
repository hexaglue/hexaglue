package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.ProductId;

/**
 * Exception thrown when a product is not found.
 */
public class ProductNotFoundException extends DomainException {
    private final ProductId productId;

    public ProductNotFoundException(ProductId productId) {
        super("Product not found: " + productId);
        this.productId = productId;
    }

    public ProductId getProductId() {
        return productId;
    }
}
