package com.example.ecommerce.port.driving;

/**
 * Command for updating Payment.
 */
public record UpdatePaymentCommand(
    String name,
    String description
) {
}
