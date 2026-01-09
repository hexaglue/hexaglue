package com.example.enterprise.marketing.port.driving;

import com.example.enterprise.marketing.domain.model.MarketingAggregate1;
import com.example.enterprise.marketing.domain.model.MarketingAggregate1Id;
import java.util.List;

/**
 * Driving port (primary) for MarketingAggregate1 operations.
 */
public interface MarketingAggregate1Service {
    MarketingAggregate1Id create(CreateMarketingAggregate1Command command);
    MarketingAggregate1 get(MarketingAggregate1Id id);
    List<MarketingAggregate1> list();
    void update(UpdateMarketingAggregate1Command command);
    void delete(MarketingAggregate1Id id);
    void activate(MarketingAggregate1Id id);
    void complete(MarketingAggregate1Id id);
}
