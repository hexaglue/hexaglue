package com.example.ecommerce.port.driving;

/**
 * Command for updating Return.
 */
public record UpdateReturnCommand(
    String name,
    String description
) {
}
