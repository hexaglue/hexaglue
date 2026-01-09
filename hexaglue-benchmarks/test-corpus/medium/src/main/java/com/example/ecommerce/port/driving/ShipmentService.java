package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.ShipmentId;
import com.example.ecommerce.domain.model.Shipment;
import java.util.List;

/**
 * Driving port (primary) for shipment operations.
 */
public interface ShipmentService {
    ShipmentId create(CreateShipmentCommand command);

    Shipment getShipment(ShipmentId id);

    List<Shipment> getAll();

    void update(ShipmentId id, UpdateShipmentCommand command);

    void delete(ShipmentId id);
}
