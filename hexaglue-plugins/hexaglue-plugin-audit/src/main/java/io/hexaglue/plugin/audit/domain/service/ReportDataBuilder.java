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

package io.hexaglue.plugin.audit.domain.service;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.audit.BoundedContextInfo;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.*;
import io.hexaglue.arch.model.audit.AuditSnapshot;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds {@link ReportData} from audit execution results.
 *
 * <p>This service aggregates data from various sources (AuditResult, ArchitecturalModel,
 * ArchitectureQuery) into a structured {@link ReportData} that can be serialized to JSON
 * and rendered to HTML/Markdown.
 *
 * @since 5.0.0
 */
public class ReportDataBuilder {

    private static final double DEFAULT_DAILY_RATE = 500.0;
    private static final String DEFAULT_CURRENCY = "EUR";

    private final HealthScoreCalculator healthScoreCalculator;
    private final IssueEnricher issueEnricher;

    /**
     * Creates a ReportDataBuilder with default dependencies.
     */
    public ReportDataBuilder() {
        this(new HealthScoreCalculator(), new IssueEnricher());
    }

    /**
     * Creates a ReportDataBuilder with custom dependencies.
     *
     * @param healthScoreCalculator the health score calculator
     * @param issueEnricher the issue enricher
     */
    public ReportDataBuilder(HealthScoreCalculator healthScoreCalculator, IssueEnricher issueEnricher) {
        this.healthScoreCalculator = Objects.requireNonNull(healthScoreCalculator);
        this.issueEnricher = Objects.requireNonNull(issueEnricher);
    }

    /**
     * Builds the report data from audit execution results.
     *
     * @param snapshot audit snapshot
     * @param auditResult domain audit result
     * @param architectureQuery architecture query interface
     * @param model architectural model
     * @param projectName project name
     * @param projectVersion project version
     * @param hexaglueVersion HexaGlue version
     * @param pluginVersion plugin version
     * @param duration audit duration
     * @return the report data
     */
    public ReportData build(
            AuditSnapshot snapshot,
            AuditResult auditResult,
            ArchitectureQuery architectureQuery,
            ArchitecturalModel model,
            String projectName,
            String projectVersion,
            String hexaglueVersion,
            String pluginVersion,
            Duration duration) {

        // Build metadata
        ReportMetadata metadata = new ReportMetadata(
                projectName,
                projectVersion,
                Instant.now(),
                formatDuration(duration),
                hexaglueVersion,
                pluginVersion);

        // Calculate health score
        HealthScore healthScore = healthScoreCalculator.calculate(auditResult.violations(), architectureQuery);

        // Build sections
        Verdict verdict = buildVerdict(auditResult, healthScore);
        ArchitectureOverview architecture = buildArchitecture(model, architectureQuery);
        IssuesSummary issues = buildIssues(auditResult.violations());
        RemediationPlan remediation = buildRemediation(auditResult.violations(), issues);
        Appendix appendix = buildAppendix(healthScore, auditResult, architectureQuery);

        return ReportData.create(metadata, verdict, architecture, issues, remediation, appendix);
    }

    private Verdict buildVerdict(AuditResult auditResult, HealthScore healthScore) {
        int score = healthScore.overall();
        String grade = healthScore.grade();
        ReportStatus status = determineStatus(auditResult.violations());
        String statusReason = determineStatusReason(auditResult.violations());
        String summary = buildSummary(auditResult, healthScore);

        List<KPI> kpis = List.of(
                KPI.percentage("ddd-compliance", "DDD Compliance", healthScore.dddCompliance(), 25, 90),
                KPI.percentage("hexagonal-compliance", "Hexagonal Architecture", healthScore.hexCompliance(), 25, 90),
                KPI.percentage("dependency-quality", "Dependencies", healthScore.dependencyQuality(), 20, 80),
                KPI.percentage("coupling-metrics", "Coupling", healthScore.coupling(), 15, 70),
                KPI.percentage("cohesion-quality", "Cohesion", healthScore.cohesion(), 15, 80));

        ImmediateAction immediateAction = buildImmediateAction(auditResult.violations());

        return new Verdict(score, grade, status, statusReason, summary, kpis, immediateAction);
    }

    private ReportStatus determineStatus(List<Violation> violations) {
        boolean hasBlocker = violations.stream().anyMatch(v -> v.severity() == Severity.BLOCKER);
        boolean hasCritical = violations.stream().anyMatch(v -> v.severity() == Severity.CRITICAL);

        if (hasBlocker) {
            return ReportStatus.FAILED;
        }
        if (hasCritical) {
            return ReportStatus.FAILED;
        }
        if (!violations.isEmpty()) {
            return ReportStatus.PASSED_WITH_WARNINGS;
        }
        return ReportStatus.PASSED;
    }

