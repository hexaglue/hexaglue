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

package io.hexaglue.plugin.audit.domain.model;

/**
 * Represents an estimation of technical debt based on architectural violations.
 *
 * <p>This immutable value object captures the financial and time costs associated
 * with addressing identified violations. The estimation includes:
 * <ul>
 *   <li>Total person-days required to fix all violations</li>
 *   <li>Total monetary cost to address all violations</li>
 *   <li>Monthly interest (ongoing cost of not fixing violations)</li>
 * </ul>
 *
 * <p>The debt estimation is calculated by mapping violation severities to estimated
 * effort in person-days. The total cost is derived from the total days multiplied
 * by a configurable cost-per-day rate. Monthly interest represents the ongoing
 * maintenance burden and increased future cost if violations remain unaddressed.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * DebtEstimation debt = new DebtEstimation(12.5, 6250.0, 312.5);
 * System.out.println("Estimated effort: " + debt.totalDays() + " days");
 * System.out.println("Total cost: $" + debt.totalCost());
 * System.out.println("Monthly interest: $" + debt.monthlyInterest());
 * }</pre>
 *
 * @param totalDays       the total person-days required to address all violations
 * @param totalCost       the total monetary cost (total days * cost per day)
 * @param monthlyInterest the monthly interest cost of leaving violations unaddressed
 * @since 1.0.0
 */
public record DebtEstimation(double totalDays, double totalCost, double monthlyInterest) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if any value is negative
     */
    public DebtEstimation {
        if (totalDays < 0) {
            throw new IllegalArgumentException("totalDays cannot be negative: " + totalDays);
        }
        if (totalCost < 0) {
            throw new IllegalArgumentException("totalCost cannot be negative: " + totalCost);
        }
        if (monthlyInterest < 0) {
            throw new IllegalArgumentException("monthlyInterest cannot be negative: " + monthlyInterest);
        }
    }

    /**
     * Returns a zero-debt estimation (no violations found).
     *
     * @return a DebtEstimation with all values set to 0.0
     */
    public static DebtEstimation zero() {
        return new DebtEstimation(0.0, 0.0, 0.0);
    }

    /**
     * Returns true if there is no debt (all values are zero).
     *
     * @return true if totalDays, totalCost, and monthlyInterest are all 0.0
     */
    public boolean isZero() {
        return totalDays == 0.0 && totalCost == 0.0 && monthlyInterest == 0.0;
    }
}
