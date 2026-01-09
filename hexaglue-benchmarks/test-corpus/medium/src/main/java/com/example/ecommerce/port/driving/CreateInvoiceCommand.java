package com.example.ecommerce.port.driving;

/**
 * Command for creating Invoice.
 */
public record CreateInvoiceCommand(
    String name,
    String description
) {
}
