package com.example.enterprise.shipping.port.driven;

import com.example.enterprise.shipping.domain.model.ShippingAggregate1Id;

/**
 * Driven port for shipping notifications.
 */
public interface ShippingNotificationPort {
    void sendCreatedNotification(ShippingAggregate1Id id, String name);
    void sendUpdatedNotification(ShippingAggregate1Id id);
    void sendCompletedNotification(ShippingAggregate1Id id);
}
