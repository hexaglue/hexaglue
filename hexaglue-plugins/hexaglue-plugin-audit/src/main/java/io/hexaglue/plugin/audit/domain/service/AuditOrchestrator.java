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

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Domain service orchestrating audit execution.
 *
 * <p>This service is responsible for:
 * <ul>
 *   <li>Coordinating constraint validation</li>
 *   <li>Coordinating metric calculation</li>
 *   <li>Determining build outcome based on violations</li>
 *   <li>Aggregating results into an AuditResult</li>
 * </ul>
 *
 * <p>The orchestrator applies the following build decision logic:
 * <ol>
 *   <li>If any BLOCKER violations exist, build outcome is FAIL</li>
 *   <li>Otherwise, build outcome is SUCCESS</li>
 * </ol>
 *
 * <p>The actual Maven build failure decision (based on errorOnBlocker,
 * errorOnCritical, failOnError) is handled by the Maven mojos, not here.
 *
 * @since 1.0.0
 */
public class AuditOrchestrator {

    private final ConstraintEngine constraintEngine;
    private final MetricAggregator metricAggregator;

    /**
     * Creates a new audit orchestrator.
     *
     * @param constraintEngine the constraint engine
     * @param metricAggregator the metric aggregator
     */
    public AuditOrchestrator(ConstraintEngine constraintEngine, MetricAggregator metricAggregator) {
        this.constraintEngine = Objects.requireNonNull(constraintEngine, "constraintEngine required");
        this.metricAggregator = Objects.requireNonNull(metricAggregator, "metricAggregator required");
    }

    /**
     * Executes a complete audit with all constraints and all metrics.
     *
     * @param model    the architectural model (may be null for legacy mode)
     * @param codebase the codebase to audit
     * @param query    architecture query for advanced analysis (may be null)
     * @return the complete audit result
     * @since 5.0.0 Added model parameter for v5 ArchType API support
     * @since 5.1.0 - Removed allowCriticalViolations (now handled by Maven mojos)
     */
    public AuditResult executeAudit(
            io.hexaglue.arch.ArchitecturalModel model, Codebase codebase, ArchitectureQuery query) {

        Objects.requireNonNull(codebase, "codebase required");

        // 1. Execute all constraints
        List<Violation> violations = constraintEngine.executeConstraints(model, codebase, query, Set.of());

        // 2. Calculate all metrics (with architecture query for rich analysis)
        Map<String, Metric> metrics = metricAggregator.calculateMetrics(model, codebase, query, Set.of());

        // 3. Determine build outcome (BLOCKER → FAIL, otherwise SUCCESS)
        BuildOutcome outcome = computeOutcome(violations);

        return new AuditResult(violations, metrics, outcome);
    }

    /**
     * Computes the build outcome based on violations.
     *
     * <p>Only BLOCKER violations cause FAIL. The finer-grained failure decision
     * (errorOnBlocker, errorOnCritical) is handled by Maven mojos.
     *
     * @param violations the list of violations
     * @return the build outcome
     */
    private BuildOutcome computeOutcome(List<Violation> violations) {
        boolean hasBlockers = violations.stream().anyMatch(v -> v.severity() == Severity.BLOCKER);
        return hasBlockers ? BuildOutcome.FAIL : BuildOutcome.SUCCESS;
    }

    /**
     * Returns all registered constraint IDs.
     *
     * @return sorted list of all constraint ID strings
     * @since 5.0.0
     * @since 5.1.0 - Always returns all constraints (no filtering)
     */
    public List<String> getExecutedConstraintIds() {
        return constraintEngine.getExecutedConstraintIds(Set.of()).stream()
                .map(io.hexaglue.plugin.audit.domain.model.ConstraintId::value)
                .sorted()
                .toList();
    }
}
