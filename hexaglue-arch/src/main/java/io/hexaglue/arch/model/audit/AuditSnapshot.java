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

package io.hexaglue.arch.model.audit;

import java.util.List;

/**
 * Complete snapshot of an audit execution.
 *
 * <p>This record represents the complete output of an audit plugin execution,
 * including the analyzed codebase, detected architecture style, violations,
 * and various quality metrics.
 *
 * @param codebase             the analyzed codebase
 * @param style                the detected architectural style
 * @param violations           all rule violations found
 * @param qualityMetrics       overall quality metrics
 * @param architectureMetrics  architecture-specific metrics
 * @param metadata             audit execution metadata
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record AuditSnapshot(
        Codebase codebase,
        DetectedArchitectureStyle style,
        List<RuleViolation> violations,
        QualityMetrics qualityMetrics,
        ArchitectureMetrics architectureMetrics,
        AuditMetadata metadata) {

    /**
     * Compact constructor with defensive copies.
     */
    public AuditSnapshot {
        violations = violations != null ? List.copyOf(violations) : List.of();
    }

    /**
     * Returns true if the audit passed (no ERROR-level violations).
     *
     * @return true if no violations have BLOCKER or CRITICAL severity
     */
    public boolean passed() {
        return violations.stream()
                .noneMatch(v -> v.severity() == Severity.BLOCKER || v.severity() == Severity.CRITICAL);
    }

    /**
     * Returns violations of a specific severity.
     *
     * @param severity the severity to filter by
     * @return list of violations with that severity
     */
    public List<RuleViolation> violationsOfSeverity(Severity severity) {
        return violations.stream().filter(v -> v.severity() == severity).toList();
    }

    /**
     * Returns error violations (BLOCKER and CRITICAL).
     *
     * @return list of BLOCKER and CRITICAL-level violations
     */
    public List<RuleViolation> errors() {
        return violations.stream()
                .filter(v -> v.severity() == Severity.BLOCKER || v.severity() == Severity.CRITICAL)
                .toList();
    }

    /**
     * Returns warning violations (MAJOR).
     *
     * @return list of MAJOR-level violations
     */
    public List<RuleViolation> warnings() {
        return violationsOfSeverity(Severity.MAJOR);
    }

    /**
     * Returns info violations.
     *
     * @return list of INFO-level violations
     */
    public List<RuleViolation> infos() {
        return violationsOfSeverity(Severity.INFO);
    }

    /**
     * Returns the count of ERROR-level violations.
     *
     * @return the error count
     */
    public long errorCount() {
        return errors().size();
    }

    /**
     * Returns the count of WARNING-level violations.
     *
     * @return the warning count
     */
    public long warningCount() {
        return warnings().size();
    }
}
