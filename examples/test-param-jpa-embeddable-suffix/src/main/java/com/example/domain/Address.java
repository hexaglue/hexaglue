package com.example.domain;

/** Shipping address value object (multi-field, generates embeddable). */
public record Address(String street, String city, String zipCode) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
    }
}
