package com.example.enterprise.payment.application;

import com.example.enterprise.payment.domain.model.*;
import com.example.enterprise.payment.port.driven.PaymentAggregate1Repository;
import com.example.enterprise.payment.domain.specification.PaymentSpecifications;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for payment read operations.
 */
public class PaymentQueryService {
    private final PaymentAggregate1Repository repository;

    public PaymentQueryService(PaymentAggregate1Repository repository) {
        this.repository = repository;
    }

    public List<PaymentAggregate1> findActive() {
        return repository.findAll().stream()
            .filter(PaymentSpecifications.isActive())
            .collect(Collectors.toList());
    }

    public List<PaymentAggregate1> findPending() {
        return repository.findAll().stream()
            .filter(PaymentSpecifications.isPending())
            .collect(Collectors.toList());
    }

    public List<PaymentAggregate1> findCompleted() {
        return repository.findAll().stream()
            .filter(PaymentSpecifications.isCompleted())
            .collect(Collectors.toList());
    }

    public long countActive() {
        return repository.findAll().stream()
            .filter(PaymentSpecifications.isActive())
            .count();
    }
}
