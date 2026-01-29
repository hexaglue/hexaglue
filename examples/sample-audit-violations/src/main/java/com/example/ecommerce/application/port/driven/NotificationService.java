package com.example.ecommerce.application.port.driven;

import com.example.ecommerce.domain.customer.Email;

/**
 * Driven port defining the contract for sending notifications to customers.
 *
 * <p>This service interface abstracts multi-channel notification delivery,
 * supporting email, SMS, and push notification channels. It also supports
 * scheduled email delivery for timed campaigns and delayed notifications.
 *
 * <p>AUDIT VIOLATION: hex:port-coverage.
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
