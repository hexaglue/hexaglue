package com.example.enterprise.shipping.port.driven;

import com.example.enterprise.shipping.domain.event.ShippingEvent;

/**
 * Driven port for publishing shipping domain events.
 */
public interface ShippingEventPublisher {
    void publish(ShippingEvent event);
    void publishAsync(ShippingEvent event);
}
