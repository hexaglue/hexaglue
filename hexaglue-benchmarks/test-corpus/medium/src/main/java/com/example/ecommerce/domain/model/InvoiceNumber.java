package com.example.ecommerce.domain.model;

/**
 * Value Object representing an invoice number.
 */
public record InvoiceNumber(String value) {
    public InvoiceNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("InvoiceNumber cannot be null or blank");
        }
    }
}
