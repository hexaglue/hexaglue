package com.example.ecommerce.port.driven;

import com.example.ecommerce.domain.model.Email;
import com.example.ecommerce.domain.model.OrderId;

/**
 * Driven port (secondary) for sending notifications.
 */
public interface NotificationService {
    void sendOrderConfirmation(OrderId orderId, Email customerEmail);

    void sendShippingNotification(OrderId orderId, Email customerEmail, String trackingNumber);

    void sendCancellationNotification(OrderId orderId, Email customerEmail);

    void sendWelcomeEmail(Email customerEmail);
}
