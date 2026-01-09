package com.example.enterprise.customer.domain.validation;

import com.example.enterprise.customer.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for customer domain objects.
 */
public class CustomerValidator {
    public ValidationResult validate(CustomerAggregate1 aggregate) {
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
