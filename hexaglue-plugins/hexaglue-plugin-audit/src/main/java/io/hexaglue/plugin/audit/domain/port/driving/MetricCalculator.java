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
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;

/**
 * Driving port for calculating quality metrics.
 *
 * <p>Each implementation calculates a specific metric. This follows the
 * Strategy pattern similar to {@link ConstraintValidator}.
 *
 * <p><b>REFACTORED (v3):</b> Calculators can now leverage the Core's
 * {@link ArchitectureQuery} for rich analysis capabilities instead of
 * duplicating calculations. The principle is: "Le Core produit des faits,
 * les plugins les exploitent."
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class CouplingMetricCalculator implements MetricCalculator {
 *
 *     @Override
 *     public String metricName() {
 *         return "coupling.average";
 *     }
 *
 *     @Override
 *     public Metric calculate(Codebase codebase, ArchitectureQuery query) {
 *         if (query != null) {
 *             // Use Core's rich analysis
 *             List<CouplingMetrics> metrics = query.analyzeAllPackageCoupling();
 *             double avg = metrics.stream().mapToDouble(CouplingMetrics::instability).average().orElse(0.0);
 *             return Metric.of("coupling.average", avg, "ratio", "Average coupling");
 *         }
 *         // Fallback without query
 *         return calculate(codebase);
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
     * Calculates the metric for the given codebase (legacy method).
     *
     * <p>This method is retained for backward compatibility with calculators
     * that don't need the ArchitectureQuery.
     *
     * @param codebase the codebase to analyze
     * @return the calculated metric
     */
    Metric calculate(Codebase codebase);

    /**
     * Calculates the metric using both codebase and architecture query.
     *
     * <p>This method allows calculators to leverage the Core's rich analysis
     * capabilities via {@link ArchitectureQuery}. Calculators should delegate
     * to the Core for graph traversal and coupling analysis, focusing only
     * on interpretation and judgment.
     *
     * <p>Default implementation delegates to {@link #calculate(Codebase)} for
     * backward compatibility.
     *
     * @param codebase the codebase to analyze
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 3.0.0
     */
    default Metric calculate(Codebase codebase, ArchitectureQuery architectureQuery) {
        return calculate(codebase);
    }
}