    private String determineStatusReason(List<Violation> violations) {
        long blockers = violations.stream().filter(v -> v.severity() == Severity.BLOCKER).count();
        long criticals = violations.stream().filter(v -> v.severity() == Severity.CRITICAL).count();

        if (blockers > 0) {
            return blockers + " blocking issue" + (blockers > 1 ? "s" : "") + " found";
        }
        if (criticals > 0) {
            return criticals + " critical issue" + (criticals > 1 ? "s" : "") + " found";
        }
        if (!violations.isEmpty()) {
            return violations.size() + " issue" + (violations.size() > 1 ? "s" : "") + " found";
        }
        return "All checks passed";
    }

    private String buildSummary(AuditResult auditResult, HealthScore healthScore) {
        if (hasBlockers(auditResult.violations())) {
            return "Your application requires immediate attention. "
                    + "Critical architectural violations must be resolved before deployment.";
        }
        if (hasCriticals(auditResult.violations())) {
            return "Your application has critical issues that should be addressed. "
                    + "Review the issues section for details.";
        }
        if (!auditResult.violations().isEmpty()) {
            return "Your application is generally healthy but has some issues that should be reviewed.";
        }
        return "Congratulations! Your application follows all architectural best practices.";
    }

    private ImmediateAction buildImmediateAction(List<Violation> violations) {
        Optional<Violation> blocker = violations.stream()
                .filter(v -> v.severity() == Severity.BLOCKER)
                .findFirst();

        if (blocker.isPresent()) {
            Violation v = blocker.get();
            String message = "Fix " + issueEnricher.extractTitle(v);
            String reference = "#" + issueEnricher.generateId(v);
            return ImmediateAction.required(message, reference);
        }
        return ImmediateAction.none();
    }

    private ArchitectureOverview buildArchitecture(ArchitecturalModel model, ArchitectureQuery architectureQuery) {
        DomainIndex domainIndex = model.domainIndex().orElse(null);
        PortIndex portIndex = model.portIndex().orElse(null);

        // Build inventory
        List<BoundedContextInventory> bcInventories = new ArrayList<>();
        if (architectureQuery != null && domainIndex != null) {
            for (BoundedContextInfo bc : architectureQuery.findBoundedContexts()) {
                // Count aggregates and value objects by filtering domain types belonging to this bounded context
                int aggregateCount = (int) domainIndex.aggregateRoots()
                        .filter(ar -> bc.containsType(ar.id().qualifiedName()))
                        .count();
                int voCount = (int) domainIndex.valueObjects()
                        .filter(vo -> bc.containsType(vo.id().qualifiedName()))
                        .count();
                bcInventories.add(new BoundedContextInventory(bc.name(), aggregateCount, 0, voCount, 0));
            }
        }

        InventoryTotals totals = calculateTotals(domainIndex, portIndex);
        Inventory inventory = new Inventory(bcInventories, totals);

        // Build component details
        ComponentDetails components = buildComponentDetails(domainIndex, portIndex, architectureQuery);

        // Build relationships
        List<Relationship> relationships = buildRelationships(model, architectureQuery);

        String summary = String.format(
                "Analyzed %d types across %d bounded context%s",
                totals.total(), bcInventories.size(), bcInventories.size() != 1 ? "s" : "");

        return new ArchitectureOverview(summary, inventory, components, DiagramsInfo.defaults(), relationships);
    }

    private InventoryTotals calculateTotals(DomainIndex domainIndex, PortIndex portIndex) {
        int aggregates = domainIndex != null ? (int) domainIndex.aggregateRoots().count() : 0;
        int entities = domainIndex != null ? (int) domainIndex.entities().count() : 0;
        int valueObjects = domainIndex != null ? (int) domainIndex.valueObjects().count() : 0;
        int identifiers = domainIndex != null ? (int) domainIndex.identifiers().count() : 0;
        int domainEvents = domainIndex != null ? (int) domainIndex.domainEvents().count() : 0;
        int drivingPorts = portIndex != null ? (int) portIndex.drivingPorts().count() : 0;
        int drivenPorts = portIndex != null ? (int) portIndex.drivenPorts().count() : 0;

        return new InventoryTotals(aggregates, entities, valueObjects, identifiers, domainEvents, drivingPorts, drivenPorts);
    }

