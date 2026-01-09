package com.example.enterprise.customer.domain.service;

import com.example.enterprise.customer.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for customer calculations.
 */
public class CustomerCalculationService {
    public CustomerAmount1 calculateTotal(List<CustomerAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return CustomerAmount1.zero("USD");
        }
        CustomerAmount1 total = CustomerAmount1.zero(amounts.get(0).currency());
        for (CustomerAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
