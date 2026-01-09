package com.example.ecommerce.port.driving;

/**
 * Command for creating Cart.
 */
public record CreateCartCommand(
    String name,
    String description
) {
}
