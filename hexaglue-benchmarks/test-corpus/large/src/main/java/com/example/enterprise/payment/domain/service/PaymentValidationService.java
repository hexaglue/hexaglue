package com.example.enterprise.payment.domain.service;

import com.example.enterprise.payment.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for payment validation.
 */
public class PaymentValidationService {
    public List<String> validate(PaymentAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(PaymentAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
