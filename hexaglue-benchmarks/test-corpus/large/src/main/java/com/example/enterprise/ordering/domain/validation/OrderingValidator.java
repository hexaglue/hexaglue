package com.example.enterprise.ordering.domain.validation;

import com.example.enterprise.ordering.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for ordering domain objects.
 */
public class OrderingValidator {
    public ValidationResult validate(OrderingAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();

        if (aggregate.getId() == null) {
            errors.add("Id is required");
        }
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getStatus() == null) {
            errors.add("Status is required");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
