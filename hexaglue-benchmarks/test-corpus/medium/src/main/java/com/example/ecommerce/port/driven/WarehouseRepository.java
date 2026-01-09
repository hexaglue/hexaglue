package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Warehouse;
import com.example.ecommerce.domain.model.WarehouseId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for Warehouse persistence.
 */
public interface WarehouseRepository {
    Warehouse save(Warehouse entity);

    Optional<Warehouse> findById(WarehouseId id);

    List<Warehouse> findAll();

    void deleteById(WarehouseId id);

    boolean existsById(WarehouseId id);
}
