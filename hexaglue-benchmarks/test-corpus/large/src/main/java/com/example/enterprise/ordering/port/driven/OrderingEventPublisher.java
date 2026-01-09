package com.example.enterprise.ordering.port.driven;

import com.example.enterprise.ordering.domain.event.OrderingEvent;

/**
 * Driven port for publishing ordering domain events.
 */
public interface OrderingEventPublisher {
    void publish(OrderingEvent event);
    void publishAsync(OrderingEvent event);
}
