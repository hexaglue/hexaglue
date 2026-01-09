package com.example.ecommerce.port.driving;

/**
 * Command for creating Return.
 */
public record CreateReturnCommand(
    String name,
    String description
) {
}
