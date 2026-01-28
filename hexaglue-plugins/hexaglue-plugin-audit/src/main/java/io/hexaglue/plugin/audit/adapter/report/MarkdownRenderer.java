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

package io.hexaglue.plugin.audit.adapter.report;

import io.hexaglue.plugin.audit.domain.model.report.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders audit reports to GitHub-flavored Markdown format.
 *
 * <p>The Markdown output includes embedded Mermaid diagrams for visualization
 * in GitHub, GitLab, and other platforms that support Mermaid rendering.
 *
 * @since 5.0.0
 */
public class MarkdownRenderer implements ReportRenderer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ReportFormat format() {
        return ReportFormat.MARKDOWN;
    }

    @Override
    public String render(ReportData data) {
        return render(data, null);
    }

    @Override
    public String render(ReportData data, DiagramSet diagrams) {
        StringBuilder md = new StringBuilder();

        renderHeader(md, data.metadata());
        renderVerdict(md, data.verdict(), diagrams);
        renderArchitecture(md, data.architecture(), diagrams);
        renderIssues(md, data.issues(), diagrams);
        renderRemediation(md, data.remediation());
        renderAppendix(md, data.appendix(), diagrams);
        renderFooter(md, data.metadata());

        return md.toString();
    }

    private void renderHeader(StringBuilder md, ReportMetadata metadata) {
        md.append("# ").append(metadata.projectName()).append(" - Architecture Audit Report\n\n");
        md.append("> **Version:** ").append(metadata.projectVersion()).append("  \n");
        md.append("> **Date:** ")
                .append(DATE_FORMAT.format(metadata.timestamp().atZone(ZoneId.systemDefault())))
                .append("  \n");
        md.append("> **Duration:** ").append(metadata.duration()).append("\n\n");
        md.append("---\n\n");
    }

    private void renderVerdict(StringBuilder md, Verdict verdict, DiagramSet diagrams) {
        md.append("## 1. Verdict\n\n");

        // Score badge
        md.append("| Score | Status | Grade |\n");
        md.append("|:-----:|:------:|:-----:|\n");
        md.append("| **")
                .append(verdict.score())
                .append("/100** | **")
                .append(verdict.status())
                .append("** | **")
                .append(verdict.grade())
                .append("** |\n\n");

        md.append(verdict.summary()).append("\n\n");

        // KPIs table
        if (!verdict.kpis().isEmpty()) {
            md.append("### Key Performance Indicators\n\n");
            md.append("| Dimension | Score | **Contribution** (Weight) | Status |\n");
            md.append("|-----------|------:|-------------------------:|:------:|\n");
            double totalContribution = 0;
            int totalWeight = 0;
            for (KPI kpi : verdict.kpis()) {
                String statusIcon =
                        switch (kpi.status()) {
                            case OK -> "\u2705";
                            case WARNING -> "\u26A0\uFE0F";
                            case CRITICAL -> "\u274C";
                        };
                double contribution = kpi.contribution();
                totalContribution += contribution;
                totalWeight += kpi.weight();
                md.append("| ")
                        .append(kpi.name())
                        .append(" | ")
                        .append(String.format(Locale.ROOT, "%.0f", kpi.value()))
                        .append(kpi.unit())
                        .append(" | **")
                        .append(String.format(Locale.ROOT, "%.1f", contribution))
                        .append("** (")
                        .append(kpi.weight())
                        .append("%)")
                        .append(" | ")
                        .append(statusIcon)
                        .append(" |\n");
            }
            // TOTAL row
            String totalStatusIcon =
                    switch (verdict.status()) {
                        case PASSED -> "\u2705";
                        case PASSED_WITH_WARNINGS -> "\u26A0\uFE0F";
                        case FAILED -> "\u274C";
                    };
            md.append("| **TOTAL** | | **")
                    .append(String.format(Locale.ROOT, "%.1f", totalContribution))
                    .append("** (")
                    .append(totalWeight)
                    .append("%)")
                    .append(" | ")
                    .append(totalStatusIcon)
                    .append(" |\n");
            md.append("\n");
        }

        // Score radar
        if (diagrams != null && diagrams.scoreRadar() != null) {
            md.append("### Score Radar\n\n");
            md.append("```mermaid\n");
            md.append(diagrams.scoreRadar());
            md.append("\n```\n\n");
        }

        // Immediate action
        if (verdict.immediateAction().required()) {
            md.append("> **\u26A0\uFE0F Immediate Action Required:** ")
                    .append(verdict.immediateAction().message())
                    .append(" [See issue](")
                    .append(verdict.immediateAction().reference())
                    .append(")\n\n");
        }
    }

    private void renderArchitecture(StringBuilder md, ArchitectureOverview arch, DiagramSet diagrams) {
        md.append("## 2. Architecture Overview\n\n");
        md.append(arch.summary()).append("\n\n");

        // Inventory
        md.append("### Component Inventory\n\n");
        InventoryTotals totals = arch.inventory().totals();
        md.append("| Component Type | Count |\n");
        md.append("|----------------|------:|\n");
        md.append("| Aggregate Roots | ").append(totals.aggregates()).append(" |\n");
        md.append("| Entities | ").append(totals.entities()).append(" |\n");
        md.append("| Value Objects | ").append(totals.valueObjects()).append(" |\n");
        md.append("| Identifiers | ").append(totals.identifiers()).append(" |\n");
        md.append("| Domain Events | ").append(totals.domainEvents()).append(" |\n");
        md.append("| Driving Ports | ").append(totals.drivingPorts()).append(" |\n");
        md.append("| Driven Ports | ").append(totals.drivenPorts()).append(" |\n\n");

        // C4 Context diagram
        if (diagrams != null && diagrams.c4Context() != null) {
            md.append("### System Context (C4)\n\n");
            md.append("```mermaid\n");
            md.append(diagrams.c4Context());
            md.append("\n```\n\n");
        }

        // C4 Component diagram
        if (diagrams != null && diagrams.c4Component() != null) {
            md.append("### Component Diagram (C4)\n\n");
            md.append("```mermaid\n");
            md.append(diagrams.c4Component());
            md.append("\n```\n\n");
        }

        // Domain Model - one diagram per aggregate
        if (diagrams != null) {
            md.append("### Domain Model\n\n");
            if (!diagrams.aggregateDiagrams().isEmpty()) {
                for (DiagramSet.AggregateDiagram aggDiagram : diagrams.aggregateDiagrams()) {
                    md.append("#### ").append(aggDiagram.aggregateName());
                    if (aggDiagram.hasErrors()) {
                        md.append(" :warning: **Cycle detected**");
                    }
                    md.append("\n\n");
                    md.append("```mermaid\n");
                    md.append(aggDiagram.diagram());
                    md.append("\n```\n\n");
                }
            } else if (diagrams.domainModel() != null) {
                md.append("```mermaid\n");
                md.append(diagrams.domainModel());
                md.append("\n```\n\n");
            }

            // Application Layer diagram (optional, since 5.0.0)
            if (diagrams.applicationLayer() != null) {
                md.append("### Application Layer\n\n");
                md.append("```mermaid\n");
                md.append(diagrams.applicationLayer());
                md.append("\n```\n\n");
            }

            // Ports Layer diagram (optional, since 5.0.0)
            if (diagrams.portsLayer() != null) {
                md.append("### Ports Layer\n\n");
                md.append("```mermaid\n");
                md.append(diagrams.portsLayer());
                md.append("\n```\n\n");
            }

            // Full Architecture diagram (optional, since 5.0.0)
            if (diagrams.fullArchitecture() != null) {
                md.append("### Full Architecture Overview\n\n");
                md.append("```mermaid\n");
                md.append(diagrams.fullArchitecture());
                md.append("\n```\n\n");
            }
        }
    }

    private void renderIssues(StringBuilder md, IssuesSummary issues, DiagramSet diagrams) {
        md.append("## 3. Issues\n\n");

        ViolationCounts counts = issues.summary();
        if (counts.total() == 0) {
            md.append("No issues detected.\n\n");
            return;
        }

        md.append("**Total Issues:** ").append(counts.total()).append("\n\n");

        // Severity breakdown
        md.append("| Severity | Count |\n");
        md.append("|----------|------:|\n");
        if (counts.blockers() > 0) {
            md.append("| \uD83D\uDED1 BLOCKER | ").append(counts.blockers()).append(" |\n");
        }
        if (counts.criticals() > 0) {
            md.append("| \uD83D\uDD34 CRITICAL | ").append(counts.criticals()).append(" |\n");
        }
        if (counts.majors() > 0) {
            md.append("| \uD83D\uDFE0 MAJOR | ").append(counts.majors()).append(" |\n");
        }
        if (counts.minors() > 0) {
            md.append("| \uD83D\uDFE1 MINOR | ").append(counts.minors()).append(" |\n");
        }
        if (counts.infos() > 0) {
            md.append("| \uD83D\uDD35 INFO | ").append(counts.infos()).append(" |\n");
        }
        md.append("\n");

        // Violations pie chart
        if (diagrams != null && diagrams.violationsPie() != null) {
            md.append("### Violations Distribution\n\n");
            md.append("```mermaid\n");
            md.append(diagrams.violationsPie());
            md.append("\n```\n\n");
        }

        // Issue groups
        for (IssueGroup group : issues.groups()) {
            md.append("### ").append(group.theme()).append("\n\n");
            md.append(group.description()).append("\n\n");

            for (IssueEntry issue : group.violations()) {
                renderIssue(md, issue);
            }
        }
    }

    private void renderIssue(StringBuilder md, IssueEntry issue) {
        String severityBadge =
                switch (issue.severity()) {
                    case BLOCKER -> "\uD83D\uDED1";
                    case CRITICAL -> "\uD83D\uDD34";
                    case MAJOR -> "\uD83D\uDFE0";
                    case MINOR -> "\uD83D\uDFE1";
                    case INFO -> "\uD83D\uDD35";
                };

        // Issue header
        md.append("#### ")
                .append(severityBadge)
                .append(" ")
                .append(issue.title())
                .append(" `")
                .append(issue.id())
                .append("`\n\n");

        md.append("**Message:** ").append(issue.message()).append("\n\n");
        md.append("**Location:** `").append(issue.location().file());
        if (issue.location().lineOpt().isPresent()) {
            md.append(":").append(issue.location().lineOpt().get());
        }
        md.append("`\n\n");

        md.append("**Impact:** ").append(issue.impact()).append("\n\n");

        Suggestion suggestion = issue.suggestion();
        md.append("**Fix:** ").append(suggestion.action()).append("\n\n");

        if (!suggestion.steps().isEmpty()) {
            md.append("**Steps:**\n");
            for (int i = 0; i < suggestion.steps().size(); i++) {
                md.append(i + 1).append(". ").append(suggestion.steps().get(i)).append("\n");
            }
            md.append("\n");
        }

        if (suggestion.codeExampleOpt().isPresent()) {
            md.append("**Example:**\n```java\n");
            md.append(suggestion.codeExampleOpt().get());
            md.append("\n```\n\n");
        }

        suggestion
                .effortOpt()
                .ifPresent(e -> md.append("**Effort:** ").append(e).append("\n\n"));

        md.append("---\n\n");
    }

    private void renderRemediation(StringBuilder md, RemediationPlan remediation) {
        md.append("## 4. Remediation Plan\n\n");

        if (remediation.actions().isEmpty()) {
            md.append("No remediation actions required.\n\n");
            return;
        }

        md.append(remediation.summary()).append("\n\n");

        // Effort summary table
        TotalEffort effort = remediation.totalEffort();
        md.append("| | Manual | With HexaGlue | Savings |\n");
        md.append("|---|-------:|-------:|-------:|\n");
        md.append("| **Effort** | ")
                .append(String.format(Locale.ROOT, "%.1f", effort.days()))
                .append(" days");
        md.append(" | ")
                .append(String.format(Locale.ROOT, "%.1f", effort.effectiveDays()))
                .append(" days");
        md.append(" | ")
                .append(String.format(Locale.ROOT, "%.1f", effort.hexaglueSavingsDays()))
                .append(" days |\n");
        if (effort.costOpt().isPresent()) {
            CostEstimate manualCost = effort.cost();
            md.append("| **Cost** | ")
                    .append(String.format(Locale.ROOT, "%.0f", manualCost.amount()))
                    .append(" ")
                    .append(manualCost.currency());
            effort.effectiveCostOpt().ifPresent(c -> md.append(" | ")
                    .append(String.format(Locale.ROOT, "%.0f", c.amount()))
                    .append(" ")
                    .append(c.currency()));
            effort.hexaglueSavingsCostOpt().ifPresent(c -> md.append(" | ")
                    .append(String.format(Locale.ROOT, "%.0f", c.amount()))
                    .append(" ")
                    .append(c.currency())
                    .append(" |"));
            md.append("\n");
        }
        md.append("\n");

        // Actions table with Manual / HexaGlue / Plugin columns
        md.append("| Action | Manual | HexaGlue | Plugin |\n");
        md.append("|--------|-------:|-------:|:------:|\n");
        for (RemediationAction action : remediation.actions()) {
            md.append("| ").append(action.title());
            if (action.isAutomatableByHexaglue()) {
                md.append(" | ~~")
                        .append(String.format(
                                Locale.ROOT, "%.1f", action.effort().days()))
                        .append("d~~");
                md.append(" | **0d**");
                md.append(" | `")
                        .append(extractPluginShortName(action.hexagluePlugin()))
                        .append("` |\n");
            } else {
                md.append(" | ")
                        .append(String.format(
                                Locale.ROOT, "%.1f", action.effort().days()))
                        .append("d");
                md.append(" | ")
                        .append(String.format(
                                Locale.ROOT, "%.1f", action.effort().days()))
                        .append("d");
                md.append(" | — |\n");
            }
        }
        md.append("| **TOTAL** | ")
                .append(String.format(Locale.ROOT, "%.1f", effort.days()))
                .append("d");
        md.append(" | **")
                .append(String.format(Locale.ROOT, "%.1f", effort.effectiveDays()))
                .append("d** | |\n");
        md.append("\n");

        // Detailed actions
        for (RemediationAction action : remediation.actions()) {
            md.append("### ")
                    .append(action.priority())
                    .append(". ")
                    .append(action.title())
                    .append("\n\n");
            md.append(action.description()).append("\n\n");
            md.append("**Impact:** ").append(action.impact()).append("\n\n");

            if (!action.affectedTypes().isEmpty()) {
                md.append("**Affected Types:**\n");
                for (String type : action.affectedTypes()) {
                    md.append("- `").append(type).append("`\n");
                }
                md.append("\n");
            }
        }
    }

    private String extractPluginShortName(String pluginName) {
        if (pluginName == null) return "";
        if (pluginName.startsWith("hexaglue-plugin-")) {
            return pluginName.substring("hexaglue-plugin-".length());
        }
        return pluginName;
    }

    private void renderAppendix(StringBuilder md, Appendix appendix, DiagramSet diagrams) {
        md.append("## 5. Appendix\n\n");

        // Score breakdown
        md.append("### Score Breakdown\n\n");
        ScoreBreakdown breakdown = appendix.scoreBreakdown();
        md.append("| Dimension | Weight | Score | Contribution |\n");
        md.append("|-----------|-------:|------:|-------------:|\n");
        renderDimensionRow(md, "DDD Compliance", breakdown.dddCompliance());
        renderDimensionRow(md, "Hexagonal Architecture", breakdown.hexagonalCompliance());
        renderDimensionRow(md, "Dependencies", breakdown.dependencyQuality());
        renderDimensionRow(md, "Coupling", breakdown.couplingMetrics());
        renderDimensionRow(md, "Cohesion", breakdown.cohesionQuality());
        md.append("\n");

        // Metrics
        if (!appendix.metrics().isEmpty()) {
            md.append("### Metrics\n\n");
            md.append("| Metric | Value | Status |\n");
            md.append("|--------|------:|:------:|\n");
            for (MetricEntry metric : appendix.metrics()) {
                String status =
                        switch (metric.status()) {
                            case OK -> "\u2705";
                            case WARNING -> "\u26A0\uFE0F";
                            case CRITICAL -> "\u274C";
                        };
                md.append("| ")
                        .append(metric.name())
                        .append(" | ")
                        .append(String.format(Locale.ROOT, "%.2f", metric.value()))
                        .append(" ")
                        .append(metric.unit())
                        .append(" | ")
                        .append(status)
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Package zones diagram
        if (diagrams != null && diagrams.packageZones() != null) {
            md.append("### Package Stability Analysis\n\n");
            md.append("```mermaid\n");
            md.append(diagrams.packageZones());
            md.append("\n```\n\n");
        }

        // Package metrics table
        if (!appendix.packageMetrics().isEmpty()) {
            md.append("### Package Metrics\n\n");
            md.append("| Package | Ca | Ce | I | A | D | Zone |\n");
            md.append("|---------|---:|---:|--:|--:|--:|------|\n");
            for (PackageMetric pm : appendix.packageMetrics()) {
                md.append("| ")
                        .append(shortenPackage(pm.packageName()))
                        .append(" | ")
                        .append(pm.ca())
                        .append(" | ")
                        .append(pm.ce())
                        .append(" | ")
                        .append(String.format(Locale.ROOT, "%.2f", pm.instability()))
                        .append(" | ")
                        .append(String.format(Locale.ROOT, "%.2f", pm.abstractness()))
                        .append(" | ")
                        .append(String.format(Locale.ROOT, "%.2f", pm.distance()))
                        .append(" | ")
                        .append(pm.zone().label())
                        .append(" |\n");
            }
            md.append("\n");
        }
    }

    private void renderDimensionRow(StringBuilder md, String name, ScoreDimension dim) {
        int contribution = (int) Math.round(dim.weight() * dim.score() / 100.0);
        md.append("| ")
                .append(name)
                .append(" | ")
                .append(dim.weight())
                .append("%")
                .append(" | ")
                .append(dim.score())
                .append("/100")
                .append(" | ")
                .append(contribution)
                .append(" |\n");
    }

    private void renderFooter(StringBuilder md, ReportMetadata metadata) {
        md.append("---\n\n");
        md.append("*Generated by [HexaGlue](https://hexaglue.io) ")
                .append(metadata.hexaglueVersion())
                .append(" | Plugin version: ")
                .append(metadata.pluginVersion())
                .append("*\n");
    }

    private String shortenPackage(String packageName) {
        if (packageName == null) return "";
        String[] parts = packageName.split("\\.");
        if (parts.length <= 3) return packageName;
        return "..." + parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}
