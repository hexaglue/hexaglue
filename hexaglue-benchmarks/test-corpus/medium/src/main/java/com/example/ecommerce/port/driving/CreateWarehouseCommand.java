package com.example.ecommerce.port.driving;

/**
 * Command for creating Warehouse.
 */
public record CreateWarehouseCommand(
    String name,
    String description
) {
}
