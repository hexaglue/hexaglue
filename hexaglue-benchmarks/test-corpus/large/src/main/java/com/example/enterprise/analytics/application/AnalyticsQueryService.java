package com.example.enterprise.analytics.application;

import com.example.enterprise.analytics.domain.model.*;
import com.example.enterprise.analytics.port.driven.AnalyticsAggregate1Repository;
import com.example.enterprise.analytics.domain.specification.AnalyticsSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for analytics read operations.
 */
public class AnalyticsQueryService {
    private final AnalyticsAggregate1Repository repository;

    public AnalyticsQueryService(AnalyticsAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<AnalyticsAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(AnalyticsSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<AnalyticsAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(AnalyticsSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<AnalyticsAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(AnalyticsSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(AnalyticsSpecifications.isActive())
            .count();
    }
}
