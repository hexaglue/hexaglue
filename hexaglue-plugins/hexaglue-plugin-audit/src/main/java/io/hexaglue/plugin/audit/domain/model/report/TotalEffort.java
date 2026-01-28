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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Optional;

/**
 * Total effort estimate for all remediation actions.
 *
 * @param days total days required (manual effort)
 * @param cost optional cost estimate (manual cost)
 * @param hexaglueSavingsDays days saved by using HexaGlue plugins
 * @param hexaglueSavingsCost cost saved by using HexaGlue plugins
 * @since 5.0.0
 */
public record TotalEffort(
        double days, CostEstimate cost, double hexaglueSavingsDays, CostEstimate hexaglueSavingsCost) {

    /**
     * Creates total effort with validation.
     */
    public TotalEffort {
        if (days < 0) {
            throw new IllegalArgumentException("days cannot be negative");
        }
        if (hexaglueSavingsDays < 0) {
            throw new IllegalArgumentException("hexaglueSavingsDays cannot be negative");
        }
    }

    /**
     * Creates total effort without cost.
     *
     * @param days number of days
     * @return total effort
     */
    public static TotalEffort days(double days) {
        return new TotalEffort(days, null, 0, null);
    }

    /**
     * Creates total effort with cost.
     *
     * @param days number of days
     * @param dailyRate daily rate
     * @param currency currency code
     * @return total effort with cost
     */
    public static TotalEffort withCost(double days, double dailyRate, String currency) {
        return new TotalEffort(days, CostEstimate.fromDays(days, dailyRate, currency), 0, null);
    }

    /**
     * Creates total effort with cost and HexaGlue savings.
     *
     * @param days total manual effort in days
     * @param hexaglueSavingsDays days that can be saved with HexaGlue
     * @param dailyRate daily rate
     * @param currency currency code
     * @return total effort with cost and savings
     */
    public static TotalEffort withSavings(double days, double hexaglueSavingsDays, double dailyRate, String currency) {
        return new TotalEffort(
                days,
                CostEstimate.fromDays(days, dailyRate, currency),
                hexaglueSavingsDays,
                CostEstimate.fromDays(hexaglueSavingsDays, dailyRate, currency));
    }

    /**
     * Returns cost as optional.
     *
     * @return optional cost
     */
    public Optional<CostEstimate> costOpt() {
        return Optional.ofNullable(cost);
    }

    /**
     * Returns formatted days string.
     *
     * @return formatted string (e.g., "6 days")
     */
    public String formattedDays() {
        if (days == 1.0) {
            return "1 day";
        }
        if (days == (int) days) {
            return (int) days + " days";
        }
        return days + " days";
    }

    /**
     * Checks if HexaGlue can provide savings.
     *
     * @return true if savings are available
     */
    public boolean hasHexaglueSavings() {
        return hexaglueSavingsDays > 0;
    }

    /**
     * Returns HexaGlue savings cost as optional.
     *
     * @return optional savings cost
     */
    public Optional<CostEstimate> hexaglueSavingsCostOpt() {
        return Optional.ofNullable(hexaglueSavingsCost);
    }

    /**
     * Returns the effective days after HexaGlue automation.
     *
     * @return days remaining after HexaGlue savings
     */
    public double effectiveDays() {
        return Math.max(0, days - hexaglueSavingsDays);
    }

    /**
     * Returns the effective cost after HexaGlue automation.
     *
     * @return cost remaining after HexaGlue savings, or empty if no cost info
     */
    public Optional<CostEstimate> effectiveCostOpt() {
        if (cost == null) {
            return Optional.empty();
        }
        double effectiveAmount =
                Math.max(0, cost.amount() - (hexaglueSavingsCost != null ? hexaglueSavingsCost.amount() : 0));
        return Optional.of(new CostEstimate(effectiveAmount, cost.currency(), cost.dailyRate()));
    }
}
