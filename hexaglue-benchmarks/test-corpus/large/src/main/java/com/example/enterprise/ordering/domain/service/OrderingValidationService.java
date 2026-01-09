package com.example.enterprise.ordering.domain.service;

import com.example.enterprise.ordering.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for ordering validation.
 */
public class OrderingValidationService {
    public List<String> validate(OrderingAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(OrderingAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
