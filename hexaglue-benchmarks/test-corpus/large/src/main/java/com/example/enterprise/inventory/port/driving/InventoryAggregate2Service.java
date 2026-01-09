package com.example.enterprise.inventory.port.driving;

import com.example.enterprise.inventory.domain.model.InventoryAggregate2;
import com.example.enterprise.inventory.domain.model.InventoryAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for InventoryAggregate2 operations.
 */
public interface InventoryAggregate2Service {
    InventoryAggregate2Id create(CreateInventoryAggregate2Command command);
    InventoryAggregate2 get(InventoryAggregate2Id id);
    List<InventoryAggregate2> list();
    void update(UpdateInventoryAggregate2Command command);
    void delete(InventoryAggregate2Id id);
    void activate(InventoryAggregate2Id id);
    void complete(InventoryAggregate2Id id);
}
