package com.example.enterprise.warehouse.port.driving;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2;
import com.example.enterprise.warehouse.domain.model.WarehouseAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for WarehouseAggregate2 operations.
 */
public interface WarehouseAggregate2Service {
    WarehouseAggregate2Id create(CreateWarehouseAggregate2Command command);
    WarehouseAggregate2 get(WarehouseAggregate2Id id);
    List<WarehouseAggregate2> list();
    void update(UpdateWarehouseAggregate2Command command);
    void delete(WarehouseAggregate2Id id);
    void activate(WarehouseAggregate2Id id);
    void complete(WarehouseAggregate2Id id);
}
