package com.example.enterprise.customer.domain.service;

import com.example.enterprise.customer.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for customer validation.
 */
public class CustomerValidationService {
    public List<String> validate(CustomerAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(CustomerAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
