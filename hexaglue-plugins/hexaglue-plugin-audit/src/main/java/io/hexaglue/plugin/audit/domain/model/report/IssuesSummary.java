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
 * Issues section of the report containing all violations grouped by theme.
 *
 * @param summary counts of violations by severity
 * @param groups violations grouped by theme
 * @since 5.0.0
 */
public record IssuesSummary(
        ViolationCounts summary,
        List<IssueGroup> groups) {

    /**
     * Creates an issues summary with validation.
     */
    public IssuesSummary {
        Objects.requireNonNull(summary, "summary is required");
        groups = groups != null ? List.copyOf(groups) : List.of();
    }

    /**
     * Creates an empty issues summary.
     *
     * @return empty summary
     */
    public static IssuesSummary empty() {
        return new IssuesSummary(ViolationCounts.empty(), List.of());
    }

    /**
     * Returns all issues flattened from all groups.
     *
     * @return all issues
     */
    public List<IssueEntry> allIssues() {
        return groups.stream().flatMap(g -> g.violations().stream()).toList();
    }

    /**
     * Checks if there are any issues.
     *
     * @return true if total > 0
     */
    public boolean hasIssues() {
        return summary.total() > 0;
    }

    /**
     * Checks if there are any blocking issues.
     *
     * @return true if there are blockers
     */
    public boolean hasBlockers() {
        return summary.hasBlockers();
    }
}
