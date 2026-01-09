package com.example.enterprise.warehouse.port.driven;

import com.example.enterprise.warehouse.domain.model.WarehouseAggregate1Id;

/**
 * Driven port for warehouse notifications.
 */
public interface WarehouseNotificationPort {
    void sendCreatedNotification(WarehouseAggregate1Id id, String name);
    void sendUpdatedNotification(WarehouseAggregate1Id id);
    void sendCompletedNotification(WarehouseAggregate1Id id);
}
