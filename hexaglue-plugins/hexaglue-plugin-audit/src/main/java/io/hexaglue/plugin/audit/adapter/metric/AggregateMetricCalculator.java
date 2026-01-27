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

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;

/**
 * Calculates aggregate size metrics.
 *
 * <p>This calculator measures the average number of methods in aggregate roots.
 * Large aggregates with many methods are harder to understand, test, and maintain.
 *
 * <p><strong>Metric:</strong> aggregate.avgSize<br>
 * <strong>Unit:</strong> methods<br>
 * <strong>Threshold:</strong> Warning if > 20 methods<br>
 * <strong>Interpretation:</strong> Lower is generally better. Aggregates with more than
 * 20 methods may benefit from refactoring or splitting.
 *
 * @since 1.0.0
 */
public class AggregateMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "aggregate.avgSize";
    private static final double WARNING_THRESHOLD = 20.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the average number of methods in aggregate roots using the v5 ArchType API.
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.0.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        return model.domainIndex()
                .map(domain -> {
                    long aggregateCount = domain.aggregateRoots().count();

                    if (aggregateCount == 0) {
                        return Metric.of(
                                METRIC_NAME,
                                0.0,
                                "methods",
                                "Average number of methods in aggregate roots (no aggregates found)");
                    }

                    double avgMethods = domain.aggregateRoots()
                            .mapToInt(agg -> agg.structure().methods().size())
                            .average()
                            .orElse(0.0);

                    return Metric.of(
                            METRIC_NAME,
                            avgMethods,
                            "methods",
                            "Average number of methods in aggregate roots",
                            MetricThreshold.greaterThan(WARNING_THRESHOLD));
                })
                .orElse(Metric.of(
                        METRIC_NAME,
                        0.0,
                        "methods",
                        "Average number of methods in aggregate roots (domain index not available)"));
    }
}
