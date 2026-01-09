package com.example.enterprise.shipping.domain.validation;

import com.example.enterprise.shipping.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for shipping domain objects.
 */
public class ShippingValidator {
    public ValidationResult validate(ShippingAggregate1 aggregate) {
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
