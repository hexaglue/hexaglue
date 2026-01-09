package com.example.enterprise.inventory.port.driving;

import com.example.enterprise.inventory.domain.model.InventoryAggregate3;
import com.example.enterprise.inventory.domain.model.InventoryAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for InventoryAggregate3 operations.
 */
public interface InventoryAggregate3Service {
    InventoryAggregate3Id create(CreateInventoryAggregate3Command command);
    InventoryAggregate3 get(InventoryAggregate3Id id);
    List<InventoryAggregate3> list();
    void update(UpdateInventoryAggregate3Command command);
    void delete(InventoryAggregate3Id id);
    void activate(InventoryAggregate3Id id);
    void complete(InventoryAggregate3Id id);
}
