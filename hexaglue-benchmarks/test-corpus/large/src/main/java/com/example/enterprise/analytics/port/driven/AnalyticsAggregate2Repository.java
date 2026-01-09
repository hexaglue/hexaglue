package com.example.enterprise.analytics.port.driven;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for AnalyticsAggregate2 persistence.
 */
public interface AnalyticsAggregate2Repository {
    AnalyticsAggregate2 save(AnalyticsAggregate2 entity);
    Optional<AnalyticsAggregate2> findById(AnalyticsAggregate2Id id);
    List<AnalyticsAggregate2> findAll();
    void deleteById(AnalyticsAggregate2Id id);
    boolean existsById(AnalyticsAggregate2Id id);
    long count();
}
