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

import java.util.Objects;

/**
 * Cost estimate for remediation.
 *
 * @param amount total cost amount
 * @param currency currency code (e.g., "EUR", "USD")
 * @param dailyRate daily rate used for calculation
 * @since 5.0.0
 */
public record CostEstimate(
        double amount,
        String currency,
        double dailyRate) {

    /**
     * Creates a cost estimate with validation.
     */
    public CostEstimate {
        Objects.requireNonNull(currency, "currency is required");
        if (dailyRate <= 0) {
            throw new IllegalArgumentException("dailyRate must be positive");
        }
    }

    /**
     * Creates a cost estimate from days and daily rate.
     *
     * @param days number of days
     * @param dailyRate rate per day
     * @param currency currency code
     * @return the cost estimate
     */
    public static CostEstimate fromDays(double days, double dailyRate, String currency) {
        return new CostEstimate(days * dailyRate, currency, dailyRate);
    }

    /**
     * Returns formatted cost string.
     *
     * @return formatted string (e.g., "3000 EUR")
     */
    public String formatted() {
        if (amount == (int) amount) {
            return (int) amount + " " + currency;
        }
        return String.format("%.2f %s", amount, currency);
    }
}
