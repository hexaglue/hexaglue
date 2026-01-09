package com.example.enterprise.marketing.domain.validation;

import com.example.enterprise.marketing.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for marketing domain objects.
 */
public class MarketingValidator {
    public ValidationResult validate(MarketingAggregate1 aggregate) {
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
