package com.example.ecommerce.domain.validation;

import com.example.ecommerce.domain.model.Order;
import com.example.ecommerce.domain.model.OrderLine;

/**
 * Validator for Order aggregate.
 */
public class OrderValidator {
    private static final int MAX_ORDER_LINES = 100;
    private static final int MIN_ORDER_LINES = 1;

    public ValidationResult validate(Order order) {
        ValidationResult result = new ValidationResult();

        if (order.getOrderLines().size() < MIN_ORDER_LINES) {
            result.addError("Order must have at least one line item");
        }

        if (order.getOrderLines().size() > MAX_ORDER_LINES) {
            result.addError("Order cannot have more than " + MAX_ORDER_LINES + " line items");
        }

        for (OrderLine line : order.getOrderLines()) {
            if (line.getQuantity() <= 0) {
                result.addError("Order line quantity must be positive");
            }
        }

        return result;
    }
}
