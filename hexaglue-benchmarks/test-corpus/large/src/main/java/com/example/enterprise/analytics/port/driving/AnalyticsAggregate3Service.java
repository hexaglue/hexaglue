package com.example.enterprise.analytics.port.driving;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for AnalyticsAggregate3 operations.
 */
public interface AnalyticsAggregate3Service {
    AnalyticsAggregate3Id create(CreateAnalyticsAggregate3Command command);
    AnalyticsAggregate3 get(AnalyticsAggregate3Id id);
    List<AnalyticsAggregate3> list();
    void update(UpdateAnalyticsAggregate3Command command);
    void delete(AnalyticsAggregate3Id id);
    void activate(AnalyticsAggregate3Id id);
    void complete(AnalyticsAggregate3Id id);
}
