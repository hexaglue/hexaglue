package com.example.ecommerce.port.driving;

/**
 * Command for updating customer's default shipping address.
 */
public record UpdateShippingAddressCommand(
    String street,
    String city,
    String postalCode,
    String country
) {
}
