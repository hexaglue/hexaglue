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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the maximum fan-out (outgoing dependencies) across all types.
 *
 * <p>Fan-out measures the number of types that a given type depends on. A type with
 * high fan-out is fragile because changes in any of its dependencies may break it.
 * The metric reports the maximum fan-out found across all types.
 *
 * <p>The description also reports the maximum fan-in (incoming dependencies) and
 * identifies types at high risk (high fan-out and high fan-in simultaneously).
 *
 * <p><strong>Metric:</strong> architecture.fan.out.max<br>
 * <strong>Unit:</strong> dependencies<br>
 * <strong>Threshold:</strong> Warning if &gt; 20<br>
 * <strong>Interpretation:</strong> Lower is better. A max fan-out above 20 indicates
 * a type that depends on too many others, suggesting it should be decomposed.
 *
 * @since 5.1.0
 */
public class FanOutMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.fan.out.max";
    private static final double WARNING_THRESHOLD = 20.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the maximum fan-out using the dependency graph from ArchitectureQuery.
     *
     * <p>Also computes fan-in (by inverting the graph) and identifies high-risk types
     * that have both high fan-out and high fan-in.
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.1.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        if (architectureQuery == null) {
            return Metric.of(METRIC_NAME, 0.0, "dependencies", "Max fan-out (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        if (dependencies.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    0.0,
                    "dependencies",
                    "Max fan-out (no dependencies found)",
                    MetricThreshold.greaterThan(WARNING_THRESHOLD));
        }

        // Compute fan-out per type
        int maxFanOut = 0;
        String maxFanOutType = "";
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            int fanOut = entry.getValue().size();
            if (fanOut > maxFanOut) {
                maxFanOut = fanOut;
                maxFanOutType = entry.getKey();
            }
        }

        // Compute fan-in by inverting the graph
        Map<String, Integer> fanInCounts = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            for (String target : entry.getValue()) {
                fanInCounts.merge(target, 1, Integer::sum);
            }
        }

        int maxFanIn = 0;
        String maxFanInType = "";
        for (Map.Entry<String, Integer> entry : fanInCounts.entrySet()) {
            if (entry.getValue() > maxFanIn) {
                maxFanIn = entry.getValue();
                maxFanInType = entry.getKey();
            }
        }

        String description = String.format(
                "Max fan-out: %d (%s), max fan-in: %d (%s)",
                maxFanOut, simpleName(maxFanOutType), maxFanIn, simpleName(maxFanInType));

        return Metric.of(
                METRIC_NAME, maxFanOut, "dependencies", description, MetricThreshold.greaterThan(WARNING_THRESHOLD));
    }

    private String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
