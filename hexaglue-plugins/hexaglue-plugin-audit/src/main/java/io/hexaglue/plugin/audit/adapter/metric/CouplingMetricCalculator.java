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

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CouplingMetrics;
import io.hexaglue.spi.audit.RoleClassification;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates coupling metrics using ArchitectureQuery.
 *
 * <p><b>REFACTORED (v3):</b> This calculator now delegates to Core via ArchitectureQuery.
 * It only interprets the results and applies thresholds (judgment).
 *
 * <p>Principle: "Le Core produit des faits, les plugins les exploitent."
 *
 * <p>This calculator measures the average coupling between packages,
 * which represents the overall dependency health of the codebase. High
 * coupling indicates tight dependencies that can make the system harder
 * to change and understand.
 *
 * <p><strong>Metric:</strong> aggregate.coupling.efferent<br>
 * <strong>Unit:</strong> dependencies<br>
 * <strong>Threshold:</strong> Warning if &gt; 0.7 (high instability)<br>
 * <strong>Interpretation:</strong> Lower is better. Packages should have
 * balanced coupling. More than 0.7 average instability suggests the
 * architecture may need refactoring.
 *
 * @since 1.0.0
 */
public class CouplingMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "aggregate.coupling.efferent";
    private static final double WARNING_THRESHOLD = 0.7;

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates coupling metric using ArchitectureQuery when available.
     *
     * <p>When ArchitectureQuery is available, delegates to Core for accurate
     * coupling analysis. Otherwise, falls back to legacy aggregate-based
     * calculation.
     *
     * @param codebase the codebase to analyze
     * @param architectureQuery the query interface from Core (may be null)
     * @return the calculated metric
     * @since 3.0.0
     */
    @Override
    public Metric calculate(Codebase codebase, ArchitectureQuery architectureQuery) {
        if (architectureQuery != null) {
            return calculateWithArchitectureQuery(architectureQuery);
        }
        // Fallback to legacy calculation
        return calculate(codebase);
    }

    /**
     * Calculates coupling metric using Core's ArchitectureQuery.
     *
     * <p>This method leverages the Core's rich analysis capabilities:
     * <ul>
     *   <li>Uses {@link ArchitectureQuery#analyzeAllPackageCoupling()} for metrics</li>
     *   <li>Calculates average instability across all packages</li>
     *   <li>Counts problematic packages using {@link CouplingMetrics#isProblematic()}</li>
     * </ul>
     *
     * @param architectureQuery the query interface from Core
     * @return the calculated metric
     */
    private Metric calculateWithArchitectureQuery(ArchitectureQuery architectureQuery) {
        // Delegate to Core
        List<CouplingMetrics> allMetrics = architectureQuery.analyzeAllPackageCoupling();

        if (allMetrics.isEmpty()) {
            return Metric.of(
                    METRIC_NAME,
                    0.0,
                    "ratio",
                    "Average package coupling (no packages found)",
                    MetricThreshold.lessThan(WARNING_THRESHOLD));
        }

        // Calculate average instability (judgment)
        double averageCoupling = allMetrics.stream()
                .mapToDouble(CouplingMetrics::instability)
                .average()
                .orElse(0.0);

        // Count problematic packages (judgment)
        long problematicCount =
                allMetrics.stream().filter(CouplingMetrics::isProblematic).count();

        String description = String.format(
                "Average package instability: %.2f (%d of %d packages are problematic)",
                averageCoupling, problematicCount, allMetrics.size());

        return Metric.of(
                METRIC_NAME, averageCoupling, "ratio", description, MetricThreshold.lessThan(WARNING_THRESHOLD));
    }

    /**
     * Legacy calculation for aggregate coupling (fallback).
     *
     * <p>This method is retained for backward compatibility when
     * ArchitectureQuery is not available.
     *
     * @param codebase the codebase to analyze
     * @return the calculated metric
     */
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
                MetricThreshold.greaterThan(3.0));
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
        Set<String> dependencies = codebase.dependencies().getOrDefault(aggregate.qualifiedName(), Set.of());

        // Count dependencies to other aggregates (excluding self)
        return (int) dependencies.stream()
                .filter(aggregateNames::contains)
                .filter(dep -> !dep.equals(aggregate.qualifiedName()))
                .count();
    }
}
