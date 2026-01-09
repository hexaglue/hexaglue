package com.example.ecommerce.port.driving;

/**
 * Command for updating Invoice.
 */
public record UpdateInvoiceCommand(
    String name,
    String description
) {
}
