package com.example.enterprise.payment.port.driven;

import com.example.enterprise.payment.domain.model.PaymentAggregate1Id;

/**
 * Driven port for payment notifications.
 */
public interface PaymentNotificationPort {
    void sendCreatedNotification(PaymentAggregate1Id id, String name);
    void sendUpdatedNotification(PaymentAggregate1Id id);
    void sendCompletedNotification(PaymentAggregate1Id id);
}
