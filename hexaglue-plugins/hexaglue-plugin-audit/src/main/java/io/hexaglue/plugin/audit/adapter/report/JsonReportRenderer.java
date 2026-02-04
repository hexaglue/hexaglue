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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders audit reports to JSON format.
 *
 * <p>The JSON output is the canonical representation of report data,
 * suitable for programmatic consumption, storage, and further processing.
 * It does NOT include Mermaid diagrams (those are generated separately
 * and used only by visual renderers like HTML and Markdown).
 *
 * @since 5.0.0
 */
public class JsonReportRenderer implements ReportRenderer {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public ReportFormat format() {
        return ReportFormat.JSON;
    }

    @Override
    public String render(ReportData data) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        json.append("  \"version\": ").append(quote(data.version())).append(",\n");

        // Metadata
        appendMetadata(json, data.metadata());
        json.append(",\n");

        // Verdict
        appendVerdict(json, data.verdict());
        json.append(",\n");

        // Architecture
        appendArchitecture(json, data.architecture());
        json.append(",\n");

        // Issues
        appendIssues(json, data.issues());
        json.append(",\n");

        // Remediation
        appendRemediation(json, data.remediation());
        json.append(",\n");

        // Appendix
        appendAppendix(json, data.appendix());
        json.append("\n");

