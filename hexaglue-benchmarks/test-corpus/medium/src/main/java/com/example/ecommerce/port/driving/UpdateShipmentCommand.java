package com.example.ecommerce.port.driving;

/**
 * Command for updating Shipment.
 */
public record UpdateShipmentCommand(
    String name,
    String description
) {
}
