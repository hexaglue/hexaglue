package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.customer.Email;

/**
 * Driven port for sending notifications.
 *
 * AUDIT VIOLATION: hex:port-coverage
 * This port does not have any adapter implementing it.
 * There is no NotificationAdapter in the infrastructure layer.
 */
public interface NotificationService {

    /**
     * Sends an email notification.
     */
    void sendEmail(Email to, String subject, String body);

    /**
     * Sends an SMS notification.
     */
    void sendSms(String phoneNumber, String message);

    /**
     * Sends a push notification.
     */
    void sendPushNotification(String userId, String title, String message);

    /**
     * Schedules an email for later delivery.
     */
    void scheduleEmail(Email to, String subject, String body, long delayMillis);
}
