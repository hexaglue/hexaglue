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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.Codebase;

/**
 * Driving port for calculating quality metrics.
 *
 * <p>Each implementation calculates a specific metric. This follows the
 * Strategy pattern similar to {@link ConstraintValidator}.
 *
 * <p><b>REFACTORED (v5.0.0):</b> Calculators now use the v5 ArchType API via
 * {@link ArchitecturalModel} for type access. Use {@code model.domainIndex()}
 * and {@code model.portIndex()} for typed access to domain and port types.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class AggregateMetricCalculator implements MetricCalculator {
 *
 *     @Override
 *     public String metricName() {
 *         return "aggregate.count";
 *     }
 *
 *     @Override
 *     public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {
 *         // Use v5 API for type access
 *         long count = model.domainIndex()
 *             .map(domain -> domain.aggregateRoots().count())
 *             .orElse(0L);
 *         return Metric.of("aggregate.count", count, "count", "Number of aggregate roots");
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @since 5.0.0 Added ArchitecturalModel parameter for v5 ArchType access
 */
public interface MetricCalculator {

    /**
     * Returns the unique name of the metric calculated by this calculator.
     *
     * @return the metric name (e.g., "aggregate.avgSize")
     */
    String metricName();

    /**
     * Calculates the metric using the v5 ArchType API.
     *
     * <p>Calculators should use the v5 indices for type access:
     * <ul>
     *   <li>{@code model.domainIndex()} for aggregate roots, entities, value objects, etc.</li>
     *   <li>{@code model.portIndex()} for driving and driven ports</li>
     *   <li>{@code model.typeRegistry()} for generic type access</li>
     * </ul>
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access (to be phased out)
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.0.0
     */
    Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery);
}
