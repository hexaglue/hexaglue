package io.hexaglue.arch.integration.fixtures.domain;

/**
 * Value object representing a shipping address.
 */
@ValueObject
public record Address(String street, String city, String zipCode, String country) {

    public String formatted() {
        return String.format("%s, %s %s, %s", street, zipCode, city, country);
    }
}
