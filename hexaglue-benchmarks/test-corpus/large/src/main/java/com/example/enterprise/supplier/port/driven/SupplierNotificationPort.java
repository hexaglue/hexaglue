package com.example.enterprise.supplier.port.driven;

import com.example.enterprise.supplier.domain.model.SupplierAggregate1Id;

/**
 * Driven port for supplier notifications.
 */
public interface SupplierNotificationPort {
    void sendCreatedNotification(SupplierAggregate1Id id, String name);
    void sendUpdatedNotification(SupplierAggregate1Id id);
    void sendCompletedNotification(SupplierAggregate1Id id);
}
