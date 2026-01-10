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

package io.hexaglue.plugin.audit.adapter.analyzer;

import io.hexaglue.plugin.audit.domain.model.DebtEstimation;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Estimates technical debt from architectural violations.
 *
 * <p>This analyzer calculates the financial and time costs associated with
 * addressing violations. It uses a severity-based effort mapping to estimate
 * the person-days required to fix each violation, then calculates total cost
 * and monthly interest.
 *
 * <p><strong>Default effort estimates (person-days per violation):</strong>
 * <ul>
 *   <li>{@link Severity#BLOCKER BLOCKER}: 3.0 days - Critical architectural issues</li>
 *   <li>{@link Severity#CRITICAL CRITICAL}: 2.0 days - Serious DDD violations</li>
 *   <li>{@link Severity#MAJOR MAJOR}: 0.5 days - Important architectural issues</li>
 *   <li>{@link Severity#MINOR MINOR}: 0.25 days - Best practice violations</li>
 *   <li>{@link Severity#INFO INFO}: 0.0 days - Informational only</li>
 * </ul>
 *
 * <p><strong>Cost calculation:</strong>
 * <ul>
 *   <li>Total Cost = Total Days × Cost Per Day</li>
 *   <li>Monthly Interest = Total Days × 0.05 × Cost Per Day</li>
 * </ul>
 *
 * <p>The monthly interest (5% of total cost) represents the ongoing maintenance
 * burden and increased future cost if violations remain unaddressed. This models
 * the compound effect of technical debt over time.
 *
 * <p><strong>Example usage:</strong>
 * <pre>{@code
 * DebtEstimator estimator = new DebtEstimator(500.0); // $500 per day
 * DebtEstimation debt = estimator.estimate(violations);
 *
 * System.out.println("Total effort: " + debt.totalDays() + " days");
 * System.out.println("Total cost: $" + debt.totalCost());
 * System.out.println("Monthly interest: $" + debt.monthlyInterest());
 * }</pre>
 *
 * @since 1.0.0
 */
public class DebtEstimator {

    /**
     * Default cost per person-day in monetary units.
     */
    public static final double DEFAULT_COST_PER_DAY = 500.0;

    /**
     * Monthly interest rate applied to total cost (5%).
     */
    private static final double MONTHLY_INTEREST_RATE = 0.05;

    private static final Map<Severity, Double> DEFAULT_EFFORT_PER_SEVERITY = Map.of(
            Severity.BLOCKER, 3.0,
            Severity.CRITICAL, 2.0,
            Severity.MAJOR, 0.5,
            Severity.MINOR, 0.25,
            Severity.INFO, 0.0);

    private final double costPerDay;
    private final Map<Severity, Double> effortPerSeverity;

    /**
     * Creates a debt estimator with default cost per day.
     */
    public DebtEstimator() {
        this(DEFAULT_COST_PER_DAY);
    }

    /**
     * Creates a debt estimator with custom cost per day.
     *
     * @param costPerDay the cost per person-day
     * @throws IllegalArgumentException if costPerDay is negative
     */
    public DebtEstimator(double costPerDay) {
        this(costPerDay, DEFAULT_EFFORT_PER_SEVERITY);
    }

    /**
     * Creates a debt estimator with custom cost and effort mapping.
     *
     * <p>This constructor allows full customization of both the cost per day
     * and the effort estimates for each severity level. Use this when you need
     * to adjust the default estimates based on your team's actual velocity.
     *
     * @param costPerDay         the cost per person-day
     * @param effortPerSeverity  map of severity to effort in days
     * @throws IllegalArgumentException if costPerDay is negative
     * @throws NullPointerException     if effortPerSeverity is null
     */
    public DebtEstimator(double costPerDay, Map<Severity, Double> effortPerSeverity) {
        if (costPerDay < 0) {
            throw new IllegalArgumentException("costPerDay cannot be negative: " + costPerDay);
        }
        this.costPerDay = costPerDay;
        this.effortPerSeverity = Map.copyOf(Objects.requireNonNull(effortPerSeverity, "effortPerSeverity required"));
    }

    /**
     * Estimates technical debt from the given violations.
     *
     * <p>The estimation aggregates effort across all violations based on their
     * severity levels. If no violations are provided or the list is empty,
     * returns a zero-debt estimation.
     *
     * @param violations the list of violations to estimate
     * @return the debt estimation
     * @throws NullPointerException if violations is null
     */
    public DebtEstimation estimate(List<Violation> violations) {
        Objects.requireNonNull(violations, "violations required");

        if (violations.isEmpty()) {
            return DebtEstimation.zero();
        }

        double totalDays = violations.stream()
                .mapToDouble(violation -> effortPerSeverity.getOrDefault(violation.severity(), 0.0))
                .sum();

        double totalCost = totalDays * costPerDay;
        double monthlyInterest = totalDays * MONTHLY_INTEREST_RATE * costPerDay;

        return new DebtEstimation(totalDays, totalCost, monthlyInterest);
    }

    /**
     * Returns the configured cost per day.
     *
     * @return the cost per person-day
     */
    public double costPerDay() {
        return costPerDay;
    }
}
