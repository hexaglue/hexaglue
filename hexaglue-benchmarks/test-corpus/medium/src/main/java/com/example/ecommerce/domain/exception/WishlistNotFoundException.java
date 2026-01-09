package com.example.ecommerce.domain.exception;

import com.example.ecommerce.domain.model.WishlistId;

/**
 * Exception thrown when Wishlist is not found.
 */
public class WishlistNotFoundException extends DomainException {
    private final WishlistId id;

    public WishlistNotFoundException(WishlistId id) {
        super("Wishlist not found: " + id);
        this.id = id;
    }

    public WishlistId getId() {
        return id;
    }
}
