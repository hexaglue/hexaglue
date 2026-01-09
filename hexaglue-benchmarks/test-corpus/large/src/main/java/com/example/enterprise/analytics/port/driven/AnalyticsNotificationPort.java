package com.example.enterprise.analytics.port.driven;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;

/**
 * Driven port for analytics notifications.
 */
public interface AnalyticsNotificationPort {
    void sendCreatedNotification(AnalyticsAggregate1Id id, String name);
    void sendUpdatedNotification(AnalyticsAggregate1Id id);
    void sendCompletedNotification(AnalyticsAggregate1Id id);
}
