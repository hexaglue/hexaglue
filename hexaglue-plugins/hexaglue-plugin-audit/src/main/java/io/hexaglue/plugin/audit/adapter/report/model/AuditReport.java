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

package io.hexaglue.plugin.audit.adapter.report.model;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.audit.AuditSnapshot;
import io.hexaglue.arch.model.audit.DetectedArchitectureStyle;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.model.Recommendation;
import io.hexaglue.plugin.audit.domain.model.ThresholdOperator;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.service.ComplianceCalculator;
import io.hexaglue.plugin.audit.domain.service.ExecutiveSummaryBuilder;
import io.hexaglue.plugin.audit.domain.service.HealthScoreCalculator;
import io.hexaglue.plugin.audit.domain.service.InventoryBuilder;
import io.hexaglue.plugin.audit.domain.service.RecommendationGenerator;
import io.hexaglue.spi.audit.ArchitectureQuery;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Complete audit report data for all report formats.
 *
 * <p>This is the unified model that all report generators work with.
 * It consolidates data from the SPI AuditSnapshot and domain AuditResult.
 *
 * @param metadata             report metadata (timestamp, duration, etc.)
 * @param summary              audit summary statistics
 * @param violations           list of all violations
 * @param metrics              list of all metrics
 * @param constraints          constraints summary
 * @param architectureAnalysis architecture analysis data (cycles, coupling, etc.)
 * @param healthScore          overall health score (0-100) with component breakdown
 * @param inventory            component inventory (aggregates, entities, ports, etc.)
 * @param portMatrix           port matrix with adapter coverage
 * @param technicalDebt        technical debt summary
 * @param recommendations      actionable recommendations
 * @param executiveSummary     executive summary for stakeholders
 * @param dddCompliancePercent DDD compliance percentage (0-100)
 * @param hexCompliancePercent hexagonal architecture compliance percentage (0-100)
 * @param detectedStyle        the detected architectural style
 * @param aggregateDetails     detailed information about each aggregate
 * @param model                the architectural model for diagram generation (may be null)
 * @param architectureQuery    the architecture query for diagram generation (may be null)
 * @since 1.0.0
 */