    private ComponentDetails buildComponentDetails(
            DomainIndex domainIndex, PortIndex portIndex, ArchitectureQuery architectureQuery) {

        List<AggregateComponent> aggregates = new ArrayList<>();
        List<ValueObjectComponent> valueObjects = new ArrayList<>();
        List<IdentifierComponent> identifiers = new ArrayList<>();
        List<PortComponent> drivingPorts = new ArrayList<>();
        List<PortComponent> drivenPorts = new ArrayList<>();
        List<AdapterComponent> adapters = new ArrayList<>();

        if (domainIndex != null) {
            for (AggregateRoot agg : domainIndex.aggregateRoots().toList()) {
                aggregates.add(new AggregateComponent(
                        agg.id().simpleName(),
                        extractPackage(agg.id().qualifiedName()),
                        agg.structure().fields().size(),
                        List.of(), // References extracted from relationships
                        List.of()  // Ports extracted from relationships
                ));
            }

            for (ValueObject vo : domainIndex.valueObjects().toList()) {
                valueObjects.add(new ValueObjectComponent(
                        vo.id().simpleName(),
                        extractPackage(vo.id().qualifiedName())));
            }

            for (Identifier id : domainIndex.identifiers().toList()) {
                identifiers.add(new IdentifierComponent(
                        id.id().simpleName(),
                        extractPackage(id.id().qualifiedName()),
                        id.wrappedType().qualifiedName()));
            }
        }

        if (portIndex != null) {
            for (DrivingPort port : portIndex.drivingPorts().toList()) {
                drivingPorts.add(PortComponent.driving(
                        port.id().simpleName(),
                        extractPackage(port.id().qualifiedName()),
                        port.structure().methods().size(),
                        false,
                        null,
                        List.of()));
            }

            for (DrivenPort port : portIndex.drivenPorts().toList()) {
                drivenPorts.add(PortComponent.driven(
                        port.id().simpleName(),
                        extractPackage(port.id().qualifiedName()),
                        port.portType().name(),
                        port.structure().methods().size(),
                        false,
                        null));
            }
        }

        return new ComponentDetails(aggregates, valueObjects, identifiers, drivingPorts, drivenPorts, adapters);
    }

    private List<Relationship> buildRelationships(ArchitecturalModel model, ArchitectureQuery architectureQuery) {
        List<Relationship> relationships = new ArrayList<>();

        if (architectureQuery != null) {
            // Find dependency cycles (type-level cycles can indicate aggregate cycles)
            var cycles = architectureQuery.findDependencyCycles();
            for (var cycle : cycles) {
                List<String> path = cycle.path();
                for (int i = 0; i < path.size() - 1; i++) {
                    String from = simpleName(path.get(i));
                    String to = simpleName(path.get(i + 1));
                    relationships.add(Relationship.cycle(from, to, "references"));
                }
            }
        }

        return relationships;
    }

    private IssuesSummary buildIssues(List<Violation> violations) {
        if (violations.isEmpty()) {
            return IssuesSummary.empty();
        }

        // Enrich violations
        List<IssueEntry> entries = violations.stream()
                .map(issueEnricher::enrich)
                .toList();

        // Group by theme
        Map<String, List<IssueEntry>> byTheme = entries.stream()
                .collect(Collectors.groupingBy(this::determineTheme));

        List<IssueGroup> groups = new ArrayList<>();
        for (var entry : byTheme.entrySet()) {
            String theme = entry.getKey();
            List<IssueEntry> themeViolations = entry.getValue();
            groups.add(IssueGroup.of(
                    themeId(theme),
                    theme,
                    themeIcon(theme),
                    themeDescription(theme),
                    themeViolations));
        }

        // Sort groups by severity (most severe first)
        groups.sort((a, b) -> {
            int aMax = maxSeverity(a.violations());
            int bMax = maxSeverity(b.violations());
            return Integer.compare(aMax, bMax);
        });

        ViolationCounts counts = ViolationCounts.fromIssues(entries);
        return new IssuesSummary(counts, groups);
    }

    private String determineTheme(IssueEntry entry) {
        String constraintId = entry.constraintId();
        if (constraintId.startsWith("ddd:")) {
            return "Domain Model Issues";
        }
        if (constraintId.startsWith("hexagonal:")) {
            return "Ports & Adapters Issues";
        }
        return "Other Issues";
    }

