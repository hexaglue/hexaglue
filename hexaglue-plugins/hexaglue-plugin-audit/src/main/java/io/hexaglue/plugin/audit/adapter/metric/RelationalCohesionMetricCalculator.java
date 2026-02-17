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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Calculates relational cohesion H as defined by Dowalil (2019).
 *
 * <p>For each package, H = (internal_dependencies + 1) / type_count.
 * The metric reports the average H across all packages with at least 2 types.
 * An internal dependency is one where both source and target reside in the same package.
 *
 * <p><strong>Metric:</strong> architecture.cohesion.relational<br>
 * <strong>Unit:</strong> ratio<br>
 * <strong>Threshold:</strong> Warning if outside [1.5, 4.0]<br>
 * <strong>Interpretation:</strong> H &lt; 1.5 means weakly cohesive packages,
 * H &gt; 4.0 means overly coupled packages. The sweet spot is between 1.5 and 4.0.
 *
 * @see <a href="https://doi.org/10.1007/978-3-030-13427-3">Dowalil (2019)</a>
 * @since 5.1.0
 */
public class RelationalCohesionMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.cohesion.relational";
    private static final double THRESHOLD_LOW = 1.5;
    private static final double THRESHOLD_HIGH = 4.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the average relational cohesion H across all packages.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Build the full type dependency graph from {@code allTypeDependencies()}</li>
     *   <li>Group types by package (extract package from qualified name)</li>
     *   <li>For each package with &ge; 2 types, count internal dependencies</li>
     *   <li>Compute H(pkg) = (internal_deps + 1) / type_count</li>
     *   <li>Return the average H</li>
     * </ol>
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
            return Metric.of(METRIC_NAME, 0.0, "ratio", "Relational cohesion (architecture query not available)");
        }

        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();

        if (dependencies.isEmpty()) {
            return Metric.of(METRIC_NAME, 0.0, "ratio", "Relational cohesion (no dependencies found)");
        }

        // Collect all types (sources and targets)
        Set<String> allTypes = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTypes::addAll);

        // Group types by package
        Map<String, Set<String>> typesByPackage = new HashMap<>();
        for (String type : allTypes) {
            String pkg = extractPackage(type);
            typesByPackage.computeIfAbsent(pkg, k -> new HashSet<>()).add(type);
        }

        // Calculate H for each package with >= 2 types
        double totalH = 0.0;
        int packageCount = 0;

        for (Map.Entry<String, Set<String>> entry : typesByPackage.entrySet()) {
            Set<String> packageTypes = entry.getValue();
            if (packageTypes.size() < 2) {
                continue;
            }

            // Count internal dependencies
            int internalDeps = 0;
            for (String source : packageTypes) {
                Set<String> targets = dependencies.getOrDefault(source, Set.of());
                for (String target : targets) {
                    if (packageTypes.contains(target)) {
                        internalDeps++;
                    }
                }
            }

            double h = (double) (internalDeps + 1) / packageTypes.size();
            totalH += h;
            packageCount++;
        }

        if (packageCount == 0) {
            return Metric.of(METRIC_NAME, 0.0, "ratio", "Relational cohesion (no packages with multiple types)");
        }

        double averageH = totalH / packageCount;

        return Metric.of(
                METRIC_NAME,
                averageH,
                "ratio",
                String.format("Relational cohesion: H=%.2f (average over %d packages)", averageH, packageCount),
                MetricThreshold.between(THRESHOLD_LOW, THRESHOLD_HIGH));
    }

    /**
     * Extracts the package name from a fully qualified type name.
     */
    private String extractPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }
}
