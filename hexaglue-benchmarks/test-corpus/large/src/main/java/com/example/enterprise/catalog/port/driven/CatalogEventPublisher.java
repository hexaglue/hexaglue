package com.example.enterprise.catalog.port.driven;

import com.example.enterprise.catalog.domain.event.CatalogEvent;

/**
 * Driven port for publishing catalog domain events.
 */
public interface CatalogEventPublisher {
    void publish(CatalogEvent event);
    void publishAsync(CatalogEvent event);
}
