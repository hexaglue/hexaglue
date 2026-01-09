package com.example.ecommerce.port.driving;

/**
 * Command for creating Wishlist.
 */
public record CreateWishlistCommand(
    String name,
    String description
) {
}
