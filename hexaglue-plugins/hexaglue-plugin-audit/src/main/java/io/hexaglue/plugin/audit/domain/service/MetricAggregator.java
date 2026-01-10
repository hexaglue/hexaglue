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

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.Codebase;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain service for aggregating metrics.
 *
 * <p>This service is responsible for:
 * <ul>
 *   <li>Managing the registry of metric calculators</li>
 *   <li>Executing enabled calculators</li>
 *   <li>Collecting calculated metrics</li>
 *   <li>Handling calculation errors gracefully</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class MetricAggregator {

    private final Map<String, MetricCalculator> calculators;

    /**
     * Creates a new metric aggregator with the given calculators.
     *
     * @param calculators the map of calculators (metric name -> calculator)
     */
    public MetricAggregator(Map<String, MetricCalculator> calculators) {
        this.calculators = Map.copyOf(Objects.requireNonNull(calculators, "calculators required"));
    }

    /**
     * Calculates the specified metrics for the codebase.
     *
     * <p>This method iterates through the enabled metrics, executes their
     * calculators, and collects all results. If a calculator throws an exception,
     * it is logged but doesn't stop execution of other calculators.
     *
     * @param codebase       the codebase to analyze
     * @param enabledMetrics the set of metric names to calculate (empty = all)
     * @return map of calculated metrics (metric name -> metric)
     */
    public Map<String, Metric> calculateMetrics(Codebase codebase, Set<String> enabledMetrics) {
        Objects.requireNonNull(codebase, "codebase required");
        Objects.requireNonNull(enabledMetrics, "enabledMetrics required");

        // If empty, calculate all metrics
        Set<String> toCalculate = enabledMetrics.isEmpty() ? calculators.keySet() : enabledMetrics;

        return toCalculate.stream()
                .map(calculators::get)
                .filter(Objects::nonNull)
                .map(calculator -> {
                    try {
                        Metric metric = calculator.calculate(codebase);
                        return Map.entry(calculator.metricName(), metric);
                    } catch (Exception e) {
                        // Log error but continue with other calculators
                        System.err.println(
                                "Error calculating metric " + calculator.metricName() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns the number of registered calculators.
     *
     * @return the calculator count
     */
    public int calculatorCount() {
        return calculators.size();
    }

    /**
     * Checks if a calculator is registered for the given metric.
     *
     * @param metricName the metric name
     * @return true if a calculator is registered
     */
    public boolean hasCalculator(String metricName) {
        return calculators.containsKey(metricName);
    }
}