        json.append("}\n");
        return json.toString();
    }

    @Override
    public String render(ReportData data, DiagramSet diagrams) {
        // JSON does not include diagrams - they are generated separately
        return render(data);
    }

    private void appendMetadata(StringBuilder json, ReportMetadata metadata) {
        json.append("  \"metadata\": {\n");
        json.append("    \"projectName\": ")
                .append(quote(metadata.projectName()))
                .append(",\n");
        json.append("    \"projectVersion\": ")
                .append(quote(metadata.projectVersion()))
                .append(",\n");
        json.append("    \"timestamp\": ")
                .append(quote(ISO_FORMATTER.format(metadata.timestamp())))
                .append(",\n");
        json.append("    \"analysisDuration\": ")
                .append(quote(metadata.duration()))
                .append(",\n");
        json.append("    \"hexaglueVersion\": ")
                .append(quote(metadata.hexaglueVersion()))
                .append(",\n");
        json.append("    \"pluginVersion\": ")
                .append(quote(metadata.pluginVersion()))
                .append("\n");
        json.append("  }");
    }

    private void appendVerdict(StringBuilder json, Verdict verdict) {
        json.append("  \"verdict\": {\n");
        json.append("    \"score\": ").append(verdict.score()).append(",\n");
        json.append("    \"grade\": ").append(quote(verdict.grade())).append(",\n");
        json.append("    \"status\": ").append(quote(verdict.status().name())).append(",\n");
        json.append("    \"statusReason\": ")
                .append(quote(verdict.statusReason()))
                .append(",\n");
        json.append("    \"summary\": ").append(quote(verdict.summary())).append(",\n");

        // KPIs
        json.append("    \"kpis\": ");
        appendKpis(json, verdict.kpis());
        json.append(",\n");

        // Immediate action
        json.append("    \"immediateAction\": {\n");
        json.append("      \"required\": ")
                .append(verdict.immediateAction().required())
                .append(",\n");
        json.append("      \"message\": ")
                .append(quote(verdict.immediateAction().message()))
                .append(",\n");
        json.append("      \"reference\": ")
                .append(quote(verdict.immediateAction().reference()))
                .append("\n");
        json.append("    }\n");
        json.append("  }");
    }

    private void appendKpis(StringBuilder json, List<KPI> kpis) {
        if (kpis.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < kpis.size(); i++) {
            KPI kpi = kpis.get(i);
            json.append("      {\n");
            json.append("        \"id\": ").append(quote(kpi.id())).append(",\n");
            json.append("        \"name\": ").append(quote(kpi.name())).append(",\n");
            json.append("        \"value\": ").append(kpi.value()).append(",\n");
            json.append("        \"unit\": ").append(quote(kpi.unit())).append(",\n");
            json.append("        \"threshold\": ").append(kpi.threshold()).append(",\n");
            json.append("        \"status\": ")
                    .append(quote(kpi.status().name()))
                    .append("\n");
            json.append("      }");
            if (i < kpis.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendArchitecture(StringBuilder json, ArchitectureOverview arch) {
        json.append("  \"architecture\": {\n");
        json.append("    \"summary\": ").append(quote(arch.summary())).append(",\n");

        // Inventory
        json.append("    \"inventory\": {\n");
        json.append("      \"boundedContexts\": ");
        appendBoundedContexts(json, arch.inventory().boundedContexts());
        json.append(",\n");
        json.append("      \"totals\": {\n");
        InventoryTotals totals = arch.inventory().totals();
        json.append("        \"aggregates\": ").append(totals.aggregates()).append(",\n");
        json.append("        \"entities\": ").append(totals.entities()).append(",\n");
        json.append("        \"valueObjects\": ").append(totals.valueObjects()).append(",\n");
        json.append("        \"identifiers\": ").append(totals.identifiers()).append(",\n");
        json.append("        \"domainEvents\": ").append(totals.domainEvents()).append(",\n");
        json.append("        \"domainServices\": ")
                .append(totals.domainServices())
                .append(",\n");
        json.append("        \"applicationServices\": ")
                .append(totals.applicationServices())
                .append(",\n");
        json.append("        \"commandHandlers\": ")
                .append(totals.commandHandlers())
                .append(",\n");
        json.append("        \"queryHandlers\": ")
                .append(totals.queryHandlers())
                .append(",\n");
        json.append("        \"drivingPorts\": ").append(totals.drivingPorts()).append(",\n");
        json.append("        \"drivenPorts\": ").append(totals.drivenPorts()).append("\n");
        json.append("      }\n");
        json.append("    },\n");

        // Components
        json.append("    \"components\": ");
        appendComponents(json, arch.components());
        json.append(",\n");

        // Relationships
        json.append("    \"relationships\": ");
        appendRelationships(json, arch.relationships());
        json.append(",\n");

        // Type violations
        json.append("    \"typeViolations\": ");
        appendTypeViolations(json, arch.typeViolations());
        json.append("\n");

        json.append("  }");
    }

    private void appendBoundedContexts(StringBuilder json, List<BoundedContextInventory> contexts) {
        if (contexts.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < contexts.size(); i++) {
            BoundedContextInventory bc = contexts.get(i);
            json.append("        {\n");
            json.append("          \"name\": ").append(quote(bc.name())).append(",\n");
            json.append("          \"aggregates\": ").append(bc.aggregates()).append(",\n");
            json.append("          \"entities\": ").append(bc.entities()).append(",\n");
            json.append("          \"valueObjects\": ")
                    .append(bc.valueObjects())
                    .append(",\n");
            json.append("          \"domainEvents\": ")
                    .append(bc.domainEvents())
                    .append("\n");
            json.append("        }");
            if (i < contexts.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("      ]");
    }

    private void appendComponents(StringBuilder json, ComponentDetails comp) {
        json.append("{\n");

        // Aggregates
        json.append("      \"aggregates\": ");
        appendAggregates(json, comp.aggregates());
        json.append(",\n");

        // Value Objects
        json.append("      \"valueObjects\": ");
        appendValueObjects(json, comp.valueObjects());
        json.append(",\n");

        // Identifiers
        json.append("      \"identifiers\": ");
        appendIdentifiers(json, comp.identifiers());
        json.append(",\n");

        // Driving Ports
        json.append("      \"drivingPorts\": ");
        appendPorts(json, comp.drivingPorts());
        json.append(",\n");

        // Driven Ports
        json.append("      \"drivenPorts\": ");
        appendPorts(json, comp.drivenPorts());
        json.append(",\n");

        // Adapters
        json.append("      \"adapters\": ");
        appendAdapters(json, comp.adapters());
        json.append("\n");

        json.append("    }");
    }

    private void appendAggregates(StringBuilder json, List<AggregateComponent> aggregates) {
        if (aggregates.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < aggregates.size(); i++) {
            AggregateComponent agg = aggregates.get(i);
            json.append("        {\n");
            json.append("          \"name\": ").append(quote(agg.name())).append(",\n");
            json.append("          \"package\": ")
                    .append(quote(agg.packageName()))
                    .append(",\n");
            json.append("          \"fields\": ").append(agg.fields()).append(",\n");
            json.append("          \"references\": ")
                    .append(toJsonArray(agg.references()))
                    .append(",\n");
            json.append("          \"usesPorts\": ")
                    .append(toJsonArray(agg.usesPorts()))
                    .append("\n");
            json.append("        }");
            if (i < aggregates.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("      ]");
    }

    private void appendValueObjects(StringBuilder json, List<ValueObjectComponent> vos) {
        if (vos.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < vos.size(); i++) {
            ValueObjectComponent vo = vos.get(i);
            json.append("        {\n");
            json.append("          \"name\": ").append(quote(vo.name())).append(",\n");
            json.append("          \"package\": ")
                    .append(quote(vo.packageName()))
                    .append("\n");
            json.append("        }");
            if (i < vos.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("      ]");
    }

    private void appendIdentifiers(StringBuilder json, List<IdentifierComponent> ids) {
        if (ids.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < ids.size(); i++) {
            IdentifierComponent id = ids.get(i);
            json.append("        {\n");
            json.append("          \"name\": ").append(quote(id.name())).append(",\n");
            json.append("          \"package\": ")
                    .append(quote(id.packageName()))
                    .append(",\n");
            json.append("          \"wrappedType\": ")
                    .append(quote(id.wrappedType()))
                    .append("\n");
            json.append("        }");
            if (i < ids.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("      ]");
    }

    private void appendPorts(StringBuilder json, List<PortComponent> ports) {
        if (ports.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < ports.size(); i++) {
            PortComponent port = ports.get(i);
            json.append("        {\n");
            json.append("          \"name\": ").append(quote(port.name())).append(",\n");
            json.append("          \"package\": ")
                    .append(quote(port.packageName()))
                    .append(",\n");
            json.append("          \"kind\": ").append(quote(port.kind())).append(",\n");
            json.append("          \"methods\": ").append(port.methods()).append(",\n");
            json.append("          \"hasAdapter\": ").append(port.hasAdapter()).append(",\n");
            json.append("          \"adapter\": ").append(quote(port.adapter()));
            if (port.orchestrates() != null && !port.orchestrates().isEmpty()) {
                json.append(",\n          \"orchestrates\": ").append(toJsonArray(port.orchestrates()));
            }
            json.append("\n        }");
            if (i < ports.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("      ]");
    }

    private void appendAdapters(StringBuilder json, List<AdapterComponent> adapters) {
        if (adapters.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < adapters.size(); i++) {
            AdapterComponent adapter = adapters.get(i);
            json.append("        {\n");
            json.append("          \"name\": ").append(quote(adapter.name())).append(",\n");
            json.append("          \"package\": ")
                    .append(quote(adapter.packageName()))
                    .append(",\n");
            json.append("          \"implementsPort\": ")
                    .append(quote(adapter.implementsPort()))
                    .append(",\n");
            json.append("          \"type\": ")
                    .append(quote(adapter.type().name()))
                    .append("\n");
            json.append("        }");
            if (i < adapters.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("      ]");
    }

    private void appendRelationships(StringBuilder json, List<Relationship> relationships) {
        if (relationships.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < relationships.size(); i++) {
            Relationship rel = relationships.get(i);
            json.append("      {\n");
            json.append("        \"from\": ").append(quote(rel.from())).append(",\n");
            json.append("        \"to\": ").append(quote(rel.to())).append(",\n");
            json.append("        \"type\": ").append(quote(rel.type())).append(",\n");
            json.append("        \"isCycle\": ").append(rel.isCycle()).append("\n");
            json.append("      }");
            if (i < relationships.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendTypeViolations(StringBuilder json, List<TypeViolation> typeViolations) {
        if (typeViolations.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < typeViolations.size(); i++) {
            TypeViolation tv = typeViolations.get(i);
            json.append("      {\n");
            json.append("        \"typeName\": ").append(quote(tv.typeName())).append(",\n");
            json.append("        \"violationType\": ")
                    .append(quote(tv.violationType().name()))
                    .append("\n");
            json.append("      }");
            if (i < typeViolations.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendIssues(StringBuilder json, IssuesSummary issues) {
        json.append("  \"issues\": {\n");

        // Summary counts
        ViolationCounts counts = issues.summary();
        json.append("    \"summary\": {\n");
        json.append("      \"total\": ").append(counts.total()).append(",\n");
        json.append("      \"blockers\": ").append(counts.blockers()).append(",\n");
        json.append("      \"criticals\": ").append(counts.criticals()).append(",\n");
        json.append("      \"majors\": ").append(counts.majors()).append(",\n");
        json.append("      \"minors\": ").append(counts.minors()).append(",\n");
        json.append("      \"infos\": ").append(counts.infos()).append("\n");
        json.append("    },\n");

        // Groups
        json.append("    \"groups\": ");
        appendIssueGroups(json, issues.groups());
        json.append("\n");

        json.append("  }");
    }

    private void appendIssueGroups(StringBuilder json, List<IssueGroup> groups) {
        if (groups.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < groups.size(); i++) {
            IssueGroup group = groups.get(i);
            json.append("      {\n");
            json.append("        \"id\": ").append(quote(group.id())).append(",\n");
            json.append("        \"theme\": ").append(quote(group.theme())).append(",\n");
            json.append("        \"icon\": ").append(quote(group.icon())).append(",\n");
            json.append("        \"description\": ")
                    .append(quote(group.description()))
                    .append(",\n");
            json.append("        \"count\": ").append(group.count()).append(",\n");
            json.append("        \"violations\": ");
            appendIssueEntries(json, group.violations());
            json.append("\n");
            json.append("      }");
            if (i < groups.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendIssueEntries(StringBuilder json, List<IssueEntry> entries) {
        if (entries.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < entries.size(); i++) {
            IssueEntry entry = entries.get(i);
            json.append("          {\n");
            json.append("            \"id\": ").append(quote(entry.id())).append(",\n");
            json.append("            \"constraintId\": ")
                    .append(quote(entry.constraintId()))
                    .append(",\n");
            json.append("            \"severity\": ")
                    .append(quote(entry.severity().name()))
                    .append(",\n");
            json.append("            \"title\": ").append(quote(entry.title())).append(",\n");
            json.append("            \"message\": ")
                    .append(quote(entry.message()))
                    .append(",\n");
            json.append("            \"location\": {\n");
            json.append("              \"type\": ")
                    .append(quote(entry.location().type()))
                    .append(",\n");
            json.append("              \"file\": ")
                    .append(quote(entry.location().file()))
                    .append(",\n");
            json.append("              \"line\": ")
                    .append(entry.location().lineOpt().map(String::valueOf).orElse("null"))
                    .append("\n");
            json.append("            },\n");
            json.append("            \"impact\": ")
                    .append(quote(entry.impact()))
                    .append(",\n");
            json.append("            \"suggestion\": {\n");
            Suggestion s = entry.suggestion();
            json.append("              \"action\": ").append(quote(s.action())).append(",\n");
            json.append("              \"steps\": ")
                    .append(toJsonArray(s.steps()))
                    .append(",\n");
            json.append("              \"codeExample\": ")
                    .append(quote(s.codeExampleOpt().orElse(null)))
                    .append(",\n");
            json.append("              \"effort\": ")
                    .append(quote(s.effortOpt().orElse(null)))
                    .append("\n");
            json.append("            }\n");
            json.append("          }");
            if (i < entries.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("        ]");
    }

    private void appendRemediation(StringBuilder json, RemediationPlan plan) {
        json.append("  \"remediation\": {\n");
        json.append("    \"summary\": ").append(quote(plan.summary())).append(",\n");

        // Actions
        json.append("    \"actions\": ");
        appendRemediationActions(json, plan.actions());
        json.append(",\n");

        // Total effort
        TotalEffort effort = plan.totalEffort();
        json.append("    \"totalEffort\": {\n");
        json.append("      \"days\": ")
                .append(String.format(Locale.US, "%.1f", effort.days()))
                .append(",\n");
        if (effort.costOpt().isPresent()) {
            CostEstimate cost = effort.costOpt().get();
            json.append("      \"cost\": {\n");
            json.append("        \"amount\": ")
                    .append(String.format(Locale.US, "%.0f", cost.amount()))
                    .append(",\n");
            json.append("        \"currency\": ").append(quote(cost.currency())).append(",\n");
            json.append("        \"dailyRate\": ")
                    .append(String.format(Locale.US, "%.0f", cost.dailyRate()))
                    .append("\n");
            json.append("      }\n");
        } else {
            json.append("      \"cost\": null\n");
        }
        json.append("    }\n");

        json.append("  }");
    }

    private void appendRemediationActions(StringBuilder json, List<RemediationAction> actions) {
        if (actions.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < actions.size(); i++) {
            RemediationAction action = actions.get(i);
            json.append("      {\n");
            json.append("        \"priority\": ").append(action.priority()).append(",\n");
            json.append("        \"severity\": ")
                    .append(quote(action.severity().name()))
                    .append(",\n");
            json.append("        \"title\": ").append(quote(action.title())).append(",\n");
            json.append("        \"description\": ")
                    .append(quote(action.description()))
                    .append(",\n");
            json.append("        \"effort\": {\n");
            json.append("          \"days\": ")
                    .append(String.format(Locale.US, "%.1f", action.effort().days()))
                    .append(",\n");
            json.append("          \"description\": ")
                    .append(quote(action.effort().description()))
                    .append("\n");
            json.append("        },\n");
            json.append("        \"impact\": ").append(quote(action.impact())).append(",\n");
            json.append("        \"affectedTypes\": ")
                    .append(toJsonArray(action.affectedTypes()))
                    .append(",\n");
            json.append("        \"relatedIssues\": ")
                    .append(toJsonArray(action.relatedIssues()))
                    .append("\n");
            json.append("      }");
            if (i < actions.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendAppendix(StringBuilder json, Appendix appendix) {
        json.append("  \"appendix\": {\n");

        // Score breakdown
        json.append("    \"scoreBreakdown\": ");
        appendScoreBreakdown(json, appendix.scoreBreakdown());
        json.append(",\n");

        // Metrics
        json.append("    \"metrics\": ");
        appendMetrics(json, appendix.metrics());
        json.append(",\n");

        // Constraints evaluated
        json.append("    \"constraintsEvaluated\": ");
        appendConstraints(json, appendix.constraintsEvaluated());
        json.append(",\n");

        // Package metrics
        json.append("    \"packageMetrics\": ");
        appendPackageMetrics(json, appendix.packageMetrics());
        json.append("\n");

        json.append("  }");
    }

    private void appendScoreBreakdown(StringBuilder json, ScoreBreakdown breakdown) {
        json.append("{\n");
        json.append("      \"dddCompliance\": ")
                .append(formatDimension(breakdown.dddCompliance()))
                .append(",\n");
        json.append("      \"hexagonalCompliance\": ")
                .append(formatDimension(breakdown.hexagonalCompliance()))
                .append(",\n");
        json.append("      \"dependencyQuality\": ")
                .append(formatDimension(breakdown.dependencyQuality()))
                .append(",\n");
        json.append("      \"couplingMetrics\": ")
                .append(formatDimension(breakdown.couplingMetrics()))
                .append(",\n");
        json.append("      \"cohesionQuality\": ")
                .append(formatDimension(breakdown.cohesionQuality()))
                .append("\n");
        json.append("    }");
    }

    private String formatDimension(ScoreDimension dim) {
        return String.format(Locale.US, "{\"weight\": %d, \"score\": %d}", dim.weight(), dim.score());
    }

    private void appendMetrics(StringBuilder json, List<MetricEntry> metrics) {
        if (metrics.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < metrics.size(); i++) {
            MetricEntry m = metrics.get(i);
            json.append("      {\n");
            json.append("        \"id\": ").append(quote(m.id())).append(",\n");
            json.append("        \"name\": ").append(quote(m.name())).append(",\n");
            json.append("        \"value\": ")
                    .append(String.format(Locale.US, "%.2f", m.value()))
                    .append(",\n");
            json.append("        \"unit\": ").append(quote(m.unit())).append(",\n");
            if (m.threshold() != null) {
                json.append("        \"threshold\": {");
                if (m.threshold().min() != null) {
                    json.append("\"min\": ").append(m.threshold().min());
                    if (m.threshold().max() != null) json.append(", ");
                }
                if (m.threshold().max() != null) {
                    json.append("\"max\": ").append(m.threshold().max());
                }
                json.append("},\n");
            } else {
                json.append("        \"threshold\": null,\n");
            }
            json.append("        \"status\": ").append(quote(m.status().name())).append("\n");
            json.append("      }");
            if (i < metrics.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendConstraints(StringBuilder json, List<ConstraintResult> constraints) {
        if (constraints.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < constraints.size(); i++) {
            ConstraintResult c = constraints.get(i);
            json.append("      {\"id\": ")
                    .append(quote(c.id()))
                    .append(", \"violations\": ")
                    .append(c.violations())
                    .append("}");
            if (i < constraints.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private void appendPackageMetrics(StringBuilder json, List<PackageMetric> packages) {
        if (packages.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < packages.size(); i++) {
            PackageMetric pm = packages.get(i);
            json.append("      {\n");
            json.append("        \"package\": ").append(quote(pm.packageName())).append(",\n");
            json.append("        \"ca\": ").append(pm.ca()).append(",\n");
            json.append("        \"ce\": ").append(pm.ce()).append(",\n");
            json.append("        \"instability\": ")
                    .append(String.format(Locale.US, "%.2f", pm.instability()))
                    .append(",\n");
            json.append("        \"abstractness\": ")
                    .append(String.format(Locale.US, "%.2f", pm.abstractness()))
                    .append(",\n");
            json.append("        \"distance\": ")
                    .append(String.format(Locale.US, "%.2f", pm.distance()))
                    .append(",\n");
            json.append("        \"zone\": ").append(quote(pm.zone().name())).append("\n");
            json.append("      }");
            if (i < packages.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("    ]");
    }

    private String quote(String s) {
        if (s == null) return "null";
        String escaped = s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(quote(list.get(i)));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
