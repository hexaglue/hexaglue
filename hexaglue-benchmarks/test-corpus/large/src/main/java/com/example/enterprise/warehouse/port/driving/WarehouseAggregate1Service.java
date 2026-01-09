package com.example.enterprise.warehouse.port.driving;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for WarehouseAggregate1 operations.
 */
public interface WarehouseAggregate1Service {
    WarehouseAggregate1Id create(CreateWarehouseAggregate1Command command);
    WarehouseAggregate1 get(WarehouseAggregate1Id id);
    List<WarehouseAggregate1> list();
    void update(UpdateWarehouseAggregate1Command command);
    void delete(WarehouseAggregate1Id id);
    void activate(WarehouseAggregate1Id id);
    void complete(WarehouseAggregate1Id id);
}
