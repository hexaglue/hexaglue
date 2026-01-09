package com.example.enterprise.inventory.port.driving;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1;
import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for InventoryAggregate1 operations.
 */
public interface InventoryAggregate1Service {
    InventoryAggregate1Id create(CreateInventoryAggregate1Command command);
    InventoryAggregate1 get(InventoryAggregate1Id id);
    List<InventoryAggregate1> list();
    void update(UpdateInventoryAggregate1Command command);
    void delete(InventoryAggregate1Id id);
    void activate(InventoryAggregate1Id id);
    void complete(InventoryAggregate1Id id);
}
