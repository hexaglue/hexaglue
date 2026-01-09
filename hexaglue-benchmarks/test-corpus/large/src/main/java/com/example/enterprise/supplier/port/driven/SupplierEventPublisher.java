package com.example.enterprise.supplier.port.driven;

import com.example.enterprise.supplier.domain.event.SupplierEvent;

/**
 * Driven port for publishing supplier domain events.
 */
public interface SupplierEventPublisher {
    void publish(SupplierEvent event);
    void publishAsync(SupplierEvent event);
}
