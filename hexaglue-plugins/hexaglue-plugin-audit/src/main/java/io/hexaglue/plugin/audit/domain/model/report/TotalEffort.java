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
 * @param days total days required
 * @param cost optional cost estimate
 * @since 5.0.0
 */
public record TotalEffort(
        double days,
        CostEstimate cost) {

    /**
     * Creates total effort with validation.
     */
    public TotalEffort {
        if (days < 0) {
            throw new IllegalArgumentException("days cannot be negative");
        }
    }

    /**
     * Creates total effort without cost.
     *
     * @param days number of days
     * @return total effort
     */
    public static TotalEffort days(double days) {
        return new TotalEffort(days, null);
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
        return new TotalEffort(days, CostEstimate.fromDays(days, dailyRate, currency));
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
}
