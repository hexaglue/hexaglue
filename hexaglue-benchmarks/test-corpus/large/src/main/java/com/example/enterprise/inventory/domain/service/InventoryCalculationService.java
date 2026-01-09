package com.example.enterprise.inventory.domain.service;

import com.example.enterprise.inventory.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for inventory calculations.
 */
public class InventoryCalculationService {
    public InventoryAmount1 calculateTotal(List<InventoryAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return InventoryAmount1.zero("USD");
        }
        InventoryAmount1 total = InventoryAmount1.zero(amounts.get(0).currency());
        for (InventoryAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
