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
import java.util.List;
import java.util.Objects;

/**
 * A group of related issues organized by theme.
 *
 * @param id unique identifier for this group (e.g., "domain-model")
 * @param theme display name of the theme (e.g., "Domain Model Issues")
 * @param icon icon identifier (e.g., "domain", "ports")
 * @param description description of what issues in this group represent
 * @param count number of issues in this group
 * @param violations list of issues in this group
 * @since 5.0.0
 */
public record IssueGroup(
        String id,
        String theme,
        String icon,
        String description,
        int count,
        List<IssueEntry> violations) {

    /**
     * Creates an issue group with validation.
     */
    public IssueGroup {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(theme, "theme is required");
        Objects.requireNonNull(icon, "icon is required");
        Objects.requireNonNull(description, "description is required");
        violations = violations != null ? List.copyOf(violations) : List.of();
        if (count < 0) {
            count = violations.size();
        }
    }

    /**
     * Creates an issue group from a list of violations.
     *
     * @param id group id
     * @param theme theme name
     * @param icon icon
     * @param description description
     * @param violations list of violations
     * @return the issue group
     */
    public static IssueGroup of(String id, String theme, String icon, String description, List<IssueEntry> violations) {
        return new IssueGroup(id, theme, icon, description, violations.size(), violations);
    }

    /**
     * Returns the count of issues with the given severity.
     *
     * @param severity severity to count
     * @return count
     */
    public long countBySeverity(Severity severity) {
        return violations.stream().filter(v -> v.severity() == severity).count();
    }

    /**
     * Checks if this group has any blockers.
     *
     * @return true if there are BLOCKER issues
     */
    public boolean hasBlockers() {
        return countBySeverity(Severity.BLOCKER) > 0;
    }

    /**
     * Checks if this group has any critical issues.
     *
     * @return true if there are CRITICAL issues
     */
    public boolean hasCriticals() {
        return countBySeverity(Severity.CRITICAL) > 0;
    }

    /**
     * Returns a summary of severities in this group.
     *
     * @return formatted summary (e.g., "1 BLOCKER, 2 MAJOR")
     */
    public String severitySummary() {
        var sb = new StringBuilder();
        for (Severity s : Severity.values()) {
            long count = countBySeverity(s);
            if (count > 0) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }
                sb.append(count).append(" ").append(s.name());
            }
        }
        return sb.toString();
    }
}
