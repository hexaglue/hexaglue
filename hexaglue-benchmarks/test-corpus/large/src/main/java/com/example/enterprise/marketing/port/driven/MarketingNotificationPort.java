package com.example.enterprise.marketing.port.driven;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;

/**
 * Driven port for marketing notifications.
 */
public interface MarketingNotificationPort {
    void sendCreatedNotification(MarketingAggregate1Id id, String name);
    void sendUpdatedNotification(MarketingAggregate1Id id);
    void sendCompletedNotification(MarketingAggregate1Id id);
}
