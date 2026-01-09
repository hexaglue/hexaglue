package com.example.enterprise.marketing.port.driven;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1;
import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for MarketingAggregate1 persistence.
 */
public interface MarketingAggregate1Repository {
    MarketingAggregate1 save(MarketingAggregate1 entity);
    Optional<MarketingAggregate1> findById(MarketingAggregate1Id id);
    List<MarketingAggregate1> findAll();
    void deleteById(MarketingAggregate1Id id);
    boolean existsById(MarketingAggregate1Id id);
    long count();
}
