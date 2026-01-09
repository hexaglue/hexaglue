package com.example.enterprise.supplier.domain.service;

import com.example.enterprise.supplier.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for supplier validation.
 */
public class SupplierValidationService {
    public List<String> validate(SupplierAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(SupplierAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
