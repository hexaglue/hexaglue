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

package io.hexaglue.core.classification;

import java.util.Objects;

/**
 * Represents a conflict between classification criteria.
 *
 * <p>When multiple criteria match with different target kinds,
 * a conflict is recorded for debugging and traceability.
 *
 * <p>Conflicts have a severity level indicating whether they represent
 * an actual problem ({@link ConflictSeverity#ERROR}) or just informational
 * ({@link ConflictSeverity#WARNING}).
 *
 * @param competingKind the kind that competed with the winner
 * @param competingCriteria name of the competing criteria
 * @param competingConfidence confidence of the competing criteria
 * @param competingPriority priority of the competing criteria
 * @param rationale explanation of why this is a conflict
 * @param severity the severity level of this conflict
 */
public record Conflict(
        String competingKind,
        String competingCriteria,
        ConfidenceLevel competingConfidence,
        int competingPriority,
        String rationale,
        ConflictSeverity severity) {

    public Conflict {
        Objects.requireNonNull(competingKind, "competingKind cannot be null");
        Objects.requireNonNull(competingCriteria, "competingCriteria cannot be null");
        Objects.requireNonNull(competingConfidence, "competingConfidence cannot be null");
        Objects.requireNonNull(rationale, "rationale cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
    }

    /**
     * Creates a conflict indicating that two criteria matched for different kinds.
     *
     * <p>This method defaults to {@link ConflictSeverity#ERROR} for backward compatibility.
     *
     * @deprecated Use {@link #error} or {@link #warning} instead for explicit severity.
     */
    @Deprecated(since = "0.5.0")
    public static Conflict between(
            String kind, String criteria, ConfidenceLevel confidence, int priority, String winnerKind) {
        return new Conflict(
                kind,
                criteria,
                confidence,
                priority,
                "Matched as %s but winner is %s".formatted(kind, winnerKind),
                ConflictSeverity.ERROR);
    }

    /**
     * Creates an ERROR-level conflict for incompatible kinds.
     *
     * <p>Use this when the competing kinds cannot conceptually coexist.
     *
     * @param kind the competing kind
     * @param criteria name of the competing criteria
     * @param confidence confidence level of the competing match
     * @param priority priority of the competing criteria
     * @param rationale explanation of the conflict
     * @return a new conflict with ERROR severity
     */
    public static Conflict error(
            String kind, String criteria, ConfidenceLevel confidence, int priority, String rationale) {
        return new Conflict(kind, criteria, confidence, priority, rationale, ConflictSeverity.ERROR);
    }

    /**
     * Creates a WARNING-level conflict for compatible kinds.
     *
     * <p>Use this when the competing kinds can conceptually coexist but one was chosen.
     *
     * @param kind the competing kind
     * @param criteria name of the competing criteria
     * @param confidence confidence level of the competing match
     * @param priority priority of the competing criteria
     * @param rationale explanation of the conflict
     * @return a new conflict with WARNING severity
     */
    public static Conflict warning(
            String kind, String criteria, ConfidenceLevel confidence, int priority, String rationale) {
        return new Conflict(kind, criteria, confidence, priority, rationale, ConflictSeverity.WARNING);
    }

    /**
     * Checks if this conflict is an error (incompatible conflict).
     *
     * @return true if severity is ERROR
     */
    public boolean isError() {
        return severity == ConflictSeverity.ERROR;
    }

    /**
     * Checks if this conflict is a warning (compatible conflict).
     *
     * @return true if severity is WARNING
     */
    public boolean isWarning() {
        return severity == ConflictSeverity.WARNING;
    }
}
