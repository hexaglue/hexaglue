package com.example.enterprise.inventory.domain.service;

import com.example.enterprise.inventory.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for inventory validation.
 */
public class InventoryValidationService {
    public List<String> validate(InventoryAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(InventoryAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
