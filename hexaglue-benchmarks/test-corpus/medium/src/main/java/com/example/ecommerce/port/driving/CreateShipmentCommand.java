package com.example.ecommerce.port.driving;

/**
 * Command for creating Shipment.
 */
public record CreateShipmentCommand(
    String name,
    String description
) {
}