    private String themeId(String theme) {
        return theme.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private String themeIcon(String theme) {
        if (theme.contains("Domain")) return "domain";
        if (theme.contains("Port")) return "ports";
        return "other";
    }

    private String themeDescription(String theme) {
        if (theme.contains("Domain")) {
            return "Problems with DDD tactical patterns that affect domain integrity";
        }
        if (theme.contains("Port")) {
            return "Problems with hexagonal architecture implementation";
        }
        return "Other architectural issues";
    }

    private int maxSeverity(List<IssueEntry> entries) {
        return entries.stream()
                .mapToInt(e -> e.severity().ordinal())
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private RemediationPlan buildRemediation(List<Violation> violations, IssuesSummary issues) {
        if (violations.isEmpty()) {
            return RemediationPlan.empty();
        }

        List<RemediationAction> actions = new ArrayList<>();
        double totalDays = 0;

        // Group issues by corrective action
        Map<String, List<IssueEntry>> byCorrection = issues.allIssues().stream()
                .collect(Collectors.groupingBy(e -> e.suggestion().action()));

        int priority = 1;
        for (var entry : byCorrection.entrySet().stream()
                .sorted((a, b) -> maxSeverity(a.getValue()) - maxSeverity(b.getValue()))
                .toList()) {

            List<IssueEntry> relatedIssues = entry.getValue();
            IssueEntry first = relatedIssues.get(0);

            double effortDays = parseEffort(first.suggestion().effortOpt().orElse("1 day"));
            totalDays += effortDays;

            List<String> affectedTypes = relatedIssues.stream()
                    .map(e -> e.location().type())
                    .distinct()
                    .toList();

            List<String> issueIds = relatedIssues.stream()
                    .map(IssueEntry::id)
                    .toList();

            actions.add(RemediationAction.builder()
                    .priority(priority++)
                    .severity(first.severity())
                    .title(entry.getKey())
                    .description(first.impact())
                    .effort(effortDays, "Estimated effort")
                    .impact("Resolves " + relatedIssues.size() + " issue(s)")
                    .affectedTypes(affectedTypes)
                    .relatedIssues(issueIds)
                    .build());
        }

        String summary = String.format(
                "%d action%s required to achieve compliance. Estimated total effort: %.1f days.",
                actions.size(),
                actions.size() != 1 ? "s" : "",
                totalDays);

        TotalEffort totalEffort = TotalEffort.withCost(totalDays, DEFAULT_DAILY_RATE, DEFAULT_CURRENCY);

        return new RemediationPlan(summary, actions, totalEffort);
    }

    private double parseEffort(String effort) {
        if (effort == null) return 1.0;
        try {
            return Double.parseDouble(effort.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    private Appendix buildAppendix(HealthScore healthScore, AuditResult auditResult, ArchitectureQuery architectureQuery) {
        ScoreBreakdown breakdown = new ScoreBreakdown(
                ScoreDimension.of(25, healthScore.dddCompliance()),
                ScoreDimension.of(25, healthScore.hexCompliance()),
                ScoreDimension.of(20, healthScore.dependencyQuality()),
                ScoreDimension.of(15, healthScore.coupling()),
                ScoreDimension.of(15, healthScore.cohesion()));

        // Metrics from audit result (Map<String, Metric>)
        List<MetricEntry> metrics = auditResult.metrics().entrySet().stream()
                .map(entry -> {
                    String metricId = entry.getKey();
                    Metric m = entry.getValue();
                    return new MetricEntry(
                            metricId,
                            m.name(),
                            m.value(),
                            m.unit(),
                            m.threshold().map(t -> new MetricEntry.MetricThreshold(
                                    t.min().orElse(null),
                                    t.max().orElse(null))).orElse(null),
                            m.exceedsThreshold() ? KpiStatus.CRITICAL : KpiStatus.OK);
                })
                .toList();

        // Constraints evaluated
        Map<String, Long> constraintViolations = auditResult.violations().stream()
                .collect(Collectors.groupingBy(v -> v.constraintId().value(), Collectors.counting()));

        List<ConstraintResult> constraintsEvaluated = constraintViolations.entrySet().stream()
                .map(e -> new ConstraintResult(e.getKey(), e.getValue().intValue()))
                .toList();

        // Package metrics using analyzeAllPackageCoupling
        List<PackageMetric> packageMetrics = List.of();
        if (architectureQuery != null) {
            packageMetrics = architectureQuery.analyzeAllPackageCoupling().stream()
                    .map(pm -> new PackageMetric(
                            pm.packageName(),
                            pm.afferentCoupling(),
                            pm.efferentCoupling(),
                            pm.instability(),
                            pm.abstractness(),
                            pm.distanceFromMainSequence(),
                            PackageMetric.calculateZone(pm.instability(), pm.abstractness())))
                    .toList();
        }

        return new Appendix(breakdown, metrics, constraintsEvaluated, packageMetrics);
    }

    private boolean hasBlockers(List<Violation> violations) {
        return violations.stream().anyMatch(v -> v.severity() == Severity.BLOCKER);
    }

    private boolean hasCriticals(List<Violation> violations) {
        return violations.stream().anyMatch(v -> v.severity() == Severity.CRITICAL);
    }

    private String formatDuration(Duration duration) {
        if (duration == null) return "N/A";
        long millis = duration.toMillis();
        if (millis < 1000) return millis + "ms";
        return String.format("%.1fs", millis / 1000.0);
    }

    private String extractPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    private String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
