package com.example.enterprise.catalog.port.driven;

import com.example.enterprise.catalog.domain.model.CatalogAggregate1Id;

/**
 * Driven port for catalog notifications.
 */
public interface CatalogNotificationPort {
    void sendCreatedNotification(CatalogAggregate1Id id, String name);
    void sendUpdatedNotification(CatalogAggregate1Id id);
    void sendCompletedNotification(CatalogAggregate1Id id);
}
