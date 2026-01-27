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
 *   <li>If any BLOCKER violations exist, build fails (non-overridable)</li>
 *   <li>If CRITICAL violations exist and not allowed in config, build fails</li>
 *   <li>Otherwise, build succeeds (MAJOR/MINOR/INFO are warnings)</li>
 * </ol>
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
     * Executes a complete audit.
     *
     * @param model                  the architectural model (may be null for legacy mode)
     * @param codebase               the codebase to audit
     * @param query                  architecture query for advanced analysis (may be null)
     * @param enabledConstraints     the constraints to execute (empty = all)
     * @param enabledMetrics         the metrics to calculate (empty = all)
     * @param allowCriticalViolations whether CRITICAL violations should fail the build
     * @return the complete audit result
     * @since 5.0.0 Added model parameter for v5 ArchType API support
     */
    public AuditResult executeAudit(
            io.hexaglue.arch.ArchitecturalModel model,
            Codebase codebase,
            ArchitectureQuery query,
            Set<String> enabledConstraints,
            Set<String> enabledMetrics,
            boolean allowCriticalViolations) {

        Objects.requireNonNull(codebase, "codebase required");

        // Convert string constraint IDs to ConstraintId objects
        Set<io.hexaglue.plugin.audit.domain.model.ConstraintId> constraintIds = enabledConstraints.stream()
                .map(io.hexaglue.plugin.audit.domain.model.ConstraintId::of)
                .collect(java.util.stream.Collectors.toSet());

        // 1. Execute constraints
        List<Violation> violations = constraintEngine.executeConstraints(model, codebase, query, constraintIds);

        // 2. Calculate metrics (with architecture query for rich analysis)
        Map<String, Metric> metrics = metricAggregator.calculateMetrics(model, codebase, query, enabledMetrics);

        // 3. Determine build outcome
        BuildOutcome outcome = computeOutcome(violations, allowCriticalViolations);

        return new AuditResult(violations, metrics, outcome);
    }

    /**
     * Computes the build outcome based on violations.
     *
     * @param violations              the list of violations
     * @param allowCriticalViolations whether CRITICAL violations are allowed
     * @return the build outcome
     */
    private BuildOutcome computeOutcome(List<Violation> violations, boolean allowCriticalViolations) {
        // Check for BLOCKER violations (non-overridable)
        boolean hasBlockers = violations.stream().anyMatch(v -> v.severity() == Severity.BLOCKER);

        if (hasBlockers) {
            return BuildOutcome.FAIL;
        }

        // Check for CRITICAL violations (overridable via config)
        boolean hasCritical = violations.stream().anyMatch(v -> v.severity() == Severity.CRITICAL);

        if (hasCritical && !allowCriticalViolations) {
            return BuildOutcome.FAIL;
        }

        // MAJOR, MINOR, INFO are warnings only
        return BuildOutcome.SUCCESS;
    }

    /**
     * Returns the constraint IDs that would be executed given the enabled constraints.
     *
     * <p>If enabledConstraints is empty, returns all registered constraint IDs.
     *
     * @param enabledConstraints the set of user-enabled constraint IDs (empty = all)
     * @return list of constraint ID strings that would actually be executed
     * @since 5.0.0
     */
    public List<String> getExecutedConstraintIds(Set<String> enabledConstraints) {
        Set<io.hexaglue.plugin.audit.domain.model.ConstraintId> constraintIds = enabledConstraints.stream()
                .map(io.hexaglue.plugin.audit.domain.model.ConstraintId::of)
                .collect(java.util.stream.Collectors.toSet());
        return constraintEngine.getExecutedConstraintIds(constraintIds).stream()
                .map(io.hexaglue.plugin.audit.domain.model.ConstraintId::value)
                .sorted()
                .toList();
    }
}
