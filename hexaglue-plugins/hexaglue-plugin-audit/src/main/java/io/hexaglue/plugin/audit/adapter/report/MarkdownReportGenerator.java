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

import io.hexaglue.plugin.audit.adapter.report.model.ArchitectureAnalysis;
import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.MetricEntry;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Generates audit reports in GitHub-flavored Markdown format.
 *
 * <p>This generator produces Markdown that renders well on GitHub and other
 * Markdown viewers. Features include:
 * <ul>
 *   <li>Emoji indicators for status (pass/fail)</li>
 *   <li>Tables for violations and metrics</li>
 *   <li>Collapsible sections using HTML details tags</li>
 *   <li>Code formatting for technical details</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class MarkdownReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ReportFormat format() {
        return ReportFormat.MARKDOWN;
    }

    @Override
    public String generate(AuditReport report) {
        Objects.requireNonNull(report, "report required");

        StringBuilder md = new StringBuilder();

        // Title
        md.append("# HexaGlue Audit Report\n\n");

        // Status indicator
        if (report.summary().passed()) {
            md.append("## Status: ✅ PASSED\n\n");
        } else {
            md.append("## Status: ❌ FAILED\n\n");
        }

        // Metadata
        md.append("**Project:** `").append(report.metadata().projectName()).append("`  \n");
        md.append("**Timestamp:** ")
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("  \n");
        md.append("**Duration:** ").append(report.metadata().duration()).append("  \n");
        md.append("**HexaGlue Version:** ")
                .append(report.metadata().hexaglueVersion())
                .append("  \n\n");

        // Summary section
        md.append("## Summary\n\n");
        md.append("| Metric | Count |\n");
        md.append("|--------|-------|\n");
        md.append("| Total Violations | ")
                .append(report.summary().totalViolations())
                .append(" |\n");
        md.append("| Blockers | ").append(report.summary().blockers()).append(" |\n");
        md.append("| Criticals | ").append(report.summary().criticals()).append(" |\n");
        md.append("| Majors | ").append(report.summary().majors()).append(" |\n");
        md.append("| Minors | ").append(report.summary().minors()).append(" |\n");
        md.append("| Infos | ").append(report.summary().infos()).append(" |\n\n");

        // Violations section
        md.append("## Violations\n\n");
        if (report.violations().isEmpty()) {
            md.append("✅ **No violations found.**\n\n");
        } else {
            md.append("<details open>\n");
            md.append("<summary><strong>")
                    .append(report.violations().size())
                    .append(" violation(s) found</strong></summary>\n\n");

            md.append("| Severity | Constraint | Message | Affected Type | Location |\n");
            md.append("|----------|------------|---------|---------------|----------|\n");

            for (ViolationEntry v : report.violations()) {
                String emoji = getSeverityEmoji(v.severity());
                md.append("| ")
                        .append(emoji)
                        .append(" ")
                        .append(v.severity())
                        .append(" | `")
                        .append(v.constraintId())
                        .append("` | ")
                        .append(escapeMarkdown(v.message()))
                        .append(" | `")
                        .append(v.affectedType())
                        .append("` | `")
                        .append(v.location())
                        .append("` |\n");
            }

            md.append("\n</details>\n\n");
        }

        // Metrics section
        if (!report.metrics().isEmpty()) {
            md.append("## Metrics\n\n");
            md.append("<details>\n");
            md.append("<summary><strong>")
                    .append(report.metrics().size())
                    .append(" metric(s) collected</strong></summary>\n\n");

            md.append("| Metric | Value | Threshold | Status |\n");
            md.append("|--------|-------|-----------|--------|\n");

            for (MetricEntry m : report.metrics()) {
                String statusEmoji = getStatusEmoji(m.status());
                md.append("| ")
                        .append(m.name())
                        .append(" | **")
                        .append(String.format("%.2f", m.value()))
                        .append("** ")
                        .append(m.unit())
                        .append(" | ");

                if (m.threshold() != null) {
                    md.append(m.thresholdType()).append(" ").append(String.format("%.2f", m.threshold()));
                } else {
                    md.append("-");
                }

                md.append(" | ")
                        .append(statusEmoji)
                        .append(" ")
                        .append(m.status())
                        .append(" |\n");
            }

            md.append("\n</details>\n\n");
        }

        // Constraints section
        if (report.constraints().totalConstraints() > 0) {
            md.append("## Constraints Evaluated\n\n");
            md.append("<details>\n");
            md.append("<summary><strong>")
                    .append(report.constraints().totalConstraints())
                    .append(" constraint(s) checked</strong></summary>\n\n");

            for (String constraintId : report.constraints().constraintNames()) {
                md.append("- `").append(constraintId).append("`\n");
            }

            md.append("\n</details>\n\n");
        }

        // Architecture Analysis section
        appendArchitectureAnalysis(md, report.architectureAnalysis());

        // Footer
        md.append("---\n\n");
        md.append("*Generated by HexaGlue Audit Plugin v")
                .append(report.metadata().hexaglueVersion())
                .append("*\n");

        return md.toString();
    }

    /**
     * Appends architecture analysis section to the Markdown output.
     */
    private void appendArchitectureAnalysis(StringBuilder md, ArchitectureAnalysis analysis) {
        if (analysis.isClean() && analysis.couplingMetrics().isEmpty()) {
            return; // Skip section if no interesting data
        }

        md.append("## Architecture Analysis\n\n");

        // Summary
        if (analysis.isClean()) {
            md.append("✅ **No architecture issues detected.**\n\n");
        } else {
            md.append("| Issue Type | Count |\n");
            md.append("|------------|-------|\n");
            if (!analysis.typeCycles().isEmpty()) {
                md.append("| 🔄 Type Cycles | ")
                        .append(analysis.typeCycles().size())
                        .append(" |\n");
            }
            if (!analysis.packageCycles().isEmpty()) {
                md.append("| 📦 Package Cycles | ")
                        .append(analysis.packageCycles().size())
                        .append(" |\n");
            }
            if (!analysis.boundedContextCycles().isEmpty()) {
                md.append("| 🏗️ BC Cycles | ")
                        .append(analysis.boundedContextCycles().size())
                        .append(" |\n");
            }
            if (!analysis.layerViolations().isEmpty()) {
                md.append("| ⚠️ Layer Violations | ")
                        .append(analysis.layerViolations().size())
                        .append(" |\n");
            }
            if (!analysis.stabilityViolations().isEmpty()) {
                md.append("| ⚖️ Stability Violations | ")
                        .append(analysis.stabilityViolations().size())
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Dependency Cycles
        if (!analysis.typeCycles().isEmpty() || !analysis.packageCycles().isEmpty()) {
            md.append("### Dependency Cycles\n\n");

            if (!analysis.typeCycles().isEmpty()) {
                md.append("<details>\n");
                md.append("<summary><strong>🔄 ")
                        .append(analysis.typeCycles().size())
                        .append(" Type-level cycle(s)</strong></summary>\n\n");
                for (var cycle : analysis.typeCycles()) {
                    md.append("- ");
                    md.append(String.join(" → ", cycle.path()));
                    md.append("\n");
                }
                md.append("\n</details>\n\n");
            }

            if (!analysis.packageCycles().isEmpty()) {
                md.append("<details>\n");
                md.append("<summary><strong>📦 ")
                        .append(analysis.packageCycles().size())
                        .append(" Package-level cycle(s)</strong></summary>\n\n");
                for (var cycle : analysis.packageCycles()) {
                    md.append("- ");
                    md.append(String.join(" → ", cycle.path()));
                    md.append("\n");
                }
                md.append("\n</details>\n\n");
            }
        }

        // Layer Violations
        if (!analysis.layerViolations().isEmpty()) {
            md.append("### Layer Violations\n\n");
            md.append("<details open>\n");
            md.append("<summary><strong>⚠️ ")
                    .append(analysis.layerViolations().size())
                    .append(" violation(s)</strong></summary>\n\n");
            md.append("| Source | Target | From Layer | To Layer | Description |\n");
            md.append("|--------|--------|------------|----------|-------------|\n");
            for (var v : analysis.layerViolations()) {
                md.append("| `")
                        .append(v.sourceType())
                        .append("` | `")
                        .append(v.targetType())
                        .append("` | ")
                        .append(v.sourceLayer())
                        .append(" | ")
                        .append(v.targetLayer())
                        .append(" | ")
                        .append(escapeMarkdown(v.description()))
                        .append(" |\n");
            }
            md.append("\n</details>\n\n");
        }

        // Stability Violations
        if (!analysis.stabilityViolations().isEmpty()) {
            md.append("### Stability Violations (SDP)\n\n");
            md.append("<details>\n");
            md.append("<summary><strong>⚖️ ")
                    .append(analysis.stabilityViolations().size())
                    .append(" violation(s)</strong></summary>\n\n");
            md.append("| Source | Target | Source I | Target I |\n");
            md.append("|--------|--------|----------|----------|\n");
            for (var v : analysis.stabilityViolations()) {
                md.append("| `")
                        .append(v.sourceType())
                        .append("` | `")
                        .append(v.targetType())
                        .append("` | ")
                        .append(String.format("%.2f", v.sourceStability()))
                        .append(" | ")
                        .append(String.format("%.2f", v.targetStability()))
                        .append(" |\n");
            }
            md.append("\n</details>\n\n");
        }

        // Package Coupling Metrics
        if (!analysis.couplingMetrics().isEmpty()) {
            md.append("### Package Coupling Metrics\n\n");
            md.append("<details>\n");
            md.append("<summary><strong>📊 ")
                    .append(analysis.couplingMetrics().size())
                    .append(" package(s) analyzed</strong></summary>\n\n");
            md.append("| Package | Ca | Ce | I | A | D | Zones |\n");
            md.append("|---------|----|----|---|---|---|-------|\n");
            for (var m : analysis.couplingMetrics()) {
                String zones = "";
                if (m.isInZoneOfPain()) zones = "🔥 Pain";
                if (m.isInZoneOfUselessness()) zones = "🚫 Useless";
                md.append("| `")
                        .append(m.packageName())
                        .append("` | ")
                        .append(m.afferentCoupling())
                        .append(" | ")
                        .append(m.efferentCoupling())
                        .append(" | ")
                        .append(String.format("%.2f", m.instability()))
                        .append(" | ")
                        .append(String.format("%.2f", m.abstractness()))
                        .append(" | ")
                        .append(String.format("%.2f", m.distance()))
                        .append(" | ")
                        .append(zones)
                        .append(" |\n");
            }
            md.append("\n*Ca=Afferent Coupling, Ce=Efferent Coupling, I=Instability, A=Abstractness, D=Distance*\n");
            md.append("\n</details>\n\n");
        }
    }

    /**
     * Returns an emoji for the given severity level.
     */
    private String getSeverityEmoji(String severity) {
        return switch (severity.toUpperCase()) {
            case "BLOCKER", "CRITICAL" -> "🔴";
            case "ERROR" -> "❌";
            case "MAJOR" -> "🟠";
            case "WARNING" -> "⚠️";
            case "MINOR" -> "🟡";
            case "INFO" -> "ℹ️";
            default -> "⚪";
        };
    }

    /**
     * Returns an emoji for the given status.
     */
    private String getStatusEmoji(String status) {
        return switch (status.toUpperCase()) {
            case "OK" -> "✅";
            case "WARNING" -> "⚠️";
            case "CRITICAL" -> "❌";
            default -> "⚪";
        };
    }

    /**
     * Escapes Markdown special characters.
     */
    private String escapeMarkdown(String s) {
        if (s == null) {
            return "";
        }
        // Escape pipe character which breaks tables
        return s.replace("|", "\\|");
    }
}
