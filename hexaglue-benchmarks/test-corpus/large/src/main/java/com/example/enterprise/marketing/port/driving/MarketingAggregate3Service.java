package com.example.enterprise.marketing.port.driving;

import com.example.enterprise.marketing.domain.model.MarketingAggregate3;
import com.example.enterprise.marketing.domain.model.MarketingAggregate3Id;
import java.util.List;

/**
 * Driving port (primary) for MarketingAggregate3 operations.
 */
public interface MarketingAggregate3Service {
    MarketingAggregate3Id create(CreateMarketingAggregate3Command command);
    MarketingAggregate3 get(MarketingAggregate3Id id);
    List<MarketingAggregate3> list();
    void update(UpdateMarketingAggregate3Command command);
    void delete(MarketingAggregate3Id id);
    void activate(MarketingAggregate3Id id);
    void complete(MarketingAggregate3Id id);
}
