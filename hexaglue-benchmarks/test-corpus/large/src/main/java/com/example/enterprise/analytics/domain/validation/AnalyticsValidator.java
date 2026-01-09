package com.example.enterprise.analytics.domain.validation;

import com.example.enterprise.analytics.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for analytics domain objects.
 */
public class AnalyticsValidator {
    public ValidationResult validate(AnalyticsAggregate1 aggregate) {
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
