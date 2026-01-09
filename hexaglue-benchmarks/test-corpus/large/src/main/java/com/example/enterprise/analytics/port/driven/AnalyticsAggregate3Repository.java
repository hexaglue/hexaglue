package com.example.enterprise.analytics.port.driven;

import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3;
import com.example.enterprise.analytics.domain.model.AnalyticsAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for AnalyticsAggregate3 persistence.
 */
public interface AnalyticsAggregate3Repository {
    AnalyticsAggregate3 save(AnalyticsAggregate3 entity);
    Optional<AnalyticsAggregate3> findById(AnalyticsAggregate3Id id);
    List<AnalyticsAggregate3> findAll();
    void deleteById(AnalyticsAggregate3Id id);
    boolean existsById(AnalyticsAggregate3Id id);
    long count();
}
