package com.example.ecommerce.port.driving;

/**
 * Command for updating Cart.
 */
public record UpdateCartCommand(
    String name,
    String description
) {
}
