/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.metric;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;

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

    @Override
    public Metric calculate(Codebase codebase) {
        List<CodeUnit> aggregates = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT);

        if (aggregates.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    0.0,
                    "methods",
                    "Average number of methods in aggregate roots (no aggregates found)");
        }

        double avgMethods = aggregates.stream()
                .mapToInt(aggregate -> aggregate.methods().size())
                .average()
                .orElse(0.0);

        return Metric.of(
                METRIC_NAME,
                avgMethods,
                "methods",
                "Average number of methods in aggregate roots",
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }
}
