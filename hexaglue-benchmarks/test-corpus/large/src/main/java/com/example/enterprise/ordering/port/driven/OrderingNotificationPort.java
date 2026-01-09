package com.example.enterprise.ordering.port.driven;

import com.example.enterprise.ordering.domain.model.OrderingAggregate1Id;

/**
 * Driven port for ordering notifications.
 */
public interface OrderingNotificationPort {
    void sendCreatedNotification(OrderingAggregate1Id id, String name);
    void sendUpdatedNotification(OrderingAggregate1Id id);
    void sendCompletedNotification(OrderingAggregate1Id id);
}
