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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * <p><b>REFACTORED (v5.0.0):</b> Now passes {@link ArchitecturalModel} to calculators
 * for v5 ArchType API access via {@code model.domainIndex()} and {@code model.portIndex()}.
 *
 * @since 1.0.0
 * @since 5.0.0 Updated to pass ArchitecturalModel to calculators for v5 ArchType access
 */
public class MetricAggregator {

    private static final Logger LOGGER = Logger.getLogger(MetricAggregator.class.getName());

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
     * Calculates the specified metrics using v5 ArchType API.
     *
     * <p>This method iterates through the enabled metrics, executes their
     * calculators with access to the v5 ArchitecturalModel, and collects
     * all results. If a calculator throws an exception, it is logged but
     * doesn't stop execution of other calculators.
     *
     * @param model             the architectural model containing v5 indices
     * @param codebase          the codebase for legacy access (to be phased out)
     * @param architectureQuery the query interface from Core (may be null)
     * @param enabledMetrics    the set of metric names to calculate (empty = all)
     * @return map of calculated metrics (metric name -> metric)
     * @since 5.0.0
     */
    public Map<String, Metric> calculateMetrics(
            ArchitecturalModel model,
            Codebase codebase,
            ArchitectureQuery architectureQuery,
            Set<String> enabledMetrics) {
        Objects.requireNonNull(model, "model required");
        Objects.requireNonNull(codebase, "codebase required");
        Objects.requireNonNull(enabledMetrics, "enabledMetrics required");

        // If empty, calculate all metrics
        Set<String> toCalculate = enabledMetrics.isEmpty() ? calculators.keySet() : enabledMetrics;

        return toCalculate.stream()
                .map(calculators::get)
                .filter(Objects::nonNull)
                .map(calculator -> {
                    try {
                        // Use the v5 method that accepts ArchitecturalModel
                        Metric metric = calculator.calculate(model, codebase, architectureQuery);
                        return Map.entry(calculator.metricName(), metric);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.WARNING,
                                "Error calculating metric " + calculator.metricName() + ": " + e.getMessage(),
                                e);
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
