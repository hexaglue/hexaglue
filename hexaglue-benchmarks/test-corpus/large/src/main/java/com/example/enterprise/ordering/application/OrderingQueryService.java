package com.example.enterprise.ordering.application;

import com.example.enterprise.ordering.domain.model.*;
import com.example.enterprise.ordering.port.driven.OrderingAggregate1Repository;
import com.example.enterprise.ordering.domain.specification.OrderingSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for ordering read operations.
 */
public class OrderingQueryService {
    private final OrderingAggregate1Repository repository;

    public OrderingQueryService(OrderingAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<OrderingAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(OrderingSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<OrderingAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(OrderingSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<OrderingAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(OrderingSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(OrderingSpecifications.isActive())
            .count();
    }
}
