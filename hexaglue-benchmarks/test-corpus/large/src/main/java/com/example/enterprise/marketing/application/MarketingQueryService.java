package com.example.enterprise.marketing.application;

import com.example.enterprise.marketing.domain.model.*;
import com.example.enterprise.marketing.port.driven.MarketingAggregate1Repository;
import com.example.enterprise.marketing.domain.specification.MarketingSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for marketing read operations.
 */
public class MarketingQueryService {
    private final MarketingAggregate1Repository repository;

    public MarketingQueryService(MarketingAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<MarketingAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(MarketingSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<MarketingAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(MarketingSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<MarketingAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(MarketingSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(MarketingSpecifications.isActive())
            .count();
    }
}
