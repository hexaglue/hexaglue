package com.example.ecommerce.port.driving;

/**
 * Command for updating customer contact information.
 */
public record UpdateContactInfoCommand(
    String email,
    String phoneNumber
) {
}
