package com.example.enterprise.inventory.domain.validation;

import com.example.enterprise.inventory.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for inventory domain objects.
 */
public class InventoryValidator {
    public ValidationResult validate(InventoryAggregate1 aggregate) {
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
