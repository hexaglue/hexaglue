package com.example.ecommerce.domain.validation;

import com.example.ecommerce.domain.model.Customer;

/**
 * Validator for Customer aggregate.
 */
public class CustomerValidator {
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 50;

    public ValidationResult validate(Customer customer) {
        ValidationResult result = new ValidationResult();

        if (customer.getFirstName().length() < MIN_NAME_LENGTH) {
            result.addError("First name must be at least " + MIN_NAME_LENGTH + " characters");
        }

        if (customer.getFirstName().length() > MAX_NAME_LENGTH) {
            result.addError("First name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }

        if (customer.getLastName().length() < MIN_NAME_LENGTH) {
            result.addError("Last name must be at least " + MIN_NAME_LENGTH + " characters");
        }

        if (customer.getLastName().length() > MAX_NAME_LENGTH) {
            result.addError("Last name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }

        return result;
    }
}
