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

package io.hexaglue.plugin.audit.adapter.report.model;

/**
 * Summary statistics for the audit report.
 *
 * @param passed          whether the audit passed overall
 * @param totalViolations total number of violations found
 * @param blockers        number of blocker violations
 * @param criticals       number of critical violations
 * @param majors          number of major violations
 * @param minors          number of minor violations
 * @param infos           number of info violations
 * @since 1.0.0
 */
public record AuditSummary(
        boolean passed, int totalViolations, int blockers, int criticals, int majors, int minors, int infos) {

    public AuditSummary {
        if (totalViolations < 0) {
            throw new IllegalArgumentException("totalViolations cannot be negative");
        }
        if (blockers < 0 || criticals < 0 || majors < 0 || minors < 0 || infos < 0) {
            throw new IllegalArgumentException("severity counts cannot be negative");
        }
    }
}
