package com.example.ecommerce.port.driven;

/**
 * Driven port (secondary) for publishing domain events.
 */
public interface EventPublisher {
    void publish(DomainEvent event);

    interface DomainEvent {
        String eventType();
    }
}
