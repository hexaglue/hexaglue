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

package io.hexaglue.plugin.audit.domain.port.driving;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.spi.audit.Codebase;

/**
 * Driving port for calculating quality metrics.
 *
 * <p>Each implementation calculates a specific metric. This follows the
 * Strategy pattern similar to {@link ConstraintValidator}.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class AggregateMetricCalculator implements MetricCalculator {
 *
 *     @Override
 *     public String metricName() {
 *         return "aggregate.avgSize";
 *     }
 *
 *     @Override
 *     public Metric calculate(Codebase codebase) {
 *         // Calculate metric
 *         return Metric.of("aggregate.avgSize", avgSize, "methods", "Average aggregate size");
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface MetricCalculator {

    /**
     * Returns the unique name of the metric calculated by this calculator.
     *
     * @return the metric name (e.g., "aggregate.avgSize")
     */
    String metricName();

    /**
     * Calculates the metric for the given codebase.
     *
     * @param codebase the codebase to analyze
     * @return the calculated metric
     */
    Metric calculate(Codebase codebase);
}
