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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the independence ratio between adapters.
 *
 * <p>In hexagonal architecture, adapters should be interchangeable: a REST adapter
 * must not depend on a JPA adapter, and vice versa. This metric measures how well
 * adapters respect this isolation principle.
 *
 * <p>The algorithm:
 * <ol>
 *   <li>Enumerate all ports (driving + driven) from the model's PortIndex</li>
 *   <li>Find implementors of each port via {@code architectureQuery.findImplementors()}</li>
 *   <li>Collect all adapter qualified names</li>
 *   <li>From the dependency graph, count inter-adapter dependencies vs total dependencies</li>
 *   <li>{@code Independence = (1 - interAdapterDeps / totalDeps) * 100}</li>
 * </ol>
 *
 * <p><strong>Metric:</strong> hexagonal.adapter.independence<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if &lt; 80%<br>
 * <strong>Interpretation:</strong> Higher is better. 100% means no adapter depends on
 * another adapter. Below 80% indicates significant coupling between adapters.
 *
 * @since 5.1.0
 */
public class AdapterIndependenceMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "hexagonal.adapter.independence";
    private static final double WARNING_THRESHOLD = 80.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates adapter independence using PortIndex and ArchitectureQuery.
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
            return Metric.of(METRIC_NAME, 0.0, "%", "Adapter independence (architecture query not available)");
        }

        // Collect all port qualified names
        Set<String> adapterNames = new HashSet<>();
        model.portIndex().ifPresent(portIndex -> {
            portIndex.drivingPorts().forEach(port -> {
                List<String> implementors =
                        architectureQuery.findImplementors(port.id().qualifiedName());
                adapterNames.addAll(implementors);
            });
            portIndex.drivenPorts().forEach(port -> {
                List<String> implementors =
                        architectureQuery.findImplementors(port.id().qualifiedName());
                adapterNames.addAll(implementors);
            });
        });

        // Also consider types from typeRegistry that are PortType implementors
        // but detected through other mechanisms
        if (adapterNames.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    100.0,
                    "%",
                    "Adapter independence: 100% (no adapters found)",
                    MetricThreshold.lessThan(WARNING_THRESHOLD));
        }

        // Analyze dependencies between adapters
        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        long totalDeps = 0;
        long interAdapterDeps = 0;

        for (String adapter : adapterNames) {
            Set<String> adapterDeps = dependencies.getOrDefault(adapter, Set.of());
            for (String dep : adapterDeps) {
                if (!dep.equals(adapter)) {
                    totalDeps++;
                    if (adapterNames.contains(dep)) {
                        interAdapterDeps++;
                    }
                }
            }
        }

        double independence;
        if (totalDeps == 0) {
            independence = 100.0;
        } else {
            independence = (1.0 - (double) interAdapterDeps / totalDeps) * 100.0;
        }

        return Metric.of(
                METRIC_NAME,
                independence,
                "%",
                String.format(
                        "Adapter independence: %.1f%% (%d inter-adapter deps out of %d total, %d adapters)",
                        independence, interAdapterDeps, totalDeps, adapterNames.size()),
                MetricThreshold.lessThan(WARNING_THRESHOLD));
    }
}
