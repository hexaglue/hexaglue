package com.example.enterprise.warehouse.domain.service;

import com.example.enterprise.warehouse.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for warehouse validation.
 */
public class WarehouseValidationService {
    public List<String> validate(WarehouseAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(WarehouseAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
