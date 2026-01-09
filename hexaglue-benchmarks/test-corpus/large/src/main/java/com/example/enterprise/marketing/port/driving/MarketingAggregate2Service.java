package com.example.enterprise.marketing.port.driving;

import com.example.enterprise.marketing.domain.model.MarketingAggregate2;
import com.example.enterprise.marketing.domain.model.MarketingAggregate2Id;
import java.util.List;

/**
 * Driving port (primary) for MarketingAggregate2 operations.
 */
public interface MarketingAggregate2Service {
    MarketingAggregate2Id create(CreateMarketingAggregate2Command command);
    MarketingAggregate2 get(MarketingAggregate2Id id);
    List<MarketingAggregate2> list();
    void update(UpdateMarketingAggregate2Command command);
    void delete(MarketingAggregate2Id id);
    void activate(MarketingAggregate2Id id);
    void complete(MarketingAggregate2Id id);
}
