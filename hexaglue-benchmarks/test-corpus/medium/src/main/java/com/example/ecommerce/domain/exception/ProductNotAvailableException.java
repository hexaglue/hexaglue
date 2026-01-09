package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.ProductId;

/**
 * Exception thrown when a product is not available in requested quantity.
 */
public class ProductNotAvailableException extends DomainException {
    private final ProductId productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public ProductNotAvailableException(ProductId productId, int requestedQuantity, int availableQuantity) {
        super(String.format("Product %s not available. Requested: %d, Available: %d",
            productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public ProductId getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
