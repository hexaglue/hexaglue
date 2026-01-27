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
 * A remediation action to fix one or more issues.
 *
 * @param priority priority order (1 = highest)
 * @param severity severity of the issues this action addresses
 * @param title short title of the action
 * @param description detailed description of what to do
 * @param effort estimated effort
 * @param impact impact of completing this action
 * @param affectedTypes fully qualified names of affected types
 * @param relatedIssues IDs of related issues
 * @since 5.0.0
 */
public record RemediationAction(
        int priority,
        Severity severity,
        String title,
        String description,
        EffortEstimate effort,
        String impact,
        List<String> affectedTypes,
        List<String> relatedIssues) {

    /**
     * Creates a remediation action with validation.
     */
    public RemediationAction {
        if (priority < 1) {
            throw new IllegalArgumentException("priority must be >= 1");
        }
        Objects.requireNonNull(severity, "severity is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(description, "description is required");
        Objects.requireNonNull(effort, "effort is required");
        Objects.requireNonNull(impact, "impact is required");
        affectedTypes = affectedTypes != null ? List.copyOf(affectedTypes) : List.of();
        relatedIssues = relatedIssues != null ? List.copyOf(relatedIssues) : List.of();
    }

    /**
     * Builder for RemediationAction.
     */
    public static class Builder {
        private int priority = 1;
        private Severity severity = Severity.MAJOR;
        private String title;
        private String description;
        private EffortEstimate effort;
        private String impact;
        private List<String> affectedTypes = List.of();
        private List<String> relatedIssues = List.of();

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder effort(EffortEstimate effort) {
            this.effort = effort;
            return this;
        }

        public Builder effort(double days, String description) {
            this.effort = new EffortEstimate(days, description);
            return this;
        }

        public Builder impact(String impact) {
            this.impact = impact;
            return this;
        }

        public Builder affectedTypes(List<String> types) {
            this.affectedTypes = types;
            return this;
        }

        public Builder relatedIssues(List<String> issues) {
            this.relatedIssues = issues;
            return this;
        }

        public RemediationAction build() {
            return new RemediationAction(
                    priority, severity, title, description, effort, impact, affectedTypes, relatedIssues);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
