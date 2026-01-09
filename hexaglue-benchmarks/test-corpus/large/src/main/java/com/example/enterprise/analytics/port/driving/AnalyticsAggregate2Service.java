package com.example.enterprise.analytics.port.driving;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for AnalyticsAggregate2 operations.
 */
public interface AnalyticsAggregate2Service {
    AnalyticsAggregate2Id create(CreateAnalyticsAggregate2Command command);
    AnalyticsAggregate2 get(AnalyticsAggregate2Id id);
    List<AnalyticsAggregate2> list();
    void update(UpdateAnalyticsAggregate2Command command);
    void delete(AnalyticsAggregate2Id id);
    void activate(AnalyticsAggregate2Id id);
    void complete(AnalyticsAggregate2Id id);
}
