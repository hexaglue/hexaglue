package com.example.enterprise.marketing.domain.service;

import com.example.enterprise.marketing.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for marketing calculations.
 */
public class MarketingCalculationService {
    public MarketingAmount1 calculateTotal(List<MarketingAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return MarketingAmount1.zero("USD");
        }
        MarketingAmount1 total = MarketingAmount1.zero(amounts.get(0).currency());
        for (MarketingAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
