package com.example.enterprise.inventory.port.driven;

import com.example.enterprise.inventory.domain.event.InventoryEvent;

/**
 * Driven port for publishing inventory domain events.
 */
public interface InventoryEventPublisher {
    void publish(InventoryEvent event);
    void publishAsync(InventoryEvent event);
}
