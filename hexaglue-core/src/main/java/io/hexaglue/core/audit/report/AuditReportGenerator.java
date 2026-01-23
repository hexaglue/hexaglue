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

package io.hexaglue.core.audit.report;

import io.hexaglue.spi.audit.AuditSnapshot;
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.audit.Severity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generator for audit reports in various formats.
 *
 * <p>This class provides methods to generate audit reports in different formats:
 * console (plain text), HTML, JSON, and Markdown. Each format is suitable for
 * different use cases:
 * <ul>
 *   <li><b>Console</b>: Quick viewing in terminal/CI logs</li>
 *   <li><b>HTML</b>: Rich viewing in browsers with styling</li>
 *   <li><b>JSON</b>: Machine-readable for tooling integration</li>
 *   <li><b>Markdown</b>: Documentation in repositories</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class AuditReportGenerator {

    /**
     * Generates a plain text console report.
     *
     * @param snapshot the audit snapshot
     * @return formatted console report
     */
    public String generateConsole(AuditSnapshot snapshot) {
        StringBuilder report = new StringBuilder();

        // Header
        report.append("=".repeat(80)).append("\n");
        report.append("HEXAGLUE AUDIT REPORT\n");
        report.append("=".repeat(80)).append("\n");
        report.append("Generated: ").append(snapshot.metadata().timestamp()).append("\n");
        report.append("Project: ").append(snapshot.codebase().name()).append("\n");
        report.append("HexaGlue Version: ")
                .append(snapshot.metadata().hexaglueVersion())
                .append("\n");
        report.append("Analysis Duration: ")
                .append(snapshot.metadata().formattedDuration())
                .append("\n");
        report.append("\n");

        // Summary
        report.append("SUMMARY\n");
        report.append("-".repeat(80)).append("\n");
        report.append("Total Violations: ").append(snapshot.violations().size()).append("\n");
        report.append("  Errors:   ").append(snapshot.errorCount()).append("\n");
        report.append("  Warnings: ").append(snapshot.warningCount()).append("\n");
        report.append("  Info:     ").append(snapshot.infos().size()).append("\n");
        report.append("Status: ")
                .append(snapshot.passed() ? "PASSED" : "FAILED")
                .append("\n");
        report.append("\n");

        // Quality Metrics
        report.append("QUALITY METRICS\n");
        report.append("-".repeat(80)).append("\n");
        var metrics = snapshot.qualityMetrics();
        report.append("Test Coverage:          ")
                .append(String.format("%.1f%%", metrics.testCoverage()))
                .append("\n");
        report.append("Documentation Coverage: ")
                .append(String.format("%.1f%%", metrics.documentationCoverage()))
                .append("\n");
        report.append("Technical Debt:         ")
                .append(metrics.technicalDebtMinutes())
                .append(" minutes\n");
        report.append("Maintainability Rating: ")
                .append(String.format("%.1f/5.0", metrics.maintainabilityRating()))
                .append("\n");
        report.append("\n");

        // Violations by Severity
        if (!snapshot.violations().isEmpty()) {
            report.append("VIOLATIONS\n");
            report.append("-".repeat(80)).append("\n");

            Map<Severity, List<RuleViolation>> bySeverity =
                    snapshot.violations().stream().collect(Collectors.groupingBy(RuleViolation::severity));

            for (Severity severity : Severity.values()) {
                List<RuleViolation> violations = bySeverity.getOrDefault(severity, List.of());
                if (!violations.isEmpty()) {
                    report.append("\n")
                            .append(severity)
                            .append(" (")
                            .append(violations.size())
                            .append(")\n");
                    report.append("-".repeat(80)).append("\n");

                    for (RuleViolation violation : violations) {
                        report.append("  [").append(violation.ruleId()).append("]\n");
                        report.append("  Location: ")
                                .append(violation.location().toIdeFormat())
                                .append("\n");
                        report.append("  ").append(violation.message()).append("\n");
                        report.append("\n");
                    }
                }
            }
        }

        report.append("=".repeat(80)).append("\n");
        return report.toString();
    }

    /**
     * Generates an HTML report with styling.
     *
     * @param snapshot the audit snapshot
     * @return formatted HTML report
     */
    public String generateHtml(AuditSnapshot snapshot) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>HexaGlue Audit Report</title>\n");
        html.append("  <style>\n");
        html.append(getHtmlStyles());
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("  <header>\n");
        html.append("    <h1>HexaGlue Audit Report</h1>\n");
        html.append("    <p class=\"metadata\">");
        html.append("Generated: ").append(snapshot.metadata().timestamp());
        html.append(" | Project: ").append(escapeHtml(snapshot.codebase().name()));
        html.append(" | HexaGlue v").append(escapeHtml(snapshot.metadata().hexaglueVersion()));
        html.append("</p>\n");
        html.append("  </header>\n");

        // Summary
        html.append("  <section class=\"summary\">\n");
        html.append("    <h2>Summary</h2>\n");
        html.append("    <div class=\"status ")
                .append(snapshot.passed() ? "passed" : "failed")
                .append("\">\n");
        html.append("      Status: ")
                .append(snapshot.passed() ? "PASSED" : "FAILED")
                .append("\n");
        html.append("    </div>\n");
        html.append("    <table>\n");
        html.append("      <tr><td>Total Violations:</td><td>")
                .append(snapshot.violations().size())
                .append("</td></tr>\n");
        html.append("      <tr><td>Errors:</td><td class=\"error\">")
                .append(snapshot.errorCount())
                .append("</td></tr>\n");
        html.append("      <tr><td>Warnings:</td><td class=\"warning\">")
                .append(snapshot.warningCount())
                .append("</td></tr>\n");
        html.append("      <tr><td>Info:</td><td class=\"info\">")
                .append(snapshot.infos().size())
                .append("</td></tr>\n");
        html.append("    </table>\n");
        html.append("  </section>\n");

        // Quality Metrics
        html.append("  <section class=\"metrics\">\n");
        html.append("    <h2>Quality Metrics</h2>\n");
        html.append("    <table>\n");
        var metrics = snapshot.qualityMetrics();
        html.append("      <tr><td>Test Coverage:</td><td>")
                .append(String.format("%.1f%%", metrics.testCoverage()))
                .append("</td></tr>\n");
        html.append("      <tr><td>Documentation Coverage:</td><td>")
                .append(String.format("%.1f%%", metrics.documentationCoverage()))
                .append("</td></tr>\n");
        html.append("      <tr><td>Technical Debt:</td><td>")
                .append(metrics.technicalDebtMinutes())
                .append(" minutes</td></tr>\n");
        html.append("      <tr><td>Maintainability Rating:</td><td>")
                .append(String.format("%.1f/5.0", metrics.maintainabilityRating()))
                .append("</td></tr>\n");
        html.append("    </table>\n");
        html.append("  </section>\n");

        // Violations
        if (!snapshot.violations().isEmpty()) {
            html.append("  <section class=\"violations\">\n");
            html.append("    <h2>Violations</h2>\n");

            Map<Severity, List<RuleViolation>> bySeverity =
                    snapshot.violations().stream().collect(Collectors.groupingBy(RuleViolation::severity));

            for (Severity severity : Severity.values()) {
                List<RuleViolation> violations = bySeverity.getOrDefault(severity, List.of());
                if (!violations.isEmpty()) {
                    html.append("    <h3 class=\"")
                            .append(severity.name().toLowerCase())
                            .append("\">");
                    html.append(severity).append(" (").append(violations.size()).append(")</h3>\n");
                    html.append("    <ul>\n");

                    for (RuleViolation violation : violations) {
                        html.append("      <li class=\"violation\">\n");
                        html.append("        <strong>[")
                                .append(escapeHtml(violation.ruleId()))
                                .append("]</strong><br>\n");
                        html.append("        <code>")
                                .append(escapeHtml(violation.location().toIdeFormat()))
                                .append("</code><br>\n");
                        html.append("        ")
                                .append(escapeHtml(violation.message()))
                                .append("\n");
                        html.append("      </li>\n");
                    }

                    html.append("    </ul>\n");
                }
            }

            html.append("  </section>\n");
        }

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Generates a JSON report for machine processing.
     *
     * @param snapshot the audit snapshot
     * @return formatted JSON report
     */
    public String generateJson(AuditSnapshot snapshot) {
        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"metadata\": {\n");
        json.append("    \"timestamp\": \"")
                .append(snapshot.metadata().timestamp())
                .append("\",\n");
        json.append("    \"projectName\": \"")
                .append(escapeJson(snapshot.codebase().name()))
                .append("\",\n");
        json.append("    \"hexaglueVersion\": \"")
                .append(escapeJson(snapshot.metadata().hexaglueVersion()))
                .append("\"\n");
        json.append("  },\n");
        json.append("  \"summary\": {\n");
        json.append("    \"passed\": ").append(snapshot.passed()).append(",\n");
        json.append("    \"totalViolations\": ")
                .append(snapshot.violations().size())
                .append(",\n");
        json.append("    \"errors\": ").append(snapshot.errorCount()).append(",\n");
        json.append("    \"warnings\": ").append(snapshot.warningCount()).append(",\n");
        json.append("    \"info\": ").append(snapshot.infos().size()).append("\n");
        json.append("  },\n");
        json.append("  \"qualityMetrics\": {\n");
        var metrics = snapshot.qualityMetrics();
        json.append("    \"testCoverage\": ").append(metrics.testCoverage()).append(",\n");
        json.append("    \"documentationCoverage\": ")
                .append(metrics.documentationCoverage())
                .append(",\n");
        json.append("    \"technicalDebtMinutes\": ")
                .append(metrics.technicalDebtMinutes())
                .append(",\n");
        json.append("    \"maintainabilityRating\": ")
                .append(metrics.maintainabilityRating())
                .append("\n");
        json.append("  },\n");
        json.append("  \"violations\": [\n");

        List<RuleViolation> violations = snapshot.violations();
        for (int i = 0; i < violations.size(); i++) {
            RuleViolation v = violations.get(i);
            json.append("    {\n");
            json.append("      \"ruleId\": \"").append(escapeJson(v.ruleId())).append("\",\n");
            json.append("      \"severity\": \"").append(v.severity()).append("\",\n");
            json.append("      \"message\": \"").append(escapeJson(v.message())).append("\",\n");
            json.append("      \"location\": \"")
                    .append(escapeJson(v.location().toIdeFormat()))
                    .append("\"\n");
            json.append("    }");
            if (i < violations.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * Generates a Markdown report for documentation.
     *
     * @param snapshot the audit snapshot
     * @return formatted Markdown report
     */
    public String generateMarkdown(AuditSnapshot snapshot) {
        StringBuilder md = new StringBuilder();

        // Header
        md.append("# HexaGlue Audit Report\n\n");
        md.append("**Generated:** ").append(snapshot.metadata().timestamp()).append("  \n");
        md.append("**Project:** ").append(snapshot.codebase().name()).append("  \n");
        md.append("**HexaGlue Version:** ")
                .append(snapshot.metadata().hexaglueVersion())
                .append("  \n");
        md.append("**Status:** ");
        if (snapshot.passed()) {
            md.append("✅ PASSED\n\n");
        } else {
            md.append("❌ FAILED\n\n");
        }

        // Summary
        md.append("## Summary\n\n");
        md.append("| Metric | Count |\n");
        md.append("|--------|-------|\n");
        md.append("| Total Violations | ").append(snapshot.violations().size()).append(" |\n");
        md.append("| Errors | ").append(snapshot.errorCount()).append(" |\n");
        md.append("| Warnings | ").append(snapshot.warningCount()).append(" |\n");
        md.append("| Info | ").append(snapshot.infos().size()).append(" |\n\n");

        // Quality Metrics
        md.append("## Quality Metrics\n\n");
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        var metrics = snapshot.qualityMetrics();
        md.append("| Test Coverage | ")
                .append(String.format("%.1f%%", metrics.testCoverage()))
                .append(" |\n");
        md.append("| Documentation Coverage | ")
                .append(String.format("%.1f%%", metrics.documentationCoverage()))
                .append(" |\n");
        md.append("| Technical Debt | ").append(metrics.technicalDebtMinutes()).append(" minutes |\n");
        md.append("| Maintainability Rating | ")
                .append(String.format("%.1f/5.0", metrics.maintainabilityRating()))
                .append(" |\n\n");

        // Violations
        if (!snapshot.violations().isEmpty()) {
            md.append("## Violations\n\n");

            Map<Severity, List<RuleViolation>> bySeverity =
                    snapshot.violations().stream().collect(Collectors.groupingBy(RuleViolation::severity));

            for (Severity severity : Severity.values()) {
                List<RuleViolation> violations = bySeverity.getOrDefault(severity, List.of());
                if (!violations.isEmpty()) {
                    String icon =
                            switch (severity) {
                                case BLOCKER -> "🛑";
                                case CRITICAL -> "❌";
                                case MAJOR -> "⚠️";
                                case MINOR -> "📝";
                                case INFO -> "ℹ️";
                            };

                    md.append("### ").append(icon).append(" ").append(severity);
                    md.append(" (").append(violations.size()).append(")\n\n");

                    for (RuleViolation violation : violations) {
                        md.append("- **[").append(violation.ruleId()).append("]** ");
                        md.append("`")
                                .append(violation.location().toIdeFormat())
                                .append("`  \n");
                        md.append("  ").append(violation.message()).append("\n\n");
                    }
                }
            }
        }

        return md.toString();
    }

    // Helper methods

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String getHtmlStyles() {
        return """
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
            header { background: #2c3e50; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
            header h1 { margin: 0 0 10px 0; }
            .metadata { opacity: 0.8; font-size: 14px; }
            section { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            h2 { color: #2c3e50; margin-top: 0; border-bottom: 2px solid #ecf0f1; padding-bottom: 10px; }
            h3 { margin-top: 20px; }
            table { width: 100%; border-collapse: collapse; }
            table td { padding: 8px; border-bottom: 1px solid #ecf0f1; }
            table td:first-child { font-weight: bold; width: 200px; }
            .status { padding: 10px; border-radius: 4px; font-weight: bold; text-align: center; margin: 10px 0; }
            .status.passed { background: #d4edda; color: #155724; }
            .status.failed { background: #f8d7da; color: #721c24; }
            .error { color: #e74c3c; font-weight: bold; }
            .warning { color: #f39c12; font-weight: bold; }
            .info { color: #3498db; font-weight: bold; }
            .violations ul { list-style: none; padding: 0; }
            .violation { background: #f8f9fa; padding: 15px; margin: 10px 0; border-left: 4px solid #3498db; border-radius: 4px; }
            .violation code { background: #e9ecef; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
            """;
    }
}
