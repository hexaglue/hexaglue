package com.example.enterprise.analytics.port.driven;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for AnalyticsAggregate1 persistence.
 */
public interface AnalyticsAggregate1Repository {
    AnalyticsAggregate1 save(AnalyticsAggregate1 entity);
    Optional<AnalyticsAggregate1> findById(AnalyticsAggregate1Id id);
    List<AnalyticsAggregate1> findAll();
    void deleteById(AnalyticsAggregate1Id id);
    boolean existsById(AnalyticsAggregate1Id id);
    long count();
}