public record AuditReport(
        ReportMetadata metadata,
        AuditSummary summary,
        List<ViolationEntry> violations,
        List<MetricEntry> metrics,
        ConstraintsSummary constraints,
        ArchitectureAnalysis architectureAnalysis,
        HealthScore healthScore,
        ComponentInventory inventory,
        List<PortMatrixEntry> portMatrix,
        TechnicalDebtSummary technicalDebt,
        List<Recommendation> recommendations,
        ExecutiveSummary executiveSummary,
        int dddCompliancePercent,
        int hexCompliancePercent,
        DetectedArchitectureStyle detectedStyle,
        List<AggregateDetails> aggregateDetails,
        ArchitecturalModel model,
        ArchitectureQuery architectureQuery) {

    public AuditReport {
        Objects.requireNonNull(metadata, "metadata required");
        Objects.requireNonNull(summary, "summary required");
        violations = violations != null ? List.copyOf(violations) : List.of();
        metrics = metrics != null ? List.copyOf(metrics) : List.of();
        Objects.requireNonNull(constraints, "constraints required");
        // architectureAnalysis may be null for backward compatibility
        if (architectureAnalysis == null) {
            architectureAnalysis = ArchitectureAnalysis.empty();
        }
        // New fields with defaults
        if (healthScore == null) {
            healthScore = HealthScore.zero();
        }
        if (inventory == null) {
            inventory = ComponentInventory.empty();
        }
        portMatrix = portMatrix != null ? List.copyOf(portMatrix) : List.of();
        if (technicalDebt == null) {
            technicalDebt = TechnicalDebtSummary.zero();
        }
        recommendations = recommendations != null ? List.copyOf(recommendations) : List.of();
        if (executiveSummary == null) {
            executiveSummary = ExecutiveSummary.empty();
        }
        if (dddCompliancePercent < 0 || dddCompliancePercent > 100) {
            throw new IllegalArgumentException("dddCompliancePercent must be 0-100: " + dddCompliancePercent);
        }
        if (hexCompliancePercent < 0 || hexCompliancePercent > 100) {
            throw new IllegalArgumentException("hexCompliancePercent must be 0-100: " + hexCompliancePercent);
        }
        if (detectedStyle == null) {
            detectedStyle = DetectedArchitectureStyle.UNKNOWN;
        }
        aggregateDetails = aggregateDetails != null ? List.copyOf(aggregateDetails) : List.of();
        // model and architectureQuery are intentionally nullable for diagram generation
    }

    /**
     * Legacy constructor for backward compatibility.
     */
    public AuditReport(
            ReportMetadata metadata,
            AuditSummary summary,
            List<ViolationEntry> violations,
            List<MetricEntry> metrics,
            ConstraintsSummary constraints) {
        this(
                metadata,
                summary,
                violations,
                metrics,
                constraints,
                ArchitectureAnalysis.empty(),
                null,
                null,
                null,
                null,
                null,
                null,
                100,
                100,
                DetectedArchitectureStyle.UNKNOWN,
                null,
                null,
                null);
    }

    /**
     * Legacy constructor with architecture analysis for backward compatibility.
     */
    public AuditReport(
            ReportMetadata metadata,
            AuditSummary summary,
            List<ViolationEntry> violations,
            List<MetricEntry> metrics,
            ConstraintsSummary constraints,
            ArchitectureAnalysis architectureAnalysis) {
        this(
                metadata,
                summary,
                violations,
                metrics,
                constraints,
                architectureAnalysis,
                null,
                null,
                null,
                null,
                null,
                null,
                100,
                100,
                DetectedArchitectureStyle.UNKNOWN,
                null,
                null,
                null);
    }

    /**
     * Converts an AuditSnapshot from the SPI to an AuditReport.
     *
     * @param snapshot    the audit snapshot
     * @param projectName the project name
     * @return a new AuditReport
     */
    public static AuditReport from(AuditSnapshot snapshot, String projectName) {
        return from(snapshot, projectName, null);
    }

    /**
     * Converts an AuditSnapshot from the SPI to an AuditReport with project version.
     *
     * @param snapshot       the audit snapshot
     * @param projectName    the project name
     * @param projectVersion the project version (can be null)
     * @return a new AuditReport
     * @since 3.0.0
     */
    public static AuditReport from(AuditSnapshot snapshot, String projectName, String projectVersion) {
        Objects.requireNonNull(snapshot, "snapshot required");
        Objects.requireNonNull(projectName, "projectName required");

        // Build metadata
        Duration duration = snapshot.metadata().analysisDuration();
        String durationStr = formatDuration(duration);
        ReportMetadata metadata = new ReportMetadata(
                projectName,
                projectVersion,
                snapshot.metadata().timestamp(),
                durationStr,
                snapshot.metadata().hexaglueVersion());

        // Build summary
        boolean passed = snapshot.passed();
        List<RuleViolation> violations = snapshot.violations();
        AuditSummary summary = buildSummary(passed, violations);

        // Convert violations
        List<ViolationEntry> violationEntries =
                violations.stream().map(AuditReport::convertViolation).toList();

        // Convert metrics (empty for now - will be populated when domain metrics are available)
        List<MetricEntry> metricEntries = List.of();

        // Constraints summary
        ConstraintsSummary constraintsSummary = new ConstraintsSummary(0, List.of());

        return new AuditReport(metadata, summary, violationEntries, metricEntries, constraintsSummary);
    }

    /**
     * Converts an AuditSnapshot with domain AuditResult to an AuditReport.
     *
     * @param snapshot       the audit snapshot
     * @param projectName    the project name
     * @param domainMetrics  the domain metrics map
     * @param constraintIds  the list of checked constraint IDs
     * @return a new AuditReport
     */
    public static AuditReport from(
            AuditSnapshot snapshot, String projectName, Map<String, Metric> domainMetrics, List<String> constraintIds) {
        return from(snapshot, projectName, domainMetrics, constraintIds, null);
    }

    /**
     * Converts an AuditSnapshot with domain AuditResult and architecture analysis to an AuditReport.
     *
     * <p>This is the most complete factory method that includes architecture analysis
     * from the core's ArchitectureQuery. When available, the report will include
     * dependency cycles, layer violations, stability violations, and coupling metrics.
     *
     * @param snapshot          the audit snapshot
     * @param projectName       the project name
     * @param domainMetrics     the domain metrics map
     * @param constraintIds     the list of checked constraint IDs
     * @param architectureQuery the architecture query for advanced analysis (may be null)
     * @return a new AuditReport
     */
    public static AuditReport from(
            AuditSnapshot snapshot,
            String projectName,
            Map<String, Metric> domainMetrics,
            List<String> constraintIds,
            ArchitectureQuery architectureQuery) {
        return from(snapshot, projectName, null, domainMetrics, constraintIds, architectureQuery);
    }

    /**
     * Converts an AuditSnapshot with project version and architecture analysis to an AuditReport.
     *
     * @param snapshot          the audit snapshot
     * @param projectName       the project name
     * @param projectVersion    the project version (can be null)
     * @param domainMetrics     the domain metrics map
     * @param constraintIds     the list of checked constraint IDs
     * @param architectureQuery the architecture query for advanced analysis (may be null)
     * @return a new AuditReport
     * @since 3.0.0
     */
    public static AuditReport from(
            AuditSnapshot snapshot,
            String projectName,
            String projectVersion,
            Map<String, Metric> domainMetrics,
            List<String> constraintIds,
            ArchitectureQuery architectureQuery) {

        Objects.requireNonNull(snapshot, "snapshot required");
        Objects.requireNonNull(projectName, "projectName required");
        Objects.requireNonNull(domainMetrics, "domainMetrics required");
        Objects.requireNonNull(constraintIds, "constraintIds required");

        // Build metadata
        Duration duration = snapshot.metadata().analysisDuration();
        String durationStr = formatDuration(duration);
        ReportMetadata metadata = new ReportMetadata(
                projectName,
                projectVersion,
                snapshot.metadata().timestamp(),
                durationStr,
                snapshot.metadata().hexaglueVersion());

        // Build summary
        boolean passed = snapshot.passed();
        List<RuleViolation> violations = snapshot.violations();
        AuditSummary summary = buildSummary(passed, violations);

        // Convert violations
        List<ViolationEntry> violationEntries =
                violations.stream().map(AuditReport::convertViolation).toList();

        // Convert metrics
        List<MetricEntry> metricEntries =
                domainMetrics.values().stream().map(AuditReport::convertMetric).toList();

        // Constraints summary
        ConstraintsSummary constraintsSummary = new ConstraintsSummary(constraintIds.size(), constraintIds);

        // Build architecture analysis from query
        ArchitectureAnalysis architectureAnalysis = ArchitectureAnalysis.from(architectureQuery);

        return new AuditReport(
                metadata, summary, violationEntries, metricEntries, constraintsSummary, architectureAnalysis);
    }

    /**
     * Creates a complete AuditReport with all enriched data from domain services.
     *
     * <p>This factory method uses all domain services to compute:
     * <ul>
     *   <li>Health score with weighted components</li>
     *   <li>Component inventory from ArchitecturalModel</li>
     *   <li>Port matrix with adapter coverage</li>
     *   <li>Technical debt estimation</li>
     *   <li>Actionable recommendations</li>
     *   <li>Executive summary for stakeholders</li>
     *   <li>DDD and hexagonal compliance percentages</li>
     * </ul>
     *
     * <p>Project name and version are extracted from the model's project context.
     *
     * @param snapshot          the audit snapshot
     * @param projectName       the project name (overrides model metadata if provided)
     * @param domainMetrics     the domain metrics map
     * @param constraintIds     the list of checked constraint IDs
     * @param architectureQuery the architecture query (may be null)
     * @param model             the architectural model for inventory building and project metadata
     * @param domainViolations  the domain violations for recommendations
     * @return a new complete AuditReport
     */
    public static AuditReport fromComplete(
            AuditSnapshot snapshot,
            String projectName,
            Map<String, Metric> domainMetrics,
            List<String> constraintIds,
            ArchitectureQuery architectureQuery,
            ArchitecturalModel model,
            List<Violation> domainViolations) {

        Objects.requireNonNull(snapshot, "snapshot required");
        Objects.requireNonNull(projectName, "projectName required");
        Objects.requireNonNull(domainMetrics, "domainMetrics required");
        Objects.requireNonNull(constraintIds, "constraintIds required");
        Objects.requireNonNull(model, "model required");
        Objects.requireNonNull(domainViolations, "domainViolations required");

        // Build metadata - use model's project context for project name and version
        Duration duration = snapshot.metadata().analysisDuration();
        String durationStr = formatDuration(duration);
        String effectiveProjectName =
                model.project().name() != null ? model.project().name() : projectName;
        String projectVersion = model.project().version().orElse(null);
        ReportMetadata metadata = new ReportMetadata(
                effectiveProjectName,
                projectVersion,
                snapshot.metadata().timestamp(),
                durationStr,
                snapshot.metadata().hexaglueVersion());

        // Build summary
        boolean passed = snapshot.passed();
        List<RuleViolation> violations = snapshot.violations();
        AuditSummary summary = buildSummary(passed, violations);

        // Convert violations
        List<ViolationEntry> violationEntries =
                violations.stream().map(AuditReport::convertViolation).toList();

        // Convert metrics
        List<MetricEntry> metricEntries =
                domainMetrics.values().stream().map(AuditReport::convertMetric).toList();

        // Constraints summary
        ConstraintsSummary constraintsSummary = new ConstraintsSummary(constraintIds.size(), constraintIds);

        // Build architecture analysis from query
        ArchitectureAnalysis architectureAnalysis = ArchitectureAnalysis.from(architectureQuery);

        // === NEW: Build enriched data using domain services ===

        // Build component inventory from model (using architecture query for bounded contexts)
        InventoryBuilder inventoryBuilder = new InventoryBuilder();
        ComponentInventory inventory = inventoryBuilder.build(model, architectureQuery);

        // Calculate compliance scores
        ComplianceCalculator complianceCalculator = new ComplianceCalculator();
        int dddCompliance = complianceCalculator.calculateDddCompliance(domainViolations);
        int hexCompliance = complianceCalculator.calculateHexCompliance(domainViolations);

        // Calculate health score
        HealthScoreCalculator healthCalculator = new HealthScoreCalculator(complianceCalculator);
        HealthScore healthScore = healthCalculator.calculate(domainViolations, architectureQuery);

        // Build port matrix from model's ports
        List<PortMatrixEntry> portMatrix = PortMatrixEntry.fromModel(model);

        // Generate recommendations
        RecommendationGenerator recGenerator = new RecommendationGenerator();
        List<Recommendation> recommendations =
                recGenerator.generate(domainViolations, architectureAnalysis, domainMetrics);

        // Calculate technical debt
        TechnicalDebtSummary technicalDebt = TechnicalDebtSummary.fromDays(
                recommendations.stream()
                        .mapToDouble(Recommendation::estimatedEffort)
                        .sum(),
                List.of());

        // Build executive summary
        ExecutiveSummaryBuilder summaryBuilder = new ExecutiveSummaryBuilder();
        ExecutiveSummary executiveSummary = summaryBuilder.build(
                effectiveProjectName,
                healthScore,
                inventory,
                domainViolations,
                architectureAnalysis,
                domainMetrics,
                dddCompliance,
                hexCompliance);

        // Build aggregate details
        List<AggregateDetails> aggregateDetails = AggregateDetails.fromQuery(architectureQuery);

        return new AuditReport(
                metadata,
                summary,
                violationEntries,
                metricEntries,
                constraintsSummary,
                architectureAnalysis,
                healthScore,
                inventory,
                portMatrix,
                technicalDebt,
                recommendations,
                executiveSummary,
                dddCompliance,
                hexCompliance,
                snapshot.style(),
                aggregateDetails,
                model,
                architectureQuery);
    }

    /**
     * Builds the audit summary with violation counts by severity.
     *
     * @param passed whether the audit passed
     * @param violations the list of rule violations
     * @return the audit summary with counts for each severity level
     */
    private static AuditSummary buildSummary(boolean passed, List<RuleViolation> violations) {
        return new AuditSummary(
                passed,
                violations.size(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.arch.model.audit.Severity.BLOCKER)
                        .count(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.arch.model.audit.Severity.CRITICAL)
                        .count(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.arch.model.audit.Severity.MAJOR)
                        .count(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.arch.model.audit.Severity.MINOR)
                        .count(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.arch.model.audit.Severity.INFO)
                        .count());
    }

    private static ViolationEntry convertViolation(RuleViolation violation) {
        String location = violation.location().filePath() + ":"
                + violation.location().lineStart() + ":" + violation.location().columnStart();

        // Use affected types from violation, fallback to message extraction if empty
        String affectedType = violation.affectedTypes().isEmpty()
                ? extractAffectedType(violation.message())
                : String.join(", ", violation.affectedTypes());

        return new ViolationEntry(
                violation.ruleId(),
                violation.severity().name(),
                violation.message(),
                affectedType,
                location,
                violation.evidence());
    }

    private static MetricEntry convertMetric(Metric metric) {
        Double threshold = null;
        String thresholdType = null;

        if (metric.threshold().isPresent()) {
            MetricThreshold mt = metric.threshold().get();
            if (mt.operator() == ThresholdOperator.LESS_THAN && mt.min().isPresent()) {
                threshold = mt.min().get();
                thresholdType = "min";
            } else if (mt.operator() == ThresholdOperator.GREATER_THAN
                    && mt.max().isPresent()) {
                threshold = mt.max().get();
                thresholdType = "max";
            } else if (mt.operator() == ThresholdOperator.BETWEEN) {
                // Use max for between thresholds
                threshold = mt.max().orElse(null);
                thresholdType = "max";
            }
        }

        String status = metric.exceedsThreshold() ? "WARNING" : "OK";

        return new MetricEntry(metric.name(), metric.value(), metric.unit(), threshold, thresholdType, status);
    }

    private static String extractAffectedType(String message) {
        // Simple heuristic: look for qualified class names in the message
        String[] words = message.split("\\s+");
        for (String word : words) {
            if (word.contains(".") && Character.isUpperCase(word.charAt(word.lastIndexOf('.') + 1))) {
                return word.replaceAll("[^a-zA-Z0-9.]", "");
            }
        }
        return "unknown";
    }

    private static String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        }
        double seconds = millis / 1000.0;
        return String.format("%.2fs", seconds);
    }
}
