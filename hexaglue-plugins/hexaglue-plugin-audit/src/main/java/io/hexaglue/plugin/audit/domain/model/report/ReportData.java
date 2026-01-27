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

import java.util.Objects;

/**
 * Root data structure for the audit report (JSON pivot format).
 *
 * <p>This record contains all the structured data for the audit report,
 * organized into 5 sections as per specification:
 * <ol>
 *   <li>Verdict - Score, grade, status, KPIs</li>
 *   <li>Architecture - Inventory, components, relationships</li>
 *   <li>Issues - Violations grouped by theme</li>
 *   <li>Remediation - Actions to fix issues</li>
 *   <li>Appendix - Score breakdown, metrics, constraints</li>
 * </ol>
 *
 * <p>Note: Mermaid diagrams are NOT included in this structure.
 * They are generated separately in {@link DiagramSet} and shared
 * between HTML and Markdown renderers.
 *
 * @param version schema version (e.g., "2.0")
 * @param metadata report metadata (project name, timestamp, etc.)
 * @param verdict section 1 - audit verdict with score and status
 * @param architecture section 2 - architecture overview and inventory
 * @param issues section 3 - issues found grouped by theme
 * @param remediation section 4 - remediation actions
 * @param appendix section 5 - detailed breakdowns and metrics
 * @since 5.0.0
 */
public record ReportData(
        String version,
        ReportMetadata metadata,
        Verdict verdict,
        ArchitectureOverview architecture,
        IssuesSummary issues,
        RemediationPlan remediation,
        Appendix appendix) {

    /** Current schema version. */
    public static final String CURRENT_VERSION = "2.0";

    /**
     * Creates report data with validation.
     */
    public ReportData {
        Objects.requireNonNull(version, "version is required");
        Objects.requireNonNull(metadata, "metadata is required");
        Objects.requireNonNull(verdict, "verdict is required");
        Objects.requireNonNull(architecture, "architecture is required");
        Objects.requireNonNull(issues, "issues is required");
        Objects.requireNonNull(remediation, "remediation is required");
        Objects.requireNonNull(appendix, "appendix is required");
    }

    /**
     * Creates report data with the current schema version.
     *
     * @param metadata report metadata
     * @param verdict verdict section
     * @param architecture architecture section
     * @param issues issues section
     * @param remediation remediation section
     * @param appendix appendix section
     * @return report data
     */
    public static ReportData create(
            ReportMetadata metadata,
            Verdict verdict,
            ArchitectureOverview architecture,
            IssuesSummary issues,
            RemediationPlan remediation,
            Appendix appendix) {
        return new ReportData(CURRENT_VERSION, metadata, verdict, architecture, issues, remediation, appendix);
    }

    /**
     * Builder for ReportData.
     */
    public static class Builder {
        private String version = CURRENT_VERSION;
        private ReportMetadata metadata;
        private Verdict verdict;
        private ArchitectureOverview architecture;
        private IssuesSummary issues;
        private RemediationPlan remediation;
        private Appendix appendix;

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder metadata(ReportMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder verdict(Verdict verdict) {
            this.verdict = verdict;
            return this;
        }

        public Builder architecture(ArchitectureOverview architecture) {
            this.architecture = architecture;
            return this;
        }

        public Builder issues(IssuesSummary issues) {
            this.issues = issues;
            return this;
        }

        public Builder remediation(RemediationPlan remediation) {
            this.remediation = remediation;
            return this;
        }

        public Builder appendix(Appendix appendix) {
            this.appendix = appendix;
            return this;
        }

        public ReportData build() {
            return new ReportData(version, metadata, verdict, architecture, issues, remediation, appendix);
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

    /**
     * Returns the overall score from the verdict.
     *
     * @return overall score (0-100)
     */
    public int score() {
        return verdict.score();
    }

    /**
     * Returns whether the audit passed.
     *
     * @return true if passed
     */
    public boolean passed() {
        return verdict.status() == ReportStatus.PASSED || verdict.status() == ReportStatus.PASSED_WITH_WARNINGS;
    }

    /**
     * Returns whether there are any issues.
     *
     * @return true if there are issues
     */
    public boolean hasIssues() {
        return issues.hasIssues();
    }

    /**
     * Returns the total number of issues.
     *
     * @return issue count
     */
    public int issueCount() {
        return issues.summary().total();
    }
}
