package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Shipment;
import com.example.ecommerce.domain.model.ShipmentId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Shipment persistence.
 */
public interface ShipmentRepository {
    Shipment save(Shipment entity);

    Optional<Shipment> findById(ShipmentId id);

    List<Shipment> findAll();

    void deleteById(ShipmentId id);

    boolean existsById(ShipmentId id);
}
