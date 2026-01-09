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
import java.time.format.DateTimeFormatter;
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
        json.append("    \"criticals\": ")
                .append(report.summary().criticals())
                .append(",\n");
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
        json.append("    \"totalViolations\": ").append(analysis.totalViolations()).append(",\n");
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
                json.append("        \"sourceType\": ").append(quote(v.sourceType())).append(",\n");
                json.append("        \"targetType\": ").append(quote(v.targetType())).append(",\n");
                json.append("        \"sourceLayer\": ").append(quote(v.sourceLayer())).append(",\n");
                json.append("        \"targetLayer\": ").append(quote(v.targetLayer())).append(",\n");
                json.append("        \"description\": ").append(quote(v.description())).append("\n");
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
                json.append("        \"sourceType\": ").append(quote(v.sourceType())).append(",\n");
                json.append("        \"targetType\": ").append(quote(v.targetType())).append(",\n");
                json.append("        \"sourceStability\": ").append(v.sourceStability()).append(",\n");
                json.append("        \"targetStability\": ").append(v.targetStability()).append(",\n");
                json.append("        \"description\": ").append(quote(v.description())).append("\n");
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
                json.append("        \"packageName\": ").append(quote(m.packageName())).append(",\n");
                json.append("        \"afferentCoupling\": ").append(m.afferentCoupling()).append(",\n");
                json.append("        \"efferentCoupling\": ").append(m.efferentCoupling()).append(",\n");
                json.append("        \"instability\": ").append(String.format("%.4f", m.instability())).append(",\n");
                json.append("        \"abstractness\": ").append(String.format("%.4f", m.abstractness())).append(",\n");
                json.append("        \"distance\": ").append(String.format("%.4f", m.distance())).append(",\n");
                json.append("        \"zoneOfPain\": ").append(m.isInZoneOfPain()).append(",\n");
                json.append("        \"zoneOfUselessness\": ").append(m.isInZoneOfUselessness()).append("\n");
                json.append("      }");
                if (i < metrics.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
        }
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
