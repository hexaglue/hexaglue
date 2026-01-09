package com.example.enterprise.marketing.port.driven;

import com.example.enterprise.marketing.domain.event.MarketingEvent;

/**
 * Driven port for publishing marketing domain events.
 */
public interface MarketingEventPublisher {
    void publish(MarketingEvent event);
    void publishAsync(MarketingEvent event);
}
