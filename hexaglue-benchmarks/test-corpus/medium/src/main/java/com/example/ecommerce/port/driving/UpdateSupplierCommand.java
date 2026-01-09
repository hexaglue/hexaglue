package com.example.ecommerce.port.driving;

/**
 * Command for updating Supplier.
 */
public record UpdateSupplierCommand(
    String name,
    String description
) {
}
