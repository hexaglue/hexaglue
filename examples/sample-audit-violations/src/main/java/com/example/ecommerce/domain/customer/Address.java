package com.example.ecommerce.domain.customer;

import java.util.Objects;

/**
 * Value object representing a postal address used for billing and shipping.
 *
 * <p>An Address is an immutable record composed of street, city, postal code, and
 * country. It supports formatting as a full address string and country-based queries.
 * Wither methods allow creating modified copies without breaking immutability.
 *
 * <p>A Customer may have multiple addresses and designate one as the default
 * billing address and another as the default shipping address.
 */
public record Address(
        String street,
        String city,
        String postalCode,
        String country
) {

    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        if (street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
    }

    public String getFullAddress() {
        return String.format("%s, %s %s, %s", street, postalCode, city, country);
    }

    public boolean isInCountry(String countryCode) {
        return country.equalsIgnoreCase(countryCode);
    }

    public Address withStreet(String newStreet) {
        return new Address(newStreet, city, postalCode, country);
    }

    public Address withCity(String newCity) {
        return new Address(street, newCity, postalCode, country);
    }
}
