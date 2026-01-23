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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Generates audit reports in JSON format.
 *
 * <p>This generator produces pretty-printed JSON suitable for programmatic
 * consumption. The output uses 2-space indentation and includes all fields.
 *
 * <p>The JSON structure is:
 * <pre>
 * {
 *   "metadata": {...},
 *   "summary": {...},
 *   "violations": [...],
 *   "metrics": [...],
 *   "constraints": {...}
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public final class JsonReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public ReportFormat format() {
        return ReportFormat.JSON;
    }

    @Override
    public String generate(AuditReport report) {
        Objects.requireNonNull(report, "report required");

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Metadata
        json.append("  \"metadata\": {\n");
        json.append("    \"projectName\": ")
                .append(quote(report.metadata().projectName()))
                .append(",\n");
        json.append("    \"timestamp\": ")
                .append(quote(ISO_FORMATTER.format(report.metadata().timestamp())))
                .append(",\n");
        json.append("    \"duration\": ")
                .append(quote(report.metadata().duration()))
                .append(",\n");
        json.append("    \"hexaglueVersion\": ")
                .append(quote(report.metadata().hexaglueVersion()))
                .append("\n");
        json.append("  },\n");

        // Summary
        json.append("  \"summary\": {\n");
        json.append("    \"passed\": ").append(report.summary().passed()).append(",\n");
        json.append("    \"totalViolations\": ")
                .append(report.summary().totalViolations())
                .append(",\n");
        json.append("    \"blockers\": ").append(report.summary().blockers()).append(",\n");
        json.append("    \"criticals\": ").append(report.summary().criticals()).append(",\n");
        json.append("    \"majors\": ").append(report.summary().majors()).append(",\n");
        json.append("    \"minors\": ").append(report.summary().minors()).append(",\n");
        json.append("    \"infos\": ").append(report.summary().infos()).append("\n");
        json.append("  },\n");

        // Violations
        json.append("  \"violations\": ");
        if (report.violations().isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < report.violations().size(); i++) {
                ViolationEntry v = report.violations().get(i);
                json.append("    {\n");
                json.append("      \"constraintId\": ")
                        .append(quote(v.constraintId()))
                        .append(",\n");
                json.append("      \"severity\": ").append(quote(v.severity())).append(",\n");
                json.append("      \"message\": ").append(quote(v.message())).append(",\n");
                json.append("      \"affectedType\": ")
                        .append(quote(v.affectedType()))
                        .append(",\n");
                json.append("      \"location\": ").append(quote(v.location())).append(",\n");
                json.append("      \"evidence\": ").append(quote(v.evidence())).append("\n");
                json.append("    }");
                if (i < report.violations().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }
        json.append(",\n");

        // Metrics
        json.append("  \"metrics\": ");
        if (report.metrics().isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < report.metrics().size(); i++) {
                MetricEntry m = report.metrics().get(i);
                json.append("    {\n");
                json.append("      \"name\": ").append(quote(m.name())).append(",\n");
                json.append("      \"value\": ").append(m.value()).append(",\n");
                json.append("      \"unit\": ").append(quote(m.unit())).append(",\n");
                json.append("      \"threshold\": ")
                        .append(m.threshold() == null ? "null" : m.threshold())
                        .append(",\n");
                json.append("      \"thresholdType\": ")
                        .append(m.thresholdType() == null ? "null" : quote(m.thresholdType()))
                        .append(",\n");
                json.append("      \"status\": ").append(quote(m.status())).append("\n");
                json.append("    }");
                if (i < report.metrics().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]");
        }
        json.append(",\n");

        // Constraints
        json.append("  \"constraints\": {\n");
        json.append("    \"totalConstraints\": ")
                .append(report.constraints().totalConstraints())
                .append(",\n");
        json.append("    \"constraintNames\": ");
        if (report.constraints().constraintNames().isEmpty()) {
            json.append("[]\n");
        } else {
            json.append("[\n");
            for (int i = 0; i < report.constraints().constraintNames().size(); i++) {
                json.append("      ")
                        .append(quote(report.constraints().constraintNames().get(i)));
                if (i < report.constraints().constraintNames().size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("    ]\n");
        }
        json.append("  },\n");

        // Architecture Analysis
        appendArchitectureAnalysis(json, report.architectureAnalysis());
        json.append(",\n");

        // Health Score
        appendHealthScore(json, report.healthScore());
        json.append(",\n");

        // Component Inventory
        appendInventory(json, report.inventory());
        json.append(",\n");

        // Port Matrix
        appendPortMatrix(json, report.portMatrix());
        json.append(",\n");

        // Technical Debt
        appendTechnicalDebt(json, report.technicalDebt());
        json.append(",\n");

        // Recommendations
        appendRecommendations(json, report.recommendations());
        json.append(",\n");

        // Executive Summary
        appendExecutiveSummary(json, report.executiveSummary());
        json.append(",\n");

        // Compliance percentages
        json.append("  \"dddCompliancePercent\": ")
                .append(report.dddCompliancePercent())
                .append(",\n");
        json.append("  \"hexCompliancePercent\": ")
                .append(report.hexCompliancePercent())
                .append("\n");

        json.append("}\n");

        return json.toString();
    }

    /**
     * Appends architecture analysis section to the JSON output.
     */
    private void appendArchitectureAnalysis(StringBuilder json, ArchitectureAnalysis analysis) {
        json.append("  \"architectureAnalysis\": {\n");

        // Summary
        json.append("    \"totalCycles\": ").append(analysis.totalCycles()).append(",\n");
        json.append("    \"totalViolations\": ")
                .append(analysis.totalViolations())
                .append(",\n");
        json.append("    \"isClean\": ").append(analysis.isClean()).append(",\n");

        // Type cycles
        json.append("    \"typeCycles\": ");
        appendCycles(json, analysis.typeCycles());
        json.append(",\n");

        // Package cycles
        json.append("    \"packageCycles\": ");
        appendCycles(json, analysis.packageCycles());
        json.append(",\n");

        // Bounded context cycles
        json.append("    \"boundedContextCycles\": ");
        appendCycles(json, analysis.boundedContextCycles());
        json.append(",\n");

        // Layer violations
        json.append("    \"layerViolations\": ");
        appendLayerViolations(json, analysis.layerViolations());
        json.append(",\n");

        // Stability violations
        json.append("    \"stabilityViolations\": ");
        appendStabilityViolations(json, analysis.stabilityViolations());
        json.append(",\n");

        // Coupling metrics
        json.append("    \"couplingMetrics\": ");
        appendCouplingMetrics(json, analysis.couplingMetrics());
        json.append("\n");

        json.append("  }\n");
    }

    private void appendCycles(StringBuilder json, java.util.List<ArchitectureAnalysis.CycleEntry> cycles) {
        if (cycles.isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < cycles.size(); i++) {
                var cycle = cycles.get(i);
                json.append("      {\n");
                json.append("        \"kind\": ").append(quote(cycle.kind())).append(",\n");
                json.append("        \"length\": ").append(cycle.length()).append(",\n");
                json.append("        \"path\": [");
                for (int j = 0; j < cycle.path().size(); j++) {
                    json.append(quote(cycle.path().get(j)));
                    if (j < cycle.path().size() - 1) json.append(", ");
                }
                json.append("]\n");
                json.append("      }");
                if (i < cycles.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
    }

    private void appendLayerViolations(
            StringBuilder json, java.util.List<ArchitectureAnalysis.LayerViolationEntry> violations) {
        if (violations.isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < violations.size(); i++) {
                var v = violations.get(i);
                json.append("      {\n");
                json.append("        \"sourceType\": ")
                        .append(quote(v.sourceType()))
                        .append(",\n");
                json.append("        \"targetType\": ")
                        .append(quote(v.targetType()))
                        .append(",\n");
                json.append("        \"sourceLayer\": ")
                        .append(quote(v.sourceLayer()))
                        .append(",\n");
                json.append("        \"targetLayer\": ")
                        .append(quote(v.targetLayer()))
                        .append(",\n");
                json.append("        \"description\": ")
                        .append(quote(v.description()))
                        .append("\n");
                json.append("      }");
                if (i < violations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
    }

    private void appendStabilityViolations(
            StringBuilder json, java.util.List<ArchitectureAnalysis.StabilityViolationEntry> violations) {
        if (violations.isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < violations.size(); i++) {
                var v = violations.get(i);
                json.append("      {\n");
                json.append("        \"sourceType\": ")
                        .append(quote(v.sourceType()))
                        .append(",\n");
                json.append("        \"targetType\": ")
                        .append(quote(v.targetType()))
                        .append(",\n");
                json.append("        \"sourceStability\": ")
                        .append(v.sourceStability())
                        .append(",\n");
                json.append("        \"targetStability\": ")
                        .append(v.targetStability())
                        .append(",\n");
                json.append("        \"description\": ")
                        .append(quote(v.description()))
                        .append("\n");
                json.append("      }");
                if (i < violations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
    }

    private void appendCouplingMetrics(
            StringBuilder json, java.util.List<ArchitectureAnalysis.PackageCouplingEntry> metrics) {
        if (metrics.isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < metrics.size(); i++) {
                var m = metrics.get(i);
                json.append("      {\n");
                json.append("        \"packageName\": ")
                        .append(quote(m.packageName()))
                        .append(",\n");
                json.append("        \"afferentCoupling\": ")
                        .append(m.afferentCoupling())
                        .append(",\n");
                json.append("        \"efferentCoupling\": ")
                        .append(m.efferentCoupling())
                        .append(",\n");
                json.append("        \"instability\": ")
                        .append(String.format(Locale.US, "%.4f", m.instability()))
                        .append(",\n");
                json.append("        \"abstractness\": ")
                        .append(String.format(Locale.US, "%.4f", m.abstractness()))
                        .append(",\n");
                json.append("        \"distance\": ")
                        .append(String.format(Locale.US, "%.4f", m.distance()))
                        .append(",\n");
                json.append("        \"zoneOfPain\": ")
                        .append(m.isInZoneOfPain())
                        .append(",\n");
                json.append("        \"zoneOfUselessness\": ")
                        .append(m.isInZoneOfUselessness())
                        .append("\n");
                json.append("      }");
                if (i < metrics.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
    }

    /**
     * Appends health score to JSON output.
     */
    private void appendHealthScore(StringBuilder json, HealthScore score) {
        json.append("  \"healthScore\": {\n");
        json.append("    \"overall\": ").append(score.overall()).append(",\n");
        json.append("    \"dddCompliance\": ").append(score.dddCompliance()).append(",\n");
        json.append("    \"hexCompliance\": ").append(score.hexCompliance()).append(",\n");
        json.append("    \"dependencyQuality\": ")
                .append(score.dependencyQuality())
                .append(",\n");
        json.append("    \"coupling\": ").append(score.coupling()).append(",\n");
        json.append("    \"cohesion\": ").append(score.cohesion()).append(",\n");
        json.append("    \"grade\": ").append(quote(score.grade())).append("\n");
        json.append("  }");
    }

    /**
     * Appends component inventory to JSON output.
     */
    private void appendInventory(StringBuilder json, ComponentInventory inv) {
        json.append("  \"inventory\": {\n");
        json.append("    \"aggregateRoots\": ").append(inv.aggregateRoots()).append(",\n");
        json.append("    \"entities\": ").append(inv.entities()).append(",\n");
        json.append("    \"valueObjects\": ").append(inv.valueObjects()).append(",\n");
        json.append("    \"domainEvents\": ").append(inv.domainEvents()).append(",\n");
        json.append("    \"domainServices\": ").append(inv.domainServices()).append(",\n");
        json.append("    \"applicationServices\": ")
                .append(inv.applicationServices())
                .append(",\n");
        json.append("    \"drivingPorts\": ").append(inv.drivingPorts()).append(",\n");
        json.append("    \"drivenPorts\": ").append(inv.drivenPorts()).append(",\n");
        json.append("    \"totalDomainTypes\": ").append(inv.totalDomainTypes()).append(",\n");
        json.append("    \"totalPorts\": ").append(inv.totalPorts()).append("\n");
        json.append("  }");
    }

    /**
     * Appends port matrix to JSON output.
     */
    private void appendPortMatrix(StringBuilder json, List<PortMatrixEntry> portMatrix) {
        json.append("  \"portMatrix\": ");
        if (portMatrix == null || portMatrix.isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < portMatrix.size(); i++) {
                PortMatrixEntry p = portMatrix.get(i);
                json.append("    {\n");
                json.append("      \"portName\": ").append(quote(p.portName())).append(",\n");
                json.append("      \"direction\": ")
                        .append(quote(p.direction()))
                        .append(",\n");
                json.append("      \"kind\": ").append(quote(p.kind())).append(",\n");
                json.append("      \"managedType\": ")
                        .append(quote(p.managedType()))
                        .append(",\n");
                json.append("      \"methodCount\": ").append(p.methodCount()).append(",\n");
                json.append("      \"adapterStatus\": ").append(quote(p.adapterStatus())).append("\n");
                json.append("    }");
                if (i < portMatrix.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]");
        }
    }

    /**
     * Appends technical debt summary to JSON output.
     */
    private void appendTechnicalDebt(StringBuilder json, TechnicalDebtSummary debt) {
        json.append("  \"technicalDebt\": {\n");
        json.append("    \"totalDays\": ")
                .append(String.format(Locale.US, "%.2f", debt.totalDays()))
                .append(",\n");
        json.append("    \"totalCost\": ")
                .append(String.format(Locale.US, "%.2f", debt.totalCost()))
                .append(",\n");
        json.append("    \"monthlyInterest\": ")
                .append(String.format(Locale.US, "%.2f", debt.monthlyInterest()))
                .append(",\n");
        json.append("    \"breakdown\": ");
        if (debt.breakdown() == null || debt.breakdown().isEmpty()) {
            json.append("[]\n");
        } else {
            json.append("[\n");
            for (int i = 0; i < debt.breakdown().size(); i++) {
                TechnicalDebtSummary.DebtCategory cat = debt.breakdown().get(i);
                json.append("      {\n");
                json.append("        \"category\": ")
                        .append(quote(cat.category()))
                        .append(",\n");
                json.append("        \"days\": ")
                        .append(String.format(Locale.US, "%.2f", cat.days()))
                        .append(",\n");
                json.append("        \"cost\": ")
                        .append(String.format(Locale.US, "%.2f", cat.cost()))
                        .append(",\n");
                json.append("        \"description\": ")
                        .append(quote(cat.description()))
                        .append("\n");
                json.append("      }");
                if (i < debt.breakdown().size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]\n");
        }
        json.append("  }");
    }

    /**
     * Appends recommendations to JSON output.
     */
    private void appendRecommendations(StringBuilder json, List<Recommendation> recommendations) {
        json.append("  \"recommendations\": ");
        if (recommendations == null || recommendations.isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < recommendations.size(); i++) {
                Recommendation r = recommendations.get(i);
                json.append("    {\n");
                json.append("      \"priority\": ")
                        .append(quote(r.priority().name()))
                        .append(",\n");
                json.append("      \"title\": ").append(quote(r.title())).append(",\n");
                json.append("      \"description\": ")
                        .append(quote(r.description()))
                        .append(",\n");
                json.append("      \"affectedTypes\": [");
                for (int j = 0; j < r.affectedTypes().size(); j++) {
                    json.append(quote(r.affectedTypes().get(j)));
                    if (j < r.affectedTypes().size() - 1) json.append(", ");
                }
                json.append("],\n");
                json.append("      \"estimatedEffort\": ")
                        .append(String.format(Locale.US, "%.2f", r.estimatedEffort()))
                        .append(",\n");
                json.append("      \"expectedImpact\": ")
                        .append(quote(r.expectedImpact()))
                        .append(",\n");
                json.append("      \"relatedViolations\": [");
                for (int j = 0; j < r.relatedViolations().size(); j++) {
                    json.append(quote(r.relatedViolations().get(j).value()));
                    if (j < r.relatedViolations().size() - 1) json.append(", ");
                }
                json.append("]\n");
                json.append("    }");
                if (i < recommendations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]");
        }
    }

    /**
     * Appends executive summary to JSON output.
     */
    private void appendExecutiveSummary(StringBuilder json, ExecutiveSummary summary) {
        json.append("  \"executiveSummary\": {\n");
        json.append("    \"verdict\": ").append(quote(summary.verdict())).append(",\n");

        // Strengths
        json.append("    \"strengths\": [");
        if (summary.strengths() != null && !summary.strengths().isEmpty()) {
            json.append("\n");
            for (int i = 0; i < summary.strengths().size(); i++) {
                json.append("      ").append(quote(summary.strengths().get(i)));
                if (i < summary.strengths().size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ");
        }
        json.append("],\n");

        // Concerns
        json.append("    \"concerns\": ");
        if (summary.concerns() == null || summary.concerns().isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < summary.concerns().size(); i++) {
                ExecutiveSummary.ConcernEntry c = summary.concerns().get(i);
                json.append("      {\n");
                json.append("        \"severity\": ")
                        .append(quote(c.severity()))
                        .append(",\n");
                json.append("        \"description\": ")
                        .append(quote(c.description()))
                        .append(",\n");
                json.append("        \"count\": ").append(c.count()).append("\n");
                json.append("      }");
                if (i < summary.concerns().size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
        json.append(",\n");

        // KPIs
        json.append("    \"kpis\": ");
        if (summary.kpis() == null || summary.kpis().isEmpty()) {
            json.append("[]");
        } else {
            json.append("[\n");
            for (int i = 0; i < summary.kpis().size(); i++) {
                ExecutiveSummary.KpiEntry k = summary.kpis().get(i);
                json.append("      {\n");
                json.append("        \"name\": ").append(quote(k.name())).append(",\n");
                json.append("        \"value\": ").append(quote(k.value())).append(",\n");
                json.append("        \"threshold\": ")
                        .append(quote(k.threshold()))
                        .append(",\n");
                json.append("        \"status\": ").append(quote(k.status())).append("\n");
                json.append("      }");
                if (i < summary.kpis().size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
        json.append(",\n");

        // Immediate actions
        json.append("    \"immediateActions\": [");
        if (summary.immediateActions() != null && !summary.immediateActions().isEmpty()) {
            json.append("\n");
            for (int i = 0; i < summary.immediateActions().size(); i++) {
                json.append("      ").append(quote(summary.immediateActions().get(i)));
                if (i < summary.immediateActions().size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ");
        }
        json.append("]\n");

        json.append("  }");
    }

    /**
     * Quotes and escapes a string for JSON.
     */
    private String quote(String s) {
        if (s == null) {
            return "null";
        }
        // Escape special characters
        String escaped = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
