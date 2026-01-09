package com.example.ecommerce.port.driving;

/**
 * Command for updating Wishlist.
 */
public record UpdateWishlistCommand(
    String name,
    String description
) {
}
