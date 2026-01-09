/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of an audit execution.
 *
 * <p>This record aggregates all violations found and metrics calculated
 * during an audit, along with the overall build outcome.
 *
 * @param violations the list of violations found
 * @param metrics    the map of metrics calculated (metric name -> metric)
 * @param outcome    the overall build outcome
 * @since 1.0.0
 */
public record AuditResult(List<Violation> violations, Map<String, Metric> metrics, BuildOutcome outcome) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public AuditResult {
        violations = violations != null ? List.copyOf(violations) : List.of();
        metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
        Objects.requireNonNull(outcome, "outcome required");
    }

    /**
     * Returns violations of a specific severity.
     *
     * @param severity the severity to filter by
     * @return list of violations with that severity
     */
    public List<Violation> violationsOfSeverity(Severity severity) {
        return violations.stream().filter(v -> v.severity() == severity).toList();
    }

    /**
     * Returns the count of blocker violations.
     *
     * @return the blocker count
     */
    public long blockerCount() {
        return violationsOfSeverity(Severity.BLOCKER).size();
    }

    /**
     * Returns the count of critical violations.
     *
     * @return the critical count
     */
    public long criticalCount() {
        return violationsOfSeverity(Severity.CRITICAL).size();
    }

    /**
     * Returns the count of major violations.
     *
     * @return the major count
     */
    public long majorCount() {
        return violationsOfSeverity(Severity.MAJOR).size();
    }

    /**
     * Returns true if the audit passed (outcome is SUCCESS).
     *
     * @return true if outcome is SUCCESS
     */
    public boolean passed() {
        return outcome == BuildOutcome.SUCCESS;
    }
}
