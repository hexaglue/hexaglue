/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
 * Generates audit reports in HTML format.
 *
 * <p>This generator produces a self-contained HTML document with embedded CSS.
 * The report is styled professionally with:
 * <ul>
 *   <li>Color-coded severity badges</li>
 *   <li>Summary cards showing key metrics</li>
 *   <li>Sortable tables for violations and metrics</li>
 *   <li>Responsive design that works on all screen sizes</li>
 *   <li>Print-friendly layout</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class HtmlReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ReportFormat format() {
        return ReportFormat.HTML;
    }

    @Override
    public String generate(AuditReport report) {
        Objects.requireNonNull(report, "report required");

        StringBuilder html = new StringBuilder();

        // HTML header
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>HexaGlue Audit Report - ")
                .append(escape(report.metadata().projectName()))
                .append("</title>\n");
        html.append("  <style>\n");
        html.append(getEmbeddedCss());
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("  <header>\n");
        html.append("    <h1>HexaGlue Audit Report</h1>\n");
        html.append("    <div class=\"metadata\">\n");
        html.append("      <span><strong>Project:</strong> ")
                .append(escape(report.metadata().projectName()))
                .append("</span>\n");
        html.append("      <span><strong>Timestamp:</strong> ")
                .append(TIMESTAMP_FORMATTER.format(
                        report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("</span>\n");
        html.append("      <span><strong>Duration:</strong> ")
                .append(escape(report.metadata().duration()))
                .append("</span>\n");
        html.append("      <span><strong>HexaGlue:</strong> ")
                .append(escape(report.metadata().hexaglueVersion()))
                .append("</span>\n");
        html.append("    </div>\n");
        html.append("  </header>\n");

        // Summary cards
        html.append("  <section class=\"summary-cards\">\n");
        html.append("    <div class=\"card ")
                .append(report.summary().passed() ? "pass" : "fail")
                .append("\">\n");
        html.append("      <h2>")
                .append(report.summary().passed() ? "PASSED" : "FAILED")
                .append("</h2>\n");
        html.append("      <p>Audit Status</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>")
                .append(report.summary().totalViolations())
                .append("</h2>\n");
        html.append("      <p>Total Violations</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card error-card\">\n");
        html.append("      <h2>")
                .append(report.summary().blockers())
                .append("</h2>\n");
        html.append("      <p>Blockers</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card warning-card\">\n");
        html.append("      <h2>")
                .append(report.summary().majors())
                .append("</h2>\n");
        html.append("      <p>Majors</p>\n");
        html.append("    </div>\n");
        html.append("  </section>\n");

        // Violations section
        html.append("  <section>\n");
        html.append("    <h2>Violations</h2>\n");
        if (report.violations().isEmpty()) {
            html.append("    <p class=\"no-violations\">No violations found.</p>\n");
        } else {
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Severity</th>\n");
            html.append("          <th>Constraint</th>\n");
            html.append("          <th>Message</th>\n");
            html.append("          <th>Affected Type</th>\n");
            html.append("          <th>Location</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (ViolationEntry v : report.violations()) {
                html.append("        <tr>\n");
                html.append("          <td><span class=\"badge badge-")
                        .append(v.severity().toLowerCase())
                        .append("\">")
                        .append(escape(v.severity()))
                        .append("</span></td>\n");
                html.append("          <td><code>")
                        .append(escape(v.constraintId()))
                        .append("</code></td>\n");
                html.append("          <td>").append(escape(v.message())).append("</td>\n");
                html.append("          <td><code>")
                        .append(escape(v.affectedType()))
                        .append("</code></td>\n");
                html.append("          <td><small>")
                        .append(escape(v.location()))
                        .append("</small></td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
        }
        html.append("  </section>\n");

        // Metrics section
        if (!report.metrics().isEmpty()) {
            html.append("  <section>\n");
            html.append("    <h2>Metrics</h2>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Metric</th>\n");
            html.append("          <th>Value</th>\n");
            html.append("          <th>Threshold</th>\n");
            html.append("          <th>Status</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (MetricEntry m : report.metrics()) {
                html.append("        <tr>\n");
                html.append("          <td>").append(escape(m.name())).append("</td>\n");
                html.append("          <td><strong>")
                        .append(String.format("%.2f", m.value()))
                        .append("</strong> ")
                        .append(escape(m.unit()))
                        .append("</td>\n");
                html.append("          <td>");
                if (m.threshold() != null) {
                    html.append(m.thresholdType())
                            .append(" ")
                            .append(String.format("%.2f", m.threshold()));
                } else {
                    html.append("-");
                }
                html.append("</td>\n");
                html.append("          <td><span class=\"badge badge-")
                        .append(m.status().toLowerCase())
                        .append("\">")
                        .append(escape(m.status()))
                        .append("</span></td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
            html.append("  </section>\n");
        }

        // Architecture Analysis section
        appendArchitectureAnalysis(html, report.architectureAnalysis());

        // Footer
        html.append("  <footer>\n");
        html.append("    <p>Generated by HexaGlue Audit Plugin v")
                .append(escape(report.metadata().hexaglueVersion()))
                .append("</p>\n");
        html.append("  </footer>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Appends architecture analysis section to HTML.
     */
    private void appendArchitectureAnalysis(StringBuilder html, ArchitectureAnalysis analysis) {
        if (analysis.isClean() && analysis.couplingMetrics().isEmpty()) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Architecture Analysis</h2>\n");

        // Summary cards for architecture issues
        if (!analysis.isClean()) {
            html.append("    <div class=\"summary-cards\" style=\"margin-bottom: 20px;\">\n");
            if (analysis.totalCycles() > 0) {
                html.append("      <div class=\"card warning-card\">\n");
                html.append("        <h2>").append(analysis.totalCycles()).append("</h2>\n");
                html.append("        <p>Dependency Cycles</p>\n");
                html.append("      </div>\n");
            }
            if (analysis.totalViolations() > 0) {
                html.append("      <div class=\"card error-card\">\n");
                html.append("        <h2>").append(analysis.totalViolations()).append("</h2>\n");
                html.append("        <p>Architecture Violations</p>\n");
                html.append("      </div>\n");
            }
            html.append("    </div>\n");
        }

        // Dependency Cycles
        if (!analysis.typeCycles().isEmpty()) {
            html.append("    <h3>Type-level Cycles</h3>\n");
            html.append("    <ul>\n");
            for (var cycle : analysis.typeCycles()) {
                html.append("      <li><code>")
                        .append(escape(String.join(" → ", cycle.path())))
                        .append("</code></li>\n");
            }
            html.append("    </ul>\n");
        }

        if (!analysis.packageCycles().isEmpty()) {
            html.append("    <h3>Package-level Cycles</h3>\n");
            html.append("    <ul>\n");
            for (var cycle : analysis.packageCycles()) {
                html.append("      <li><code>")
                        .append(escape(String.join(" → ", cycle.path())))
                        .append("</code></li>\n");
            }
            html.append("    </ul>\n");
        }

        // Layer Violations
        if (!analysis.layerViolations().isEmpty()) {
            html.append("    <h3>Layer Violations</h3>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Source</th>\n");
            html.append("          <th>Target</th>\n");
            html.append("          <th>From Layer</th>\n");
            html.append("          <th>To Layer</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (var v : analysis.layerViolations()) {
                html.append("        <tr>\n");
                html.append("          <td><code>").append(escape(v.sourceType())).append("</code></td>\n");
                html.append("          <td><code>").append(escape(v.targetType())).append("</code></td>\n");
                html.append("          <td><span class=\"badge badge-info\">")
                        .append(escape(v.sourceLayer()))
                        .append("</span></td>\n");
                html.append("          <td><span class=\"badge badge-warning\">")
                        .append(escape(v.targetLayer()))
                        .append("</span></td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
        }

        // Stability Violations
        if (!analysis.stabilityViolations().isEmpty()) {
            html.append("    <h3>Stability Violations (SDP)</h3>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Source</th>\n");
            html.append("          <th>Target</th>\n");
            html.append("          <th>Source I</th>\n");
            html.append("          <th>Target I</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (var v : analysis.stabilityViolations()) {
                html.append("        <tr>\n");
                html.append("          <td><code>").append(escape(v.sourceType())).append("</code></td>\n");
                html.append("          <td><code>").append(escape(v.targetType())).append("</code></td>\n");
                html.append("          <td>").append(String.format("%.2f", v.sourceStability())).append("</td>\n");
                html.append("          <td>").append(String.format("%.2f", v.targetStability())).append("</td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
        }

        // Package Coupling Metrics
        if (!analysis.couplingMetrics().isEmpty()) {
            html.append("    <h3>Package Coupling Metrics</h3>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Package</th>\n");
            html.append("          <th>Ca</th>\n");
            html.append("          <th>Ce</th>\n");
            html.append("          <th>Instability (I)</th>\n");
            html.append("          <th>Abstractness (A)</th>\n");
            html.append("          <th>Distance (D)</th>\n");
            html.append("          <th>Status</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (var m : analysis.couplingMetrics()) {
                html.append("        <tr>\n");
                html.append("          <td><code>").append(escape(m.packageName())).append("</code></td>\n");
                html.append("          <td>").append(m.afferentCoupling()).append("</td>\n");
                html.append("          <td>").append(m.efferentCoupling()).append("</td>\n");
                html.append("          <td>").append(String.format("%.2f", m.instability())).append("</td>\n");
                html.append("          <td>").append(String.format("%.2f", m.abstractness())).append("</td>\n");
                html.append("          <td>").append(String.format("%.2f", m.distance())).append("</td>\n");
                html.append("          <td>");
                if (m.isInZoneOfPain()) {
                    html.append("<span class=\"badge badge-error\">Zone of Pain</span>");
                } else if (m.isInZoneOfUselessness()) {
                    html.append("<span class=\"badge badge-warning\">Zone of Uselessness</span>");
                } else {
                    html.append("<span class=\"badge badge-ok\">OK</span>");
                }
                html.append("</td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
            html.append("    <p><small><em>Ca=Afferent Coupling, Ce=Efferent Coupling</em></small></p>\n");
        }

        html.append("  </section>\n");
    }

    /**
     * Returns embedded CSS styles.
     */
    private String getEmbeddedCss() {
        return """
            * {
              box-sizing: border-box;
              margin: 0;
              padding: 0;
            }

            body {
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
              line-height: 1.6;
              color: #333;
              background-color: #f5f5f5;
              padding: 20px;
            }

            header {
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
              padding: 30px;
              border-radius: 8px;
              margin-bottom: 30px;
              box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            }

            header h1 {
              font-size: 2em;
              margin-bottom: 15px;
            }

            .metadata {
              display: flex;
              flex-wrap: wrap;
              gap: 20px;
              font-size: 0.9em;
            }

            .summary-cards {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
              gap: 20px;
              margin-bottom: 30px;
            }

            .card {
              background: white;
              border-radius: 8px;
              padding: 25px;
              text-align: center;
              box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
              transition: transform 0.2s;
            }

            .card:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
            }

            .card h2 {
              font-size: 2.5em;
              margin-bottom: 10px;
            }

            .card.pass h2 {
              color: #10b981;
            }

            .card.fail h2 {
              color: #ef4444;
            }

            .error-card h2 {
              color: #ef4444;
            }

            .warning-card h2 {
              color: #f59e0b;
            }

            section {
              background: white;
              border-radius: 8px;
              padding: 25px;
              margin-bottom: 20px;
              box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            }

            section h2 {
              margin-bottom: 20px;
              color: #667eea;
              border-bottom: 2px solid #667eea;
              padding-bottom: 10px;
            }

            .no-violations {
              color: #10b981;
              font-weight: bold;
              padding: 20px;
              text-align: center;
              font-size: 1.2em;
            }

            table {
              width: 100%;
              border-collapse: collapse;
            }

            th, td {
              padding: 12px;
              text-align: left;
              border-bottom: 1px solid #e5e7eb;
            }

            th {
              background-color: #f9fafb;
              font-weight: 600;
              color: #374151;
            }

            tr:hover {
              background-color: #f9fafb;
            }

            code {
              background-color: #f3f4f6;
              padding: 2px 6px;
              border-radius: 3px;
              font-family: "Monaco", "Courier New", monospace;
              font-size: 0.9em;
            }

            .badge {
              display: inline-block;
              padding: 4px 12px;
              border-radius: 12px;
              font-size: 0.85em;
              font-weight: 600;
              text-transform: uppercase;
            }

            .badge-error, .badge-blocker {
              background-color: #fee2e2;
              color: #991b1b;
            }

            .badge-warning, .badge-major {
              background-color: #fef3c7;
              color: #92400e;
            }

            .badge-info, .badge-minor {
              background-color: #dbeafe;
              color: #1e40af;
            }

            .badge-ok {
              background-color: #d1fae5;
              color: #065f46;
            }

            .badge-critical {
              background-color: #fecaca;
              color: #7f1d1d;
            }

            footer {
              text-align: center;
              padding: 20px;
              color: #6b7280;
              font-size: 0.9em;
            }

            @media print {
              body {
                background: white;
                padding: 0;
              }

              .card:hover {
                transform: none;
              }
            }
            """;
    }

    /**
     * Escapes HTML special characters.
     */
    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
