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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.List;
import java.util.Objects;

/**
 * Appendix section of the report containing detailed metrics and breakdowns.
 *
 * @param scoreBreakdown detailed breakdown of the score by dimension
 * @param metrics list of all metrics
 * @param constraintsEvaluated results of all constraint evaluations
 * @param packageMetrics package-level metrics for stability analysis
 * @since 5.0.0
 */
public record Appendix(
        ScoreBreakdown scoreBreakdown,
        List<MetricEntry> metrics,
        List<ConstraintResult> constraintsEvaluated,
        List<PackageMetric> packageMetrics) {

    /**
     * Creates an appendix with validation.
     */
    public Appendix {
        Objects.requireNonNull(scoreBreakdown, "scoreBreakdown is required");
        metrics = metrics != null ? List.copyOf(metrics) : List.of();
        constraintsEvaluated = constraintsEvaluated != null ? List.copyOf(constraintsEvaluated) : List.of();
        packageMetrics = packageMetrics != null ? List.copyOf(packageMetrics) : List.of();
    }

    /**
     * Returns constraints that have violations.
     *
     * @return failed constraints
     */
    public List<ConstraintResult> failedConstraints() {
        return constraintsEvaluated.stream().filter(c -> !c.passed()).toList();
    }

    /**
     * Returns constraints that passed.
     *
     * @return passed constraints
     */
    public List<ConstraintResult> passedConstraints() {
        return constraintsEvaluated.stream().filter(ConstraintResult::passed).toList();
    }

    /**
     * Returns metrics with WARNING or CRITICAL status.
     *
     * @return problematic metrics
     */
    public List<MetricEntry> problematicMetrics() {
        return metrics.stream().filter(m -> m.status() != KpiStatus.OK).toList();
    }
}
