package com.example.ecommerce.port.driving;

/**
 * Command for registering a new customer.
 */
public record RegisterCustomerCommand(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    AddressDto shippingAddress
) {
    public record AddressDto(String street, String city, String postalCode, String country) {
    }
}
