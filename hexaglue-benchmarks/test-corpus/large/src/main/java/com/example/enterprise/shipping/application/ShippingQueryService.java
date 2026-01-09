package com.example.enterprise.shipping.application;

import com.example.enterprise.shipping.domain.model.*;
import com.example.enterprise.shipping.port.driven.ShippingAggregate1Repository;
import com.example.enterprise.shipping.domain.specification.ShippingSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for shipping read operations.
 */
public class ShippingQueryService {
    private final ShippingAggregate1Repository repository;

    public ShippingQueryService(ShippingAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<ShippingAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(ShippingSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<ShippingAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(ShippingSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<ShippingAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(ShippingSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(ShippingSpecifications.isActive())
            .count();
    }
}
