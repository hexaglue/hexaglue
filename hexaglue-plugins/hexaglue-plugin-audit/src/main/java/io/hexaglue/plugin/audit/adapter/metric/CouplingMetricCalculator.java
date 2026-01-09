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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates aggregate coupling metrics.
 *
 * <p>This calculator measures the average efferent coupling between aggregates,
 * which represents the average number of outgoing dependencies from one aggregate
 * to other aggregates. High coupling indicates tight dependencies that can make
 * the system harder to change and understand.
 *
 * <p><strong>Metric:</strong> aggregate.coupling.efferent<br>
 * <strong>Unit:</strong> dependencies<br>
 * <strong>Threshold:</strong> Warning if > 3 dependencies<br>
 * <strong>Interpretation:</strong> Lower is better. Aggregates should be relatively
 * independent. More than 3 aggregate dependencies suggests the aggregate boundaries
 * may need reconsideration.
 *
 * @since 1.0.0
 */
public class CouplingMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "aggregate.coupling.efferent";
    private static final double WARNING_THRESHOLD = 3.0;

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
                    "dependencies",
                    "Average outgoing dependencies between aggregates (no aggregates found)");
        }

        // Build set of all aggregate qualified names for filtering
        Set<String> aggregateNames =
                aggregates.stream().map(CodeUnit::qualifiedName).collect(Collectors.toSet());

        // Calculate average efferent coupling (outgoing dependencies to other aggregates)
        double avgEfferent = aggregates.stream()
                .mapToInt(aggregate -> calculateEfferentCoupling(aggregate, aggregateNames, codebase))
                .average()
                .orElse(0.0);

        return Metric.of(
                METRIC_NAME,
                avgEfferent,
                "dependencies",
                "Average outgoing dependencies between aggregates",
                MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }

    /**
     * Calculates the efferent coupling for a single aggregate.
     *
     * <p>Efferent coupling is the number of other aggregates this aggregate depends on.
     *
     * @param aggregate the aggregate to analyze
     * @param aggregateNames set of all aggregate qualified names
     * @param codebase the codebase
     * @return the number of aggregate dependencies
     */
    private int calculateEfferentCoupling(CodeUnit aggregate, Set<String> aggregateNames, Codebase codebase) {
        Set<String> dependencies =
                codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());

        // Count dependencies to other aggregates (excluding self)
        return (int) dependencies.stream()
                .filter(aggregateNames::contains)
                .filter(dep -> !dep.equals(aggregate.qualifiedName()))
                .count();
    }
}
