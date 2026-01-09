package com.example.ecommerce.domain.model;

/**
 * Value Object representing a physical address.
 */
public record Address(
    String street,
    String city,
    String postalCode,
    String country
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be null or blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be null or blank");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code cannot be null or blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be null or blank");
        }
    }

    public String getFullAddress() {
        return String.format("%s, %s %s, %s", street, postalCode, city, country);
    }
}
