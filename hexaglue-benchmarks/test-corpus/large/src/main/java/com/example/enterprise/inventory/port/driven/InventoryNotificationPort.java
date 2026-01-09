package com.example.enterprise.inventory.port.driven;

import com.example.enterprise.inventory.domain.model.InventoryAggregate1Id;

/**
 * Driven port for inventory notifications.
 */
public interface InventoryNotificationPort {
    void sendCreatedNotification(InventoryAggregate1Id id, String name);
    void sendUpdatedNotification(InventoryAggregate1Id id);
    void sendCompletedNotification(InventoryAggregate1Id id);
}
