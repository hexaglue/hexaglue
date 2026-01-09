package com.example.enterprise.payment.domain.validation;

import java.util.List;
import java.util.Collections;

/**
 * Result of a validation operation.
 */
public record ValidationResult(boolean valid, List<String> errors) {
    public ValidationResult {
        errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
