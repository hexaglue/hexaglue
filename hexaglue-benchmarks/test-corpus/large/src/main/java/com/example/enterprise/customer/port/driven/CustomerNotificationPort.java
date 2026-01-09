package com.example.enterprise.customer.port.driven;

import com.example.enterprise.customer.domain.model.CustomerAggregate1Id;

/**
 * Driven port for customer notifications.
 */
public interface CustomerNotificationPort {
    void sendCreatedNotification(CustomerAggregate1Id id, String name);
    void sendUpdatedNotification(CustomerAggregate1Id id);
    void sendCompletedNotification(CustomerAggregate1Id id);
}
