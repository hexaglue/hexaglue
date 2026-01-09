package com.example.ecommerce.port.driving;

import com.example.ecommerce.domain.model.WarehouseId;
import com.example.ecommerce.domain.model.Warehouse;
import java.util.List;

/**
 * Driving port (primary) for warehouse operations.
 */
public interface WarehouseService {
    WarehouseId create(CreateWarehouseCommand command);

    Warehouse getWarehouse(WarehouseId id);

    List<Warehouse> getAll();

    void update(WarehouseId id, UpdateWarehouseCommand command);

    void delete(WarehouseId id);
}
