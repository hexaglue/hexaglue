package com.ecommerce.domain.order;

/**
 * Address value object for shipping and billing.
 */
public record Address(
        String street,
        String city,
        String postalCode,
        String country) {

    public String formatted() {
        return String.format("%s, %s %s, %s", street, postalCode, city, country);
    }
}
