package com.example.enterprise.warehouse.port.driving;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for WarehouseAggregate3 operations.
 */
public interface WarehouseAggregate3Service {
    WarehouseAggregate3Id create(CreateWarehouseAggregate3Command command);
    WarehouseAggregate3 get(WarehouseAggregate3Id id);
    List<WarehouseAggregate3> list();
    void update(UpdateWarehouseAggregate3Command command);
    void delete(WarehouseAggregate3Id id);
    void activate(WarehouseAggregate3Id id);
    void complete(WarehouseAggregate3Id id);
}
