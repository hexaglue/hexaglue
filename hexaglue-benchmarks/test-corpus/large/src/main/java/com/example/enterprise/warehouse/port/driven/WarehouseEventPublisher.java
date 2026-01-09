package com.example.enterprise.warehouse.port.driven;

import com.example.enterprise.warehouse.domain.event.WarehouseEvent;

/**
 * Driven port for publishing warehouse domain events.
 */
public interface WarehouseEventPublisher {
    void publish(WarehouseEvent event);
    void publishAsync(WarehouseEvent event);
}
