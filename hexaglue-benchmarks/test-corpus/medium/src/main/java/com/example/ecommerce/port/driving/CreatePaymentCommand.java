package com.example.ecommerce.port.driving;

/**
 * Command for creating Payment.
 */
public record CreatePaymentCommand(
    String name,
    String description
) {
}
