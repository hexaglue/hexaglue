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
 * Renders audit reports to self-contained HTML format.
 *
 * <p>The HTML output includes embedded CSS and Mermaid.js for diagram rendering,
 * producing a fully self-contained file suitable for viewing in any browser.
 *
 * @since 5.0.0
 */
public class HtmlRenderer implements ReportRenderer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ReportFormat format() {
        return ReportFormat.HTML;
    }

    @Override
    public String render(ReportData data) {
        return render(data, null);
    }

    @Override
    public String render(ReportData data, DiagramSet diagrams) {
        StringBuilder html = new StringBuilder();

        renderHead(html, data.metadata());
        html.append("<body>\n<div class=\"container\">\n");

        renderHeader(html, data.metadata());
        renderVerdict(html, data.verdict(), diagrams);
        renderArchitecture(html, data.architecture(), diagrams);
        renderIssues(html, data.issues(), diagrams);
        renderRemediation(html, data.remediation());
        renderAppendix(html, data.appendix(), diagrams);
        renderFooter(html, data.metadata());

        html.append("</div>\n");
        html.append(MERMAID_SCRIPT);
        html.append("</body>\n</html>");

        return html.toString();
    }

    private void renderHead(StringBuilder html, ReportMetadata metadata) {
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>").append(escape(metadata.projectName())).append(" - Architecture Audit Report</title>\n");
        html.append(STYLES);
        html.append("</head>\n");
    }

    private void renderHeader(StringBuilder html, ReportMetadata metadata) {
        html.append("<header>\n");
        html.append("  <h1>HexaGlue Architecture Audit Report</h1>\n");
        html.append("  <p class=\"meta\">\n");
        html.append("    <strong>").append(escape(metadata.projectName())).append("</strong> v");
        html.append(escape(metadata.projectVersion())).append(" | ");
        html.append(DATE_FORMAT.format(metadata.timestamp().atZone(ZoneId.systemDefault())));
        html.append(" | HexaGlue v").append(escape(metadata.hexaglueVersion()));
        html.append("\n  </p>\n");
        html.append("</header>\n");
    }

    private void renderVerdict(StringBuilder html, Verdict verdict, DiagramSet diagrams) {
        html.append("<section class=\"card\" id=\"verdict\">\n");
        html.append("  <h2>1. Verdict</h2>\n");

        // Score display with status
        String statusClass = switch (verdict.status()) {
            case PASSED -> "passed";
            case PASSED_WITH_WARNINGS -> "warning";
            case FAILED -> "failed";
        };
        String statusLabel = switch (verdict.status()) {
            case PASSED -> "Passed";
            case PASSED_WITH_WARNINGS -> "Warning";
            case FAILED -> "Failed";
        };

        html.append("  <div class=\"verdict-grid\">\n");
        html.append("    <div class=\"score-display ").append(statusClass).append("\">\n");
        html.append("      <div class=\"score\">").append(verdict.score()).append("</div>\n");
        html.append("      <div class=\"grade\">Grade ").append(escape(verdict.grade())).append("</div>\n");
        html.append("      <span class=\"status-badge\">").append(statusLabel).append("</span>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"verdict-content\">\n");
        html.append("      <p class=\"summary\">").append(escape(verdict.summary())).append("</p>\n");

        // Immediate action
        if (verdict.immediateAction().required()) {
            html.append("      <div class=\"immediate-action\">\n");
            html.append("        <strong>Immediate Action Required:</strong><br>\n");
            html.append("        ").append(escape(verdict.immediateAction().message())).append("\n");
            html.append("      </div>\n");
        }
        html.append("    </div>\n");
        html.append("  </div>\n");

        // KPIs table
        if (!verdict.kpis().isEmpty()) {
            html.append("  <h3>Key Performance Indicators</h3>\n");
            html.append("  <table class=\"kpi-table\">\n");
            html.append("    <thead><tr><th>Dimension</th><th>Score</th><th><strong>Contribution</strong> (Weight)</th><th>Status</th></tr></thead>\n");
            html.append("    <tbody>\n");
            double totalContribution = 0;
            int totalWeight = 0;
            for (KPI kpi : verdict.kpis()) {
                String kpiStatusClass = switch (kpi.status()) {
                    case OK -> "status-ok";
                    case WARNING -> "status-warning";
                    case CRITICAL -> "status-critical";
                };
                double contribution = kpi.contribution();
                totalContribution += contribution;
                totalWeight += kpi.weight();
                html.append("      <tr>");
                html.append("<td>").append(escape(kpi.name())).append("</td>");
                html.append("<td>").append(String.format(Locale.ROOT, "%.0f", kpi.value())).append(escape(kpi.unit())).append("</td>");
                html.append("<td><strong>").append(String.format(Locale.ROOT, "%.1f", contribution)).append("</strong> (").append(kpi.weight()).append("%)</td>");
                html.append("<td class=\"").append(kpiStatusClass).append("\">").append(kpi.status().label()).append("</td></tr>\n");
            }
            // TOTAL row
            String totalStatusClass = switch (verdict.status()) {
                case PASSED -> "status-ok";
                case PASSED_WITH_WARNINGS -> "status-warning";
                case FAILED -> "status-critical";
            };
            String totalStatusLabel = switch (verdict.status()) {
                case PASSED -> "Passed";
                case PASSED_WITH_WARNINGS -> "Warning";
                case FAILED -> "Failed";
            };
            html.append("      <tr class=\"total-row\">");
            html.append("<td><strong>TOTAL</strong></td>");
            html.append("<td></td>");
            html.append("<td><strong>").append(String.format(Locale.ROOT, "%.1f", totalContribution)).append("</strong> (").append(totalWeight).append("%)</td>");
            html.append("<td class=\"").append(totalStatusClass).append("\">").append(totalStatusLabel).append("</td></tr>\n");
            html.append("    </tbody>\n");
            html.append("  </table>\n");
        }

        // Score radar
        if (diagrams != null && diagrams.scoreRadar() != null) {
            html.append("  <h3>Score Radar</h3>\n");
            html.append("  <div class=\"diagram-container\">\n");
            html.append("    <div class=\"mermaid\">\n").append(diagrams.scoreRadar()).append("\n    </div>\n");
            html.append("  </div>\n");
        }

        html.append("</section>\n");
    }

    private void renderArchitecture(StringBuilder html, ArchitectureOverview arch, DiagramSet diagrams) {
        html.append("<section class=\"card\" id=\"architecture\">\n");
        html.append("  <h2>2. Architecture Analyzed</h2>\n");
        html.append("  <p>").append(escape(arch.summary())).append("</p>\n");

        // Inventory grid
        InventoryTotals totals = arch.inventory().totals();
        html.append("  <h3>Component Inventory</h3>\n");
        html.append("  <div class=\"inventory-grid\">\n");
        renderInventoryItem(html, totals.aggregates(), "Aggregates");
        renderInventoryItem(html, totals.entities(), "Entities");
        renderInventoryItem(html, totals.valueObjects(), "Value Objects");
        renderInventoryItem(html, totals.identifiers(), "Identifiers");
        renderInventoryItem(html, totals.drivingPorts(), "Driving Ports");
        renderInventoryItem(html, totals.drivenPorts(), "Driven Ports");
        html.append("  </div>\n");

        // Inventory table with component names
        html.append("  <table class=\"inventory-table\">\n");
        html.append("    <thead><tr><th>Category</th><th>Count</th><th>Details</th></tr></thead>\n");
        html.append("    <tbody>\n");
        renderInventoryRow(html, "Aggregate Roots", totals.aggregates(), arch.components().aggregates());
        renderInventoryRow(html, "Value Objects", totals.valueObjects(), arch.components().valueObjects());
        renderInventoryRow(html, "Identifiers", totals.identifiers(), arch.components().identifiers());
        renderInventoryRowPorts(html, "Driving Ports", totals.drivingPorts(), arch.components().drivingPorts());
        renderInventoryRowPorts(html, "Driven Ports", totals.drivenPorts(), arch.components().drivenPorts());
        html.append("    </tbody>\n");
        html.append("  </table>\n");

        // Diagrams
        if (diagrams != null) {
            if (diagrams.c4Context() != null) {
                html.append("  <h3>System Context Diagram</h3>\n");
                html.append("  <div class=\"diagram-container\">\n");
                html.append("    <div class=\"mermaid\">\n").append(diagrams.c4Context()).append("\n    </div>\n");
                html.append("  </div>\n");
            }
            if (diagrams.c4Component() != null) {
                html.append("  <h3>C4 Component Diagram</h3>\n");
                html.append("  <div class=\"diagram-container\">\n");
                html.append("    <div class=\"mermaid\">\n").append(diagrams.c4Component()).append("\n    </div>\n");
                html.append("  </div>\n");
            }
            if (diagrams.domainModel() != null) {
                html.append("  <h3>Domain Model</h3>\n");
                html.append("  <div class=\"diagram-container\">\n");
                html.append("    <div class=\"mermaid\">\n").append(diagrams.domainModel()).append("\n    </div>\n");
                html.append("  </div>\n");
            }
        }

        html.append("</section>\n");
    }

    private void renderInventoryItem(StringBuilder html, int count, String label) {
        html.append("    <div class=\"inventory-item\">\n");
        html.append("      <div class=\"count\">").append(count).append("</div>\n");
        html.append("      <div class=\"label\">").append(escape(label)).append("</div>\n");
        html.append("    </div>\n");
    }

    private void renderInventoryRow(StringBuilder html, String category, int count, java.util.List<? extends Object> items) {
        html.append("      <tr><td>").append(escape(category)).append("</td>");
        html.append("<td class=\"count-cell").append(count == 0 ? " count-zero" : "").append("\">").append(count).append("</td>");
        html.append("<td class=\"details-cell\">");
        if (items.isEmpty()) {
            html.append("-");
        } else {
            for (Object item : items) {
                String name = extractName(item);
                html.append("<span class=\"tag\">").append(escape(name)).append("</span> ");
            }
        }
        html.append("</td></tr>\n");
    }

    private void renderInventoryRowPorts(StringBuilder html, String category, int count, java.util.List<? extends PortComponent> ports) {
        html.append("      <tr><td>").append(escape(category)).append("</td>");
        html.append("<td class=\"count-cell").append(count == 0 ? " count-zero" : "").append("\">").append(count).append("</td>");
        html.append("<td class=\"details-cell\">");
        if (ports.isEmpty()) {
            html.append("-");
        } else {
            for (PortComponent port : ports) {
                html.append("<span class=\"tag port\">").append(escape(port.name())).append("</span> ");
            }
        }
        html.append("</td></tr>\n");
    }

    private String extractName(Object item) {
        if (item instanceof AggregateComponent agg) return agg.name();
        if (item instanceof ValueObjectComponent vo) return vo.name();
        if (item instanceof IdentifierComponent id) return id.name();
        return item.toString();
    }

    private void renderIssues(StringBuilder html, IssuesSummary issues, DiagramSet diagrams) {
        html.append("<section class=\"card\" id=\"issues\">\n");
        html.append("  <h2>3. Issues Found</h2>\n");

        ViolationCounts counts = issues.summary();
        if (counts.total() == 0) {
            html.append("  <p class=\"success\">No issues detected.</p>\n");
        } else {
            html.append("  <p><strong>").append(counts.total()).append(" violations</strong> found, grouped by theme.</p>\n");

            // Issue groups
            for (IssueGroup group : issues.groups()) {
                html.append("  <div class=\"issue-group\">\n");
                html.append("    <div class=\"issue-group-header\">\n");
                html.append("      <h4 class=\"issue-group-title\">").append(escape(group.theme())).append("</h4>\n");
                html.append("      <span class=\"issue-count\">").append(group.count()).append(" issues</span>\n");
                html.append("    </div>\n");
                html.append("    <p class=\"issue-group-desc\">").append(escape(group.description())).append("</p>\n");

                for (IssueEntry issue : group.violations()) {
                    renderIssue(html, issue);
                }
                html.append("  </div>\n");
            }
        }

        html.append("</section>\n");
    }

    private void renderIssue(StringBuilder html, IssueEntry issue) {
        String severityClass = "severity-" + issue.severity().name().toLowerCase();
        html.append("    <div class=\"violation\" id=\"").append(escape(issue.id())).append("\">\n");
        html.append("      <div class=\"violation-header\">\n");
        html.append("        <span class=\"severity-badge ").append(severityClass).append("\">").append(issue.severity()).append("</span>\n");
        html.append("        <span class=\"violation-title\">").append(escape(issue.title())).append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class=\"violation-body\">\n");
        html.append("        <div class=\"violation-location\">").append(escape(issue.location().file()));
        issue.location().lineOpt().ifPresent(line -> html.append(" (line ").append(line).append(")"));
        html.append("</div>\n");
        html.append("        <p>").append(escape(issue.message())).append("</p>\n");
        html.append("        <div class=\"violation-impact\"><strong>Impact:</strong> ").append(escape(issue.impact())).append("</div>\n");

        Suggestion s = issue.suggestion();
        html.append("        <div class=\"violation-fix\">\n");
        html.append("          <h5>How to fix:</h5>\n");

        if (!s.steps().isEmpty()) {
            html.append("          <ol>\n");
            for (String step : s.steps()) {
                html.append("            <li>").append(escape(step)).append("</li>\n");
            }
            html.append("          </ol>\n");
        }

        s.codeExampleOpt().ifPresent(code -> {
            html.append("          <div class=\"code-example\">").append(escape(code)).append("</div>\n");
        });

        s.effortOpt().ifPresent(effort -> {
            html.append("          <p><strong>Effort:</strong> ").append(escape(effort)).append("</p>\n");
        });

        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
    }

    private void renderRemediation(StringBuilder html, RemediationPlan plan) {
        html.append("<section class=\"card\" id=\"remediation\">\n");
        html.append("  <h2>4. How to Fix</h2>\n");

        if (plan.actions().isEmpty()) {
            html.append("  <p class=\"success\">No remediation actions required.</p>\n");
        } else {
            html.append("  <p><strong>").append(plan.actions().size()).append(" actions</strong> required to achieve compliance.</p>\n");

            html.append("  <table class=\"action-table\">\n");
            html.append("    <thead><tr><th style=\"width:60px\">Priority</th><th>Action</th><th style=\"width:100px\">Effort</th><th>Impact</th></tr></thead>\n");
            html.append("    <tbody>\n");
            for (RemediationAction action : plan.actions()) {
                html.append("      <tr>");
                html.append("<td><span class=\"priority-badge\">").append(action.priority()).append("</span></td>");
                html.append("<td><strong>").append(escape(action.title())).append("</strong><br>");
                html.append("<small class=\"text-muted\">").append(escape(action.description())).append("</small></td>");
                html.append("<td><span class=\"effort-badge\">").append(String.format(Locale.ROOT, "%.1f", action.effort().days())).append(" days</span></td>");
                html.append("<td>").append(escape(action.impact())).append("</td>");
                html.append("</tr>\n");
            }
            html.append("    </tbody>\n");
            html.append("  </table>\n");

            // Total effort
            TotalEffort effort = plan.totalEffort();
            html.append("  <div class=\"total-effort\">\n");
            html.append("    <div class=\"total-effort-item\">\n");
            html.append("      <div class=\"value\">").append(String.format(Locale.ROOT, "%.1f", effort.days())).append("</div>\n");
            html.append("      <div class=\"label\">Days</div>\n");
            html.append("    </div>\n");
            effort.costOpt().ifPresent(cost -> {
                html.append("    <div class=\"total-effort-item\">\n");
                html.append("      <div class=\"value\">").append(String.format(Locale.ROOT, "%.0f", cost.amount())).append(" ").append(cost.currency()).append("</div>\n");
                html.append("      <div class=\"label\">Estimated Cost</div>\n");
                html.append("    </div>\n");
            });
            html.append("  </div>\n");
        }

        html.append("</section>\n");
    }

    private void renderAppendix(StringBuilder html, Appendix appendix, DiagramSet diagrams) {
        html.append("<section class=\"card\" id=\"appendix\">\n");
        html.append("  <h2>5. Appendix</h2>\n");

        // Score breakdown
        html.append("  <h3>Score Breakdown</h3>\n");
        html.append("  <table class=\"appendix-table\">\n");
        html.append("    <thead><tr><th>Dimension</th><th>Weight</th><th>Score</th><th>Contribution</th></tr></thead>\n");
        html.append("    <tbody>\n");
        ScoreBreakdown b = appendix.scoreBreakdown();
        renderDimensionRow(html, "DDD Compliance", b.dddCompliance());
        renderDimensionRow(html, "Hexagonal Architecture", b.hexagonalCompliance());
        renderDimensionRow(html, "Dependency Quality", b.dependencyQuality());
        renderDimensionRow(html, "Coupling Metrics", b.couplingMetrics());
        renderDimensionRow(html, "Cohesion Quality", b.cohesionQuality());
        html.append("    </tbody>\n");
        html.append("  </table>\n");

        // Metrics
        if (!appendix.metrics().isEmpty()) {
            html.append("  <h3>Metrics</h3>\n");
            html.append("  <table class=\"appendix-table\">\n");
            html.append("    <thead><tr><th>Metric</th><th>Value</th><th>Status</th></tr></thead>\n");
            html.append("    <tbody>\n");
            for (MetricEntry metric : appendix.metrics()) {
                String statusClass = switch (metric.status()) {
                    case OK -> "status-ok";
                    case WARNING -> "status-warning";
                    case CRITICAL -> "status-critical";
                };
                html.append("      <tr><td>").append(escape(metric.name())).append("</td>");
                html.append("<td>").append(String.format(Locale.ROOT, "%.2f", metric.value())).append(" ").append(escape(metric.unit())).append("</td>");
                html.append("<td class=\"").append(statusClass).append("\">").append(metric.status().label()).append("</td></tr>\n");
            }
            html.append("    </tbody>\n");
            html.append("  </table>\n");
        }

        // Package metrics
        if (!appendix.packageMetrics().isEmpty()) {
            html.append("  <h3>Package Metrics</h3>\n");
            html.append("  <table class=\"appendix-table\">\n");
            html.append("    <thead><tr><th>Package</th><th>Ca</th><th>Ce</th><th>I</th><th>A</th><th>D</th><th>Zone</th></tr></thead>\n");
            html.append("    <tbody>\n");
            for (PackageMetric pm : appendix.packageMetrics()) {
                html.append("      <tr><td>").append(escape(pm.packageName())).append("</td>");
                html.append("<td>").append(pm.ca()).append("</td>");
                html.append("<td>").append(pm.ce()).append("</td>");
                html.append("<td>").append(String.format(Locale.ROOT, "%.2f", pm.instability())).append("</td>");
                html.append("<td>").append(String.format(Locale.ROOT, "%.2f", pm.abstractness())).append("</td>");
                html.append("<td>").append(String.format(Locale.ROOT, "%.2f", pm.distance())).append("</td>");
                html.append("<td>").append(pm.zone().label()).append("</td></tr>\n");
            }
            html.append("    </tbody>\n");
            html.append("  </table>\n");
            html.append("  <p class=\"legend\">Ca=Afferent Coupling, Ce=Efferent Coupling, I=Instability, A=Abstractness, D=Distance</p>\n");
        }

        html.append("</section>\n");
    }

    private void renderDimensionRow(StringBuilder html, String name, ScoreDimension dim) {
        html.append("      <tr><td>").append(escape(name)).append("</td>");
        html.append("<td>").append(dim.weight()).append("%</td>");
        html.append("<td>").append(dim.score()).append("/100</td>");
        html.append("<td>").append(String.format(Locale.ROOT, "%.1f", dim.contribution())).append("</td></tr>\n");
    }

    private void renderFooter(StringBuilder html, ReportMetadata metadata) {
        html.append("<footer>\n");
        html.append("  Generated by <a href=\"https://hexaglue.io\">HexaGlue</a> ");
        html.append(escape(metadata.hexaglueVersion())).append(" | Plugin: ");
        html.append(escape(metadata.pluginVersion())).append("\n");
        html.append("</footer>\n");
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final String STYLES = """
<style>
:root {
    --color-ok: #4CAF50;
    --color-warning: #FF9800;
    --color-critical: #E53935;
    --color-blocker: #B71C1C;
    --color-bg: #f5f5f5;
    --color-card: #ffffff;
    --color-text: #333333;
    --color-muted: #666666;
    --color-border: #e0e0e0;
}
* { box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--color-bg); color: var(--color-text); line-height: 1.6; margin: 0; padding: 20px; }
.container { max-width: 1200px; margin: 0 auto; }
header { text-align: center; margin-bottom: 30px; }
header h1 { margin: 0; font-size: 2em; }
.meta { color: var(--color-muted); font-size: 0.9em; }
.card { background: var(--color-card); border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); padding: 24px; margin-bottom: 24px; }
.card h2 { margin-top: 0; padding-bottom: 12px; border-bottom: 2px solid var(--color-border); }
.card h3 { color: var(--color-muted); font-size: 1.1em; margin-top: 24px; }
.verdict-grid { display: grid; grid-template-columns: 200px 1fr; gap: 24px; align-items: start; }
.score-display { text-align: center; padding: 20px; border-radius: 8px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; align-self: start; }
.score-display .score { font-size: 3em; font-weight: bold; line-height: 1; }
.score-display .grade { font-size: 1.5em; opacity: 0.9; }
.score-display.failed { background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%); }
.score-display.passed { background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%); }
.score-display.warning { background: linear-gradient(135deg, #FF9800 0%, #F57C00 100%); }
.status-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-weight: bold; font-size: 0.85em; margin-top: 8px; background: rgba(255,255,255,0.2); }
.verdict-content { align-self: start; }
.verdict-content .summary { font-size: 1.1em; padding: 16px; background: #fff3e0; border-left: 4px solid var(--color-warning); border-radius: 4px; margin: 0; }
.immediate-action { background: #ffebee; border-left: 4px solid var(--color-critical); padding: 16px; border-radius: 4px; margin-top: 16px; }
.immediate-action strong { color: var(--color-critical); }
table { width: 100%; border-collapse: collapse; margin-top: 16px; }
th, td { padding: 12px; text-align: left; border: 1px solid var(--color-border); }
.total-row { font-weight: bold; background: var(--color-bg); }
th { background: var(--color-bg); font-weight: 600; }
.status-ok { color: var(--color-ok); font-weight: bold; }
.status-warning { color: var(--color-warning); font-weight: bold; }
.status-critical { color: var(--color-critical); font-weight: bold; }
.inventory-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 16px; margin: 16px 0; }
.inventory-item { text-align: center; padding: 16px; background: var(--color-bg); border-radius: 8px; }
.inventory-item .count { font-size: 2em; font-weight: bold; color: #667eea; }
.inventory-item .label { color: var(--color-muted); font-size: 0.9em; }
.inventory-table .count-cell { text-align: center; font-weight: bold; font-size: 1.1em; color: #667eea; }
.inventory-table .count-zero { color: var(--color-muted); font-weight: normal; }
.inventory-table .details-cell { font-size: 0.9em; }
.tag { display: inline-block; padding: 2px 8px; margin: 2px; background: #e3f2fd; color: #1565c0; border-radius: 12px; font-size: 0.85em; }
.tag.port { background: #fff3e0; color: #e65100; }
.diagram-container { background: white; padding: 20px; border-radius: 8px; border: 1px solid var(--color-border); margin: 16px 0; overflow-x: auto; }
.issue-group { margin-bottom: 24px; }
.issue-group-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.issue-group-title { font-size: 1.2em; font-weight: 600; margin: 0; }
.issue-count { background: var(--color-bg); padding: 4px 12px; border-radius: 20px; font-size: 0.85em; }
.issue-group-desc { color: var(--color-muted); margin-top: 0; }
.violation { border: 1px solid var(--color-border); border-radius: 8px; margin-bottom: 16px; overflow: hidden; }
.violation-header { padding: 16px; display: flex; align-items: center; gap: 12px; background: var(--color-bg); }
.severity-badge { padding: 4px 10px; border-radius: 4px; font-size: 0.75em; font-weight: bold; text-transform: uppercase; color: white; }
.severity-blocker { background: var(--color-blocker); }
.severity-critical { background: var(--color-critical); }
.severity-major { background: var(--color-warning); }
.severity-minor { background: #2196F3; }
.severity-info { background: #607D8B; }
.violation-title { flex: 1; font-weight: 600; }
.violation-body { padding: 16px; border-top: 1px solid var(--color-border); }
.violation-location { font-family: Monaco, Menlo, monospace; font-size: 0.85em; color: var(--color-muted); margin-bottom: 12px; }
.violation-impact { background: #fff3e0; padding: 12px; border-radius: 4px; margin: 12px 0; }
.violation-fix { background: #e8f5e9; padding: 12px; border-radius: 4px; margin: 12px 0; }
.violation-fix h5 { margin: 0 0 8px 0; color: var(--color-ok); }
.violation-fix ol { margin: 8px 0; padding-left: 20px; }
.code-example { background: #263238; color: #aed581; padding: 12px; border-radius: 4px; font-family: Monaco, Menlo, monospace; font-size: 0.85em; overflow-x: auto; white-space: pre; }
.action-table th, .action-table td { padding: 12px; text-align: left; border: 1px solid var(--color-border); }
.priority-badge { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 50%; background: #667eea; color: white; font-weight: bold; }
.effort-badge { display: inline-block; padding: 4px 8px; background: var(--color-bg); border-radius: 4px; font-size: 0.85em; }
.text-muted { color: var(--color-muted); }
.total-effort { display: flex; gap: 24px; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 8px; margin-top: 24px; }
.total-effort-item { text-align: center; }
.total-effort-item .value { font-size: 2em; font-weight: bold; }
.total-effort-item .label { opacity: 0.8; font-size: 0.9em; }
.appendix-table { width: 100%; border-collapse: collapse; font-size: 0.9em; }
.appendix-table th, .appendix-table td { padding: 8px 12px; text-align: left; border: 1px solid var(--color-border); }
.appendix-table th { background: var(--color-bg); }
.legend { color: var(--color-muted); font-size: 0.85em; margin-top: 8px; }
.success { color: var(--color-ok); font-weight: bold; }
footer { text-align: center; color: var(--color-muted); font-size: 0.85em; margin-top: 40px; padding-top: 20px; border-top: 1px solid var(--color-border); }
footer a { color: #667eea; }
@media (max-width: 768px) { .verdict-grid { grid-template-columns: 1fr; } .inventory-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
""";

    private static final String MERMAID_SCRIPT = """
<script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
<script>mermaid.initialize({startOnLoad: true, theme: 'default', securityLevel: 'loose'});</script>
""";
}
