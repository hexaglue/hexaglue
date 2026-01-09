package com.example.enterprise.catalog.domain.service;

import com.example.enterprise.catalog.domain.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for catalog validation.
 */
public class CatalogValidationService {
    public List<String> validate(CatalogAggregate1 aggregate) {
        List<String> errors = new ArrayList<>();
        if (aggregate.getName() == null || aggregate.getName().isBlank()) {
            errors.add("Name is required");
        }
        if (aggregate.getItems().isEmpty()) {
            errors.add("At least one item is required");
        }
        return errors;
    }

    public boolean isValid(CatalogAggregate1 aggregate) {
        return validate(aggregate).isEmpty();
    }
}
