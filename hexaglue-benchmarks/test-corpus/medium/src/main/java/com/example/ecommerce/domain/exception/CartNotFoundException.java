package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.CartId;

/**
 * Exception thrown when Cart is not found.
 */
public class CartNotFoundException extends DomainException {
    private final CartId id;

    public CartNotFoundException(CartId id) {
        super("Cart not found: " + id);
        this.id = id;
    }

    public CartId getId() {
        return id;
    }
}
