package com.example.enterprise.shipping.domain.service;

import com.example.enterprise.shipping.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for shipping validation.
 */
public class ShippingValidationService {
    public List<String> validate(ShippingAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(ShippingAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
