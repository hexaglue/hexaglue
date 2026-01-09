package com.example.enterprise.payment.port.driven;

import com.example.enterprise.payment.domain.event.PaymentEvent;

/**
 * Driven port for publishing payment domain events.
 */
public interface PaymentEventPublisher {
    void publish(PaymentEvent event);
    void publishAsync(PaymentEvent event);
}
