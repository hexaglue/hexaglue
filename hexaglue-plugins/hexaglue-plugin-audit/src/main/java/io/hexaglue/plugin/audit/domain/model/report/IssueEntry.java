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

import io.hexaglue.plugin.audit.domain.model.Severity;
import java.util.Objects;

/**
 * An individual issue/violation entry in the report.
 *
 * <p>Each issue entry contains all information needed to understand and fix
 * a violation, including the business impact and detailed remediation steps.
 *
 * @param id unique identifier for this issue (e.g., "aggregate-cycle-1")
 * @param constraintId identifier of the violated constraint (e.g., "ddd:aggregate-cycle")
 * @param severity severity level
 * @param title short title describing the issue
 * @param message detailed message explaining the violation
 * @param location source location where the issue was detected
 * @param impact business impact of this violation (MANDATORY)
 * @param suggestion how to fix this issue (MANDATORY)
 * @since 5.0.0
 */
public record IssueEntry(
        String id,
        String constraintId,
        Severity severity,
        String title,
        String message,
        SourceLocation location,
        String impact,
        Suggestion suggestion) {

    /**
     * Creates an issue entry with validation.
     * Impact and suggestion are mandatory as per specification.
     */
    public IssueEntry {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(constraintId, "constraintId is required");
        Objects.requireNonNull(severity, "severity is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(message, "message is required");
        Objects.requireNonNull(location, "location is required");
        Objects.requireNonNull(impact, "impact is mandatory for each issue");
        Objects.requireNonNull(suggestion, "suggestion is mandatory for each issue");
    }

    /**
     * Checks if this issue is a blocker.
     *
     * @return true if severity is BLOCKER
     */
    public boolean isBlocker() {
        return severity == Severity.BLOCKER;
    }

    /**
     * Checks if this issue is critical or blocker.
     *
     * @return true if severity is CRITICAL or BLOCKER
     */
    public boolean isCriticalOrBlocker() {
        return severity == Severity.BLOCKER || severity == Severity.CRITICAL;
    }

    /**
     * Returns the anchor reference for linking.
     *
     * @return anchor reference (e.g., "#aggregate-cycle-1")
     */
    public String anchor() {
        return "#" + id;
    }
}
