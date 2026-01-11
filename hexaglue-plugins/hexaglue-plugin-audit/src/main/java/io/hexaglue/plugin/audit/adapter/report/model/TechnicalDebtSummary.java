/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.plugin.audit.adapter.report.model;

import java.util.List;
import java.util.Objects;

/**
 * Summary of technical debt calculated from architectural violations.
 *
 * <p>Provides estimates in person-days and monetary cost for addressing
 * identified architectural issues. The debt is broken down by category
 * for prioritization.
 *
 * @param totalDays       total person-days to address all debt
 * @param totalCost       total monetary cost (based on configured daily rate)
 * @param monthlyInterest estimated monthly cost of not addressing the debt
 * @param breakdown       debt broken down by category
 * @since 1.0.0
 */
public record TechnicalDebtSummary(
        double totalDays, double totalCost, double monthlyInterest, List<DebtCategory> breakdown) {

    /** Default daily rate for cost calculation (EUR). */
    public static final double DEFAULT_DAILY_RATE = 500.0;

    /** Default monthly interest rate (percentage of total debt). */
    public static final double DEFAULT_INTEREST_RATE = 0.05;

    public TechnicalDebtSummary {
        if (totalDays < 0) {
            throw new IllegalArgumentException("totalDays cannot be negative: " + totalDays);
        }
        if (totalCost < 0) {
            throw new IllegalArgumentException("totalCost cannot be negative: " + totalCost);
        }
        if (monthlyInterest < 0) {
            throw new IllegalArgumentException("monthlyInterest cannot be negative: " + monthlyInterest);
        }
        breakdown = breakdown != null ? List.copyOf(breakdown) : List.of();
    }

    /**
     * Returns a zero technical debt summary.
     *
     * @return a TechnicalDebtSummary with all values at 0
     */
    public static TechnicalDebtSummary zero() {
        return new TechnicalDebtSummary(0.0, 0.0, 0.0, List.of());
    }

    /**
     * Creates a TechnicalDebtSummary from total days using default rates.
     *
     * @param totalDays total person-days of debt
     * @param breakdown debt breakdown by category
     * @return a new TechnicalDebtSummary
     */
    public static TechnicalDebtSummary fromDays(double totalDays, List<DebtCategory> breakdown) {
        double cost = totalDays * DEFAULT_DAILY_RATE;
        double interest = cost * DEFAULT_INTEREST_RATE;
        return new TechnicalDebtSummary(totalDays, cost, interest, breakdown);
    }

    /**
     * Returns true if there is no technical debt.
     *
     * @return true if totalDays is 0
     */
    public boolean isZero() {
        return totalDays == 0.0;
    }

    /**
     * A category of technical debt.
     *
     * @param category    the category name (e.g., "Dependency Cycles", "Layer Violations")
     * @param days        person-days for this category
     * @param cost        monetary cost for this category
     * @param description brief description of what needs to be done
     */
    public record DebtCategory(String category, double days, double cost, String description) {
        public DebtCategory {
            Objects.requireNonNull(category, "category required");
            if (days < 0) {
                throw new IllegalArgumentException("days cannot be negative: " + days);
            }
            if (cost < 0) {
                throw new IllegalArgumentException("cost cannot be negative: " + cost);
            }
            Objects.requireNonNull(description, "description required");
        }

        /**
         * Creates a DebtCategory from days using the default daily rate.
         *
         * @param category    the category name
         * @param days        person-days
         * @param description the description
         * @return a new DebtCategory
         */
        public static DebtCategory fromDays(String category, double days, String description) {
            return new DebtCategory(category, days, days * DEFAULT_DAILY_RATE, description);
        }
    }

    /**
     * Creates a builder for constructing a TechnicalDebtSummary.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TechnicalDebtSummary.
     */
    public static final class Builder {
        private double totalDays;
        private double totalCost;
        private double monthlyInterest;
        private List<DebtCategory> breakdown = List.of();

        public Builder totalDays(double days) {
            this.totalDays = days;
            return this;
        }

        public Builder totalCost(double cost) {
            this.totalCost = cost;
            return this;
        }

        public Builder monthlyInterest(double interest) {
            this.monthlyInterest = interest;
            return this;
        }

        public Builder breakdown(List<DebtCategory> breakdown) {
            this.breakdown = breakdown;
            return this;
        }

        public TechnicalDebtSummary build() {
            return new TechnicalDebtSummary(totalDays, totalCost, monthlyInterest, breakdown);
        }
    }
}
