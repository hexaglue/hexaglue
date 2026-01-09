package com.example.enterprise.analytics.domain.service;

import com.example.enterprise.analytics.domain.model.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for analytics calculations.
 */
public class AnalyticsCalculationService {
    public AnalyticsAmount1 calculateTotal(List<AnalyticsAmount1> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return AnalyticsAmount1.zero("USD");
        }
        AnalyticsAmount1 total = AnalyticsAmount1.zero(amounts.get(0).currency());
        for (AnalyticsAmount1 amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    public BigDecimal calculatePercentage(BigDecimal value, BigDecimal percentage) {
        return value.multiply(percentage).divide(BigDecimal.valueOf(100));
    }
}
