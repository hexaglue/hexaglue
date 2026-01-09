package com.example.enterprise.ordering.domain.service;

import com.example.enterprise.ordering.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for ordering calculations.
 */
public class OrderingCalculationService {
    public OrderingAmount1 calculateTotal(List<OrderingAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return OrderingAmount1.zero("USD");
        }
        OrderingAmount1 total = OrderingAmount1.zero(amounts.get(0).currency());
        for (OrderingAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
