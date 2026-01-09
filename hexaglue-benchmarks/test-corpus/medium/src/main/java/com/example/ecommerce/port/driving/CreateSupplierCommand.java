package com.example.ecommerce.port.driving;

/**
 * Command for creating Supplier.
 */
public record CreateSupplierCommand(
    String name,
    String description
) {
}
