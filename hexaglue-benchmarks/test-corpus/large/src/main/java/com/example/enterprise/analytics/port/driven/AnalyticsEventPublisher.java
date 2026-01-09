package com.example.enterprise.analytics.port.driven;

import com.example.enterprise.analytics.domain.event.AnalyticsEvent;

/**
 * Driven port for publishing analytics domain events.
 */
public interface AnalyticsEventPublisher {
    void publish(AnalyticsEvent event);
    void publishAsync(AnalyticsEvent event);
}
