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
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory;
import io.hexaglue.plugin.audit.adapter.report.model.ExecutiveSummary;
import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.adapter.report.model.MetricEntry;
import io.hexaglue.plugin.audit.adapter.report.model.PortMatrixEntry;
import io.hexaglue.plugin.audit.adapter.report.model.TechnicalDebtSummary;
import io.hexaglue.plugin.audit.adapter.report.model.ViolationEntry;
import io.hexaglue.plugin.audit.domain.model.Recommendation;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
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
        // Health Score card
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>").append(report.healthScore().overall()).append("<small>/100</small></h2>\n");
        html.append("      <p>Health Score (")
                .append(report.healthScore().grade())
                .append(")</p>\n");
        html.append("    </div>\n");
        // DDD Compliance
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>").append(report.dddCompliancePercent()).append("<small>%</small></h2>\n");
        html.append("      <p>DDD Compliance</p>\n");
        html.append("    </div>\n");
        // Hex Compliance
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>").append(report.hexCompliancePercent()).append("<small>%</small></h2>\n");
        html.append("      <p>Hexagonal Compliance</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>").append(report.summary().totalViolations()).append("</h2>\n");
        html.append("      <p>Total Violations</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card error-card\">\n");
        html.append("      <h2>").append(report.summary().blockers()).append("</h2>\n");
        html.append("      <p>Blockers</p>\n");
        html.append("    </div>\n");
        html.append("  </section>\n");

        // Executive Summary
        appendExecutiveSummary(html, report.executiveSummary());

        // Health Score Details
        appendHealthScore(html, report.healthScore());

        // Component Inventory
        appendComponentInventory(html, report.inventory());

        // Port Matrix
        appendPortMatrix(html, report.portMatrix());

        // Technical Debt
        appendTechnicalDebt(html, report.technicalDebt());

        // Recommendations
        appendRecommendations(html, report.recommendations());

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
                    html.append(m.thresholdType()).append(" ").append(String.format("%.2f", m.threshold()));
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
                html.append("          <td><code>")
                        .append(escape(v.sourceType()))
                        .append("</code></td>\n");
                html.append("          <td><code>")
                        .append(escape(v.targetType()))
                        .append("</code></td>\n");
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
                html.append("          <td><code>")
                        .append(escape(v.sourceType()))
                        .append("</code></td>\n");
                html.append("          <td><code>")
                        .append(escape(v.targetType()))
                        .append("</code></td>\n");
                html.append("          <td>")
                        .append(String.format("%.2f", v.sourceStability()))
                        .append("</td>\n");
                html.append("          <td>")
                        .append(String.format("%.2f", v.targetStability()))
                        .append("</td>\n");
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
                html.append("          <td><code>")
                        .append(escape(m.packageName()))
                        .append("</code></td>\n");
                html.append("          <td>").append(m.afferentCoupling()).append("</td>\n");
                html.append("          <td>").append(m.efferentCoupling()).append("</td>\n");
                html.append("          <td>")
                        .append(String.format("%.2f", m.instability()))
                        .append("</td>\n");
                html.append("          <td>")
                        .append(String.format("%.2f", m.abstractness()))
                        .append("</td>\n");
                html.append("          <td>")
                        .append(String.format("%.2f", m.distance()))
                        .append("</td>\n");
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

            .card h2 small {
              font-size: 0.4em;
              color: #6b7280;
            }

            .verdict-box {
              background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
              border-left: 4px solid #667eea;
              padding: 20px;
              margin-bottom: 20px;
              border-radius: 4px;
            }

            .verdict-box p {
              font-size: 1.1em;
              line-height: 1.6;
            }

            .strengths-list, .actions-list {
              padding-left: 20px;
              margin: 15px 0;
            }

            .strengths-list li, .actions-list li {
              padding: 8px 0;
              color: #374151;
            }

            .strengths-list li::marker {
              color: #10b981;
            }

            .score-overview {
              margin-bottom: 25px;
            }

            .score-main {
              display: flex;
              align-items: baseline;
              margin-bottom: 10px;
            }

            .score-value {
              font-size: 3em;
              font-weight: bold;
              color: #667eea;
            }

            .score-label {
              font-size: 1.2em;
              color: #6b7280;
              margin-left: 5px;
            }

            .progress-bar {
              height: 20px;
              background-color: #e5e7eb;
              border-radius: 10px;
              overflow: hidden;
            }

            .progress-fill {
              height: 100%;
              background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
              border-radius: 10px;
              transition: width 0.5s ease;
            }

            .recommendation-card {
              background: #f9fafb;
              border-radius: 8px;
              padding: 20px;
              margin-bottom: 15px;
              border-left: 4px solid #667eea;
            }

            .recommendation-card h3 {
              margin-bottom: 10px;
              display: flex;
              align-items: center;
              gap: 10px;
            }

            .recommendation-card p {
              margin: 8px 0;
            }

            section h3 {
              margin: 20px 0 15px 0;
              color: #374151;
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
     * Appends executive summary section.
     */
    private void appendExecutiveSummary(StringBuilder html, ExecutiveSummary summary) {
        if (summary == null || summary.verdict() == null) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Executive Summary</h2>\n");

        // Verdict
        html.append("    <div class=\"verdict-box\">\n");
        html.append("      <p>").append(escape(summary.verdict())).append("</p>\n");
        html.append("    </div>\n");

        // Strengths
        if (summary.strengths() != null && !summary.strengths().isEmpty()) {
            html.append("    <h3>Key Strengths</h3>\n");
            html.append("    <ul class=\"strengths-list\">\n");
            for (String strength : summary.strengths()) {
                html.append("      <li>").append(escape(strength)).append("</li>\n");
            }
            html.append("    </ul>\n");
        }

        // Concerns
        if (summary.concerns() != null && !summary.concerns().isEmpty()) {
            html.append("    <h3>Areas of Concern</h3>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Severity</th>\n");
            html.append("          <th>Description</th>\n");
            html.append("          <th>Count</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (ExecutiveSummary.ConcernEntry concern : summary.concerns()) {
                html.append("        <tr>\n");
                html.append("          <td><span class=\"badge badge-")
                        .append(concern.severity().toLowerCase())
                        .append("\">")
                        .append(escape(concern.severity()))
                        .append("</span></td>\n");
                html.append("          <td>")
                        .append(escape(concern.description()))
                        .append("</td>\n");
                html.append("          <td><strong>").append(concern.count()).append("</strong></td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
        }

        // KPIs
        if (summary.kpis() != null && !summary.kpis().isEmpty()) {
            html.append("    <h3>Key Performance Indicators</h3>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Indicator</th>\n");
            html.append("          <th>Value</th>\n");
            html.append("          <th>Threshold</th>\n");
            html.append("          <th>Status</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");
            for (ExecutiveSummary.KpiEntry kpi : summary.kpis()) {
                html.append("        <tr>\n");
                html.append("          <td>").append(escape(kpi.name())).append("</td>\n");
                html.append("          <td><strong>")
                        .append(escape(kpi.value()))
                        .append("</strong></td>\n");
                html.append("          <td>").append(escape(kpi.threshold())).append("</td>\n");
                html.append("          <td><span class=\"badge badge-")
                        .append(kpi.status().toLowerCase())
                        .append("\">")
                        .append(escape(kpi.status()))
                        .append("</span></td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </tbody>\n");
            html.append("    </table>\n");
        }

        // Immediate Actions
        if (summary.immediateActions() != null && !summary.immediateActions().isEmpty()) {
            html.append("    <h3>Immediate Actions Required</h3>\n");
            html.append("    <ol class=\"actions-list\">\n");
            for (String action : summary.immediateActions()) {
                html.append("      <li>").append(escape(action)).append("</li>\n");
            }
            html.append("    </ol>\n");
        }

        html.append("  </section>\n");
    }

    /**
     * Appends health score details section.
     */
    private void appendHealthScore(StringBuilder html, HealthScore score) {
        if (score == null) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Health Score Breakdown</h2>\n");

        // Overall score with progress bar
        html.append("    <div class=\"score-overview\">\n");
        html.append("      <div class=\"score-main\">\n");
        html.append("        <span class=\"score-value\">")
                .append(score.overall())
                .append("</span>\n");
        html.append("        <span class=\"score-label\">/100 (Grade ")
                .append(score.grade())
                .append(")</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"progress-bar\">\n");
        html.append("        <div class=\"progress-fill\" style=\"width: ")
                .append(score.overall())
                .append("%;\"></div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Component scores
        html.append("    <table>\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th>Component</th>\n");
        html.append("          <th>Score</th>\n");
        html.append("          <th>Weight</th>\n");
        html.append("          <th>Status</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        appendScoreRow(html, "DDD Compliance", score.dddCompliance(), "25%");
        appendScoreRow(html, "Hexagonal Compliance", score.hexCompliance(), "25%");
        appendScoreRow(html, "Dependency Quality", score.dependencyQuality(), "20%");
        appendScoreRow(html, "Coupling", score.coupling(), "15%");
        appendScoreRow(html, "Cohesion", score.cohesion(), "15%");

        html.append("      </tbody>\n");
        html.append("    </table>\n");
        html.append("  </section>\n");
    }

    /**
     * Helper to append a score row in the health score table.
     */
    private void appendScoreRow(StringBuilder html, String name, int score, String weight) {
        String status = score >= 80 ? "ok" : score >= 60 ? "warning" : "error";
        html.append("        <tr>\n");
        html.append("          <td>").append(name).append("</td>\n");
        html.append("          <td><strong>").append(score).append("%</strong></td>\n");
        html.append("          <td>").append(weight).append("</td>\n");
        html.append("          <td><span class=\"badge badge-")
                .append(status)
                .append("\">")
                .append(score >= 80 ? "Good" : score >= 60 ? "Fair" : "Poor")
                .append("</span></td>\n");
        html.append("        </tr>\n");
    }

    /**
     * Appends component inventory section.
     */
    private void appendComponentInventory(StringBuilder html, ComponentInventory inventory) {
        if (inventory == null) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Component Inventory</h2>\n");

        // Domain Model subsection
        html.append("    <h3>Domain Model</h3>\n");
        html.append("    <div class=\"summary-cards\" style=\"margin-bottom: 20px;\">\n");

        appendInventoryCard(html, "Aggregate Roots", inventory.aggregateRoots());
        appendInventoryCard(html, "Entities", inventory.entities());
        appendInventoryCard(html, "Value Objects", inventory.valueObjects());
        appendInventoryCard(html, "Domain Events", inventory.domainEvents());
        appendInventoryCard(html, "Domain Services", inventory.domainServices());
        appendInventoryCard(html, "Application Services", inventory.applicationServices());

        html.append("    </div>\n");

        // Ports subsection
        html.append("    <h3>Ports</h3>\n");
        html.append("    <div class=\"summary-cards\" style=\"margin-bottom: 20px;\">\n");
        html.append("      <div class=\"card\">\n");
        html.append("        <h2>").append(inventory.drivingPorts()).append("</h2>\n");
        html.append("        <p>Driving Ports</p>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"card\">\n");
        html.append("        <h2>").append(inventory.drivenPorts()).append("</h2>\n");
        html.append("        <p>Driven Ports</p>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Totals
        html.append("    <p><strong>Total Domain Types:</strong> ")
                .append(inventory.totalDomainTypes())
                .append("</p>\n");
        html.append("    <p><strong>Total Ports:</strong> ")
                .append(inventory.totalPorts())
                .append("</p>\n");

        html.append("  </section>\n");
    }

    /**
     * Helper to append an inventory card.
     */
    private void appendInventoryCard(StringBuilder html, String label, int count) {
        html.append("      <div class=\"card\">\n");
        html.append("        <h2>").append(count).append("</h2>\n");
        html.append("        <p>").append(label).append("</p>\n");
        html.append("      </div>\n");
    }

    /**
     * Appends port matrix section.
     */
    private void appendPortMatrix(StringBuilder html, List<PortMatrixEntry> portMatrix) {
        if (portMatrix == null || portMatrix.isEmpty()) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Port Matrix</h2>\n");
        html.append("    <table>\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th>Port Name</th>\n");
        html.append("          <th>Direction</th>\n");
        html.append("          <th>Kind</th>\n");
        html.append("          <th>Managed Type</th>\n");
        html.append("          <th>Methods</th>\n");
        html.append("          <th>Adapter</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        for (PortMatrixEntry port : portMatrix) {
            html.append("        <tr>\n");
            html.append("          <td><code>").append(escape(port.portName())).append("</code></td>\n");
            html.append("          <td><span class=\"badge badge-")
                    .append("DRIVING".equals(port.direction()) ? "ok" : "info")
                    .append("\">")
                    .append(escape(port.direction()))
                    .append("</span></td>\n");
            html.append("          <td>").append(escape(port.kind())).append("</td>\n");
            html.append("          <td><code>")
                    .append(escape(port.managedType() != null ? port.managedType() : "-"))
                    .append("</code></td>\n");
            html.append("          <td>").append(port.methodCount()).append("</td>\n");
            html.append("          <td>");
            if (port.hasAdapter()) {
                html.append("<span class=\"badge badge-ok\">Yes</span>");
            } else {
                html.append("<span class=\"badge badge-warning\">No</span>");
            }
            html.append("</td>\n");
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");
        html.append("  </section>\n");
    }

    /**
     * Appends technical debt section.
     */
    private void appendTechnicalDebt(StringBuilder html, TechnicalDebtSummary debt) {
        if (debt == null || debt.totalDays() == 0) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Technical Debt</h2>\n");

        // Summary cards
        html.append("    <div class=\"summary-cards\" style=\"margin-bottom: 20px;\">\n");
        html.append("      <div class=\"card\">\n");
        html.append("        <h2>")
                .append(String.format("%.1f", debt.totalDays()))
                .append("<small> days</small></h2>\n");
        html.append("        <p>Total Debt</p>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"card\">\n");
        html.append("        <h2>")
                .append(String.format("%.0f", debt.totalCost()))
                .append("<small> €</small></h2>\n");
        html.append("        <p>Estimated Cost</p>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"card\">\n");
        html.append("        <h2>")
                .append(String.format("%.0f", debt.monthlyInterest()))
                .append("<small> €/month</small></h2>\n");
        html.append("        <p>Monthly Interest</p>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Breakdown table
        if (debt.breakdown() != null && !debt.breakdown().isEmpty()) {
            html.append("    <h3>Debt by Category</h3>\n");
            html.append("    <table>\n");
            html.append("      <thead>\n");
            html.append("        <tr>\n");
            html.append("          <th>Category</th>\n");
            html.append("          <th>Days</th>\n");
            html.append("          <th>Cost</th>\n");
            html.append("          <th>Description</th>\n");
            html.append("        </tr>\n");
            html.append("      </thead>\n");
            html.append("      <tbody>\n");

            for (TechnicalDebtSummary.DebtCategory cat : debt.breakdown()) {
                html.append("        <tr>\n");
                html.append("          <td>").append(escape(cat.category())).append("</td>\n");
                html.append("          <td>")
                        .append(String.format("%.1f", cat.days()))
                        .append("</td>\n");
                html.append("          <td>")
                        .append(String.format("%.0f €", cat.cost()))
                        .append("</td>\n");
                html.append("          <td>").append(escape(cat.description())).append("</td>\n");
                html.append("        </tr>\n");
            }

            html.append("      </tbody>\n");
            html.append("    </table>\n");
        }

        html.append("  </section>\n");
    }

    /**
     * Appends recommendations section.
     */
    private void appendRecommendations(StringBuilder html, List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }

        html.append("  <section>\n");
        html.append("    <h2>Recommendations</h2>\n");

        for (Recommendation rec : recommendations) {
            html.append("    <div class=\"recommendation-card\">\n");
            html.append("      <h3><span class=\"badge badge-")
                    .append(getPriorityClass(rec.priority()))
                    .append("\">")
                    .append(rec.priority().name())
                    .append("</span> ")
                    .append(escape(rec.title()))
                    .append("</h3>\n");
            html.append("      <p>").append(escape(rec.description())).append("</p>\n");

            if (!rec.affectedTypes().isEmpty()) {
                html.append("      <p><strong>Affected Types:</strong> ");
                html.append(String.join(
                        ", ",
                        rec.affectedTypes().stream()
                                .map(t -> "<code>" + escape(t) + "</code>")
                                .toList()));
                html.append("</p>\n");
            }

            html.append("      <p><strong>Estimated Effort:</strong> ")
                    .append(String.format("%.1f", rec.estimatedEffort()))
                    .append(" person-days</p>\n");
            html.append("      <p><strong>Expected Impact:</strong> ")
                    .append(escape(rec.expectedImpact()))
                    .append("</p>\n");
            html.append("    </div>\n");
        }

        html.append("  </section>\n");
    }

    /**
     * Returns CSS class for recommendation priority.
     */
    private String getPriorityClass(io.hexaglue.plugin.audit.domain.model.RecommendationPriority priority) {
        return switch (priority) {
            case IMMEDIATE -> "error";
            case SHORT_TERM -> "warning";
            case MEDIUM_TERM -> "info";
            case LOW -> "ok";
        };
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
