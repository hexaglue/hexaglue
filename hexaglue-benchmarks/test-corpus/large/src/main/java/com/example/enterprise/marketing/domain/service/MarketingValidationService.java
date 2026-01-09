package com.example.enterprise.marketing.domain.service;

import com.example.enterprise.marketing.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for marketing validation.
 */
public class MarketingValidationService {
    public List<String> validate(MarketingAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(MarketingAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
