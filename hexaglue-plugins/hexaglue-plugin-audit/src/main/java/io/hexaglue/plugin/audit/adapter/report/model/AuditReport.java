/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit.adapter.report.model;

import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.MetricThreshold;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.ThresholdOperator;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.AuditSnapshot;
import io.hexaglue.spi.audit.RuleViolation;
import java.time.Duration;
import java.time.Instant;
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
 * @since 1.0.0
 */
public record AuditReport(
        ReportMetadata metadata,
        AuditSummary summary,
        List<ViolationEntry> violations,
        List<MetricEntry> metrics,
        ConstraintsSummary constraints,
        ArchitectureAnalysis architectureAnalysis) {

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
        this(metadata, summary, violations, metrics, constraints, ArchitectureAnalysis.empty());
    }

    /**
     * Converts an AuditSnapshot from the SPI to an AuditReport.
     *
     * @param snapshot    the audit snapshot
     * @param projectName the project name
     * @return a new AuditReport
     */
    public static AuditReport from(AuditSnapshot snapshot, String projectName) {
        Objects.requireNonNull(snapshot, "snapshot required");
        Objects.requireNonNull(projectName, "projectName required");

        // Build metadata
        Duration duration = snapshot.metadata().analysisDuration();
        String durationStr = formatDuration(duration);
        ReportMetadata metadata = new ReportMetadata(
                projectName,
                snapshot.metadata().timestamp(),
                durationStr,
                snapshot.metadata().hexaglueVersion());

        // Build summary
        boolean passed = snapshot.passed();
        List<RuleViolation> violations = snapshot.violations();
        AuditSummary summary = new AuditSummary(
                passed,
                violations.size(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.spi.audit.Severity.ERROR)
                        .count(),
                0, // critical - not distinguished in SPI
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.spi.audit.Severity.WARNING)
                        .count(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.spi.audit.Severity.INFO)
                        .count(),
                0 // info
                );

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
            AuditSnapshot snapshot,
            String projectName,
            Map<String, Metric> domainMetrics,
            List<String> constraintIds) {
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

        Objects.requireNonNull(snapshot, "snapshot required");
        Objects.requireNonNull(projectName, "projectName required");
        Objects.requireNonNull(domainMetrics, "domainMetrics required");
        Objects.requireNonNull(constraintIds, "constraintIds required");

        // Build metadata
        Duration duration = snapshot.metadata().analysisDuration();
        String durationStr = formatDuration(duration);
        ReportMetadata metadata = new ReportMetadata(
                projectName,
                snapshot.metadata().timestamp(),
                durationStr,
                snapshot.metadata().hexaglueVersion());

        // Build summary
        boolean passed = snapshot.passed();
        List<RuleViolation> violations = snapshot.violations();
        AuditSummary summary = new AuditSummary(
                passed,
                violations.size(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.spi.audit.Severity.ERROR)
                        .count(),
                0, // critical
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.spi.audit.Severity.WARNING)
                        .count(),
                (int) violations.stream()
                        .filter(v -> v.severity() == io.hexaglue.spi.audit.Severity.INFO)
                        .count(),
                0 // info
                );

        // Convert violations
        List<ViolationEntry> violationEntries =
                violations.stream().map(AuditReport::convertViolation).toList();

        // Convert metrics
        List<MetricEntry> metricEntries = domainMetrics.values().stream()
                .map(AuditReport::convertMetric)
                .toList();

        // Constraints summary
        ConstraintsSummary constraintsSummary = new ConstraintsSummary(constraintIds.size(), constraintIds);

        // Build architecture analysis from query
        ArchitectureAnalysis architectureAnalysis = ArchitectureAnalysis.from(architectureQuery);

        return new AuditReport(
                metadata, summary, violationEntries, metricEntries, constraintsSummary, architectureAnalysis);
    }

    private static ViolationEntry convertViolation(RuleViolation violation) {
        String location = violation.location().filePath() + ":" + violation.location().lineStart() + ":"
                + violation.location().columnStart();
        return new ViolationEntry(
                violation.ruleId(),
                violation.severity().name(),
                violation.message(),
                extractAffectedType(violation.message()),
                location,
                ""); // evidence not in SPI yet
    }

    private static MetricEntry convertMetric(Metric metric) {
        Double threshold = null;
        String thresholdType = null;

        if (metric.threshold().isPresent()) {
            MetricThreshold mt = metric.threshold().get();
            if (mt.operator() == ThresholdOperator.LESS_THAN && mt.min().isPresent()) {
                threshold = mt.min().get();
                thresholdType = "min";
            } else if (mt.operator() == ThresholdOperator.GREATER_THAN && mt.max().isPresent()) {
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
