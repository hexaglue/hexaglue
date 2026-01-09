package com.example.ecommerce.domain.model;

/**
 * Value Object representing a country code (ISO 3166-1 alpha-2).
 */
public record Country(String code) {
    public Country {
        if (code == null || code.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2 characters (ISO 3166-1 alpha-2)");
        }
    }

    public static Country FR = new Country("FR");
    public static Country US = new Country("US");
    public static Country DE = new Country("DE");
    public static Country GB = new Country("GB");
    public static Country IT = new Country("IT");
    public static Country ES = new Country("ES");
}
