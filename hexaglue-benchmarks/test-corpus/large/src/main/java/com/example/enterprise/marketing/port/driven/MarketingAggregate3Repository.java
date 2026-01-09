package com.example.enterprise.marketing.port.driven;

import com.example.enterprise.marketing.domain.model.MarketingAggregate3;
import com.example.enterprise.marketing.domain.model.MarketingAggregate3Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for MarketingAggregate3 persistence.
 */
public interface MarketingAggregate3Repository {
    MarketingAggregate3 save(MarketingAggregate3 entity);
    Optional<MarketingAggregate3> findById(MarketingAggregate3Id id);
    List<MarketingAggregate3> findAll();
    void deleteById(MarketingAggregate3Id id);
    boolean existsById(MarketingAggregate3Id id);
    long count();
}
