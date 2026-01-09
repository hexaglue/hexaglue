package com.example.enterprise.marketing.port.driven;

import com.example.enterprise.marketing.domain.model.MarketingAggregate2;
import com.example.enterprise.marketing.domain.model.MarketingAggregate2Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for MarketingAggregate2 persistence.
 */
public interface MarketingAggregate2Repository {
    MarketingAggregate2 save(MarketingAggregate2 entity);
    Optional<MarketingAggregate2> findById(MarketingAggregate2Id id);
    List<MarketingAggregate2> findAll();
    void deleteById(MarketingAggregate2Id id);
    boolean existsById(MarketingAggregate2Id id);
    long count();
}
