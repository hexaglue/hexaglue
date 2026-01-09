package com.example.enterprise.analytics.port.driving;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for AnalyticsAggregate1 operations.
 */
public interface AnalyticsAggregate1Service {
    AnalyticsAggregate1Id create(CreateAnalyticsAggregate1Command command);
    AnalyticsAggregate1 get(AnalyticsAggregate1Id id);
    List<AnalyticsAggregate1> list();
    void update(UpdateAnalyticsAggregate1Command command);
    void delete(AnalyticsAggregate1Id id);
    void activate(AnalyticsAggregate1Id id);
    void complete(AnalyticsAggregate1Id id);
}
