package com.example.enterprise.warehouse.domain.service;

import com.example.enterprise.warehouse.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for warehouse calculations.
 */
public class WarehouseCalculationService {
    public WarehouseAmount1 calculateTotal(List<WarehouseAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return WarehouseAmount1.zero("USD");
        }
        WarehouseAmount1 total = WarehouseAmount1.zero(amounts.get(0).currency());
        for (WarehouseAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
