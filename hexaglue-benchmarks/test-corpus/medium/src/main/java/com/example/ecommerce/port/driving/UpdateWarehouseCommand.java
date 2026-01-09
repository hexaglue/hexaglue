package com.example.ecommerce.port.driving;

/**
 * Command for updating Warehouse.
 */
public record UpdateWarehouseCommand(
    String name,
    String description
) {
}
