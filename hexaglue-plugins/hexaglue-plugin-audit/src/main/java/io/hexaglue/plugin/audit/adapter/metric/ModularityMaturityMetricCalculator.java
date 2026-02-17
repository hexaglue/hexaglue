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
import io.hexaglue.arch.model.audit.CouplingMetrics;
import io.hexaglue.plugin.audit.adapter.validator.util.CycleDetector;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the Modularity Maturity Index (MMI) as defined by Lilienthal.
 *
 * <p>The MMI is a composite score (0-100) combining three dimensions:
 * <ul>
 *   <li><strong>Modularity (45%)</strong>: Average (1 - distanceFromMainSequence) across packages</li>
 *   <li><strong>Hierarchy (30%)</strong>: Penalty based on relative cyclicity (SCC sizes)</li>
 *   <li><strong>Pattern Consistency (25%)</strong>: Ratio of non-problematic packages</li>
 * </ul>
 *
 * <p><strong>Metric:</strong> architecture.mmi<br>
 * <strong>Unit:</strong> score<br>
 * <strong>Threshold:</strong> Warning if MMI &lt; 50<br>
 * <strong>Interpretation:</strong> Higher is better. An MMI above 70 indicates
 * a well-structured codebase. Below 50 indicates significant structural debt.
 *
 * @see <a href="https://www.oreilly.com/library/view/sustainable-software-architecture/9783960886723/">Lilienthal</a>
 * @since 5.1.0
 */
public class ModularityMaturityMetricCalculator implements MetricCalculator {

    private static final String METRIC_NAME = "architecture.mmi";
    private static final double WARNING_THRESHOLD = 50.0;
    private static final double WEIGHT_MODULARITY = 0.45;
    private static final double WEIGHT_HIERARCHY = 0.30;
    private static final double WEIGHT_PATTERN = 0.25;
    private static final double CYCLICITY_PENALTY_FACTOR = 5.0;

    private final CycleDetector cycleDetector;

    /** Creates a calculator with a default {@link CycleDetector}. */
    public ModularityMaturityMetricCalculator() {
        this.cycleDetector = new CycleDetector();
    }

    /** Creates a calculator with the given {@link CycleDetector} (for testing). */
    ModularityMaturityMetricCalculator(CycleDetector cycleDetector) {
        this.cycleDetector = cycleDetector;
    }

    @Override
    public String metricName() {
        return METRIC_NAME;
    }

    /**
     * Calculates the Modularity Maturity Index.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Modularity axis: average (1 - D) × 100 across all packages</li>
     *   <li>Hierarchy axis: 100 - relativeCyclicity × penalty, clamped to [0, 100]</li>
     *   <li>Pattern axis: (1 - problematic / total) × 100</li>
     *   <li>MMI = 0.45 × modularity + 0.30 × hierarchy + 0.25 × pattern</li>
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
            return Metric.of(METRIC_NAME, 0.0, "score", "Modularity Maturity Index (architecture query not available)");
        }

        double modularity = calculateModularityAxis(architectureQuery);
        double hierarchy = calculateHierarchyAxis(architectureQuery);
        double pattern = calculatePatternAxis(architectureQuery);

        double mmi = WEIGHT_MODULARITY * modularity + WEIGHT_HIERARCHY * hierarchy + WEIGHT_PATTERN * pattern;

        return Metric.of(
                METRIC_NAME,
                mmi,
                "score",
                String.format(
                        "MMI: %.1f (modularity=%.1f, hierarchy=%.1f, pattern=%.1f)",
                        mmi, modularity, hierarchy, pattern),
                MetricThreshold.lessThan(WARNING_THRESHOLD));
    }

    /**
     * Modularity axis: average (1 - distanceFromMainSequence) × 100.
     * Returns 100 if no package coupling data is available.
     */
    private double calculateModularityAxis(ArchitectureQuery architectureQuery) {
        List<CouplingMetrics> metrics = architectureQuery.analyzeAllPackageCoupling();
        if (metrics.isEmpty()) {
            return 100.0;
        }

        double totalScore = 0.0;
        for (CouplingMetrics cm : metrics) {
            totalScore += (1.0 - cm.distanceFromMainSequence()) * 100.0;
        }
        return totalScore / metrics.size();
    }

    /**
     * Hierarchy axis: penalizes based on relative cyclicity of SCCs.
     * Formula: max(0, 100 - relativeCyclicity × penalty_factor)
     * where relativeCyclicity = Σ(scc_size²) / N² × 100.
     */
    private double calculateHierarchyAxis(ArchitectureQuery architectureQuery) {
        Map<String, Set<String>> dependencies = architectureQuery.allTypeDependencies();
        if (dependencies.isEmpty()) {
            return 100.0;
        }

        Set<String> allTypes = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTypes::addAll);

        long n = allTypes.size();
        if (n <= 1) {
            return 100.0;
        }

        List<Set<String>> sccs = cycleDetector.findStronglyConnectedComponents(allTypes, dependencies);
        if (sccs.isEmpty()) {
            return 100.0;
        }

        long sumSquaredSizes = 0;
        for (Set<String> scc : sccs) {
            int size = scc.size();
            sumSquaredSizes += (long) size * size;
        }

        double relativeCyclicity = (double) sumSquaredSizes / (n * n) * 100.0;
        return Math.max(0.0, 100.0 - relativeCyclicity * CYCLICITY_PENALTY_FACTOR);
    }

    /**
     * Pattern consistency axis: ratio of non-problematic packages × 100.
     * Returns 100 if no package coupling data is available.
     */
    private double calculatePatternAxis(ArchitectureQuery architectureQuery) {
        List<CouplingMetrics> metrics = architectureQuery.analyzeAllPackageCoupling();
        if (metrics.isEmpty()) {
            return 100.0;
        }

        long problematic =
                metrics.stream().filter(CouplingMetrics::isProblematic).count();
        return (1.0 - (double) problematic / metrics.size()) * 100.0;
    }
}
