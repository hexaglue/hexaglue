package com.example.enterprise.analytics.domain.service;

import com.example.enterprise.analytics.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for analytics validation.
 */
public class AnalyticsValidationService {
    public List<String> validate(AnalyticsAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(AnalyticsAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
