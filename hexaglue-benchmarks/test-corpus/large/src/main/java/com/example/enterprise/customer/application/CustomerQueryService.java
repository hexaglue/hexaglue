package com.example.enterprise.customer.application;

import com.example.enterprise.customer.domain.model.*;
import com.example.enterprise.customer.port.driven.CustomerAggregate1Repository;
import com.example.enterprise.customer.domain.specification.CustomerSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for customer read operations.
 */
public class CustomerQueryService {
    private final CustomerAggregate1Repository repository;

    public CustomerQueryService(CustomerAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<CustomerAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(CustomerSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<CustomerAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(CustomerSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<CustomerAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(CustomerSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(CustomerSpecifications.isActive())
            .count();
    }
}
