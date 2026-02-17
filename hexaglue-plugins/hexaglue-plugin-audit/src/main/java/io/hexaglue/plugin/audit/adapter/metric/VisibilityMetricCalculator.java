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
import io.hexaglue.arch.model.ArchType;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.syntax.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the Average Relative Visibility (ARV) across all packages.
 *
 * <p>For each package, the relative visibility (RV) is the ratio of public types
 * to total types. The ARV is the mean of all per-package RV values.
 *
 * <p>In hexagonal architecture, ports (interfaces) should be public while adapter
 * implementations should have restricted visibility. A high ARV indicates too many
 * public types, suggesting insufficient information hiding.
 *
 * <p><strong>Metric:</strong> architecture.visibility.average<br>
 * <strong>Unit:</strong> %<br>
 * <strong>Threshold:</strong> Warning if &gt; 70%<br>
 * <strong>Interpretation:</strong> Lower is better. An ARV above 70% means most
 * types are publicly exposed, reducing encapsulation and increasing coupling surface.
 *
 * @since 5.1.0
 */
public class VisibilityMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.visibility.average";
    private static final double WARNING_THRESHOLD = 70.0;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the Average Relative Visibility from the TypeRegistry.
     *
     * <p>Groups all types by package, computes per-package visibility ratio
     * (public types / total types), then averages across all packages.
     *
     * @param model the architectural model containing v5 indices
     * @param codebase the codebase for legacy access
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 5.1.0
     */
    @Override
    public Metric calculate(ArchitecturalModel model, Codebase codebase, ArchitectureQuery architectureQuery) {
        return model.typeRegistry()
                .map(registry -> {
                    List<ArchType> allTypes = registry.all().toList();

                    if (allTypes.isEmpty()) {
                        return Metric.of(METRIC_NAME, 0.0, "%", "Average visibility (no types found)");
                    }

                    // Group types by package and count public vs total
                    Map<String, int[]> packageStats = new HashMap<>();
                    for (ArchType type : allTypes) {
                        String pkg = type.id().packageName();
                        int[] stats = packageStats.computeIfAbsent(pkg, k -> new int[2]);
                        stats[0]++; // total
                        if (type.structure().modifiers().contains(Modifier.PUBLIC)) {
                            stats[1]++; // public
                        }
                    }

                    // Average the per-package visibility ratios
                    double totalRv = 0.0;
                    for (int[] stats : packageStats.values()) {
                        totalRv += (double) stats[1] / stats[0] * 100.0;
                    }
                    double arv = totalRv / packageStats.size();

                    return Metric.of(
                            METRIC_NAME,
                            arv,
                            "%",
                            String.format("Average visibility: %.1f%% across %d packages", arv, packageStats.size()),
                            MetricThreshold.greaterThan(WARNING_THRESHOLD));
                })
                .orElse(Metric.of(METRIC_NAME, 0.0, "%", "Average visibility (type registry not available)"));
    }
}
