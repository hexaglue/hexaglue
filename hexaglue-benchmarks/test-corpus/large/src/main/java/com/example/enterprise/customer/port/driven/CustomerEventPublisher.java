package com.example.enterprise.customer.port.driven;

import com.example.enterprise.customer.domain.event.CustomerEvent;

/**
 * Driven port for publishing customer domain events.
 */
public interface CustomerEventPublisher {
    void publish(CustomerEvent event);
    void publishAsync(CustomerEvent event);
}
