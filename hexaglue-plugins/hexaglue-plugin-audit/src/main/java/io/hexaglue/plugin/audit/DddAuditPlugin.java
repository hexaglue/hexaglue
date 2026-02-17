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

package io.hexaglue.plugin.audit;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.audit.ArchitectureMetrics;
import io.hexaglue.arch.model.audit.AuditMetadata;
import io.hexaglue.arch.model.audit.AuditSnapshot;
import io.hexaglue.arch.model.audit.CodeMetrics;
import io.hexaglue.arch.model.audit.CodeUnit;
import io.hexaglue.arch.model.audit.CodeUnitKind;
import io.hexaglue.arch.model.audit.Codebase;
import io.hexaglue.arch.model.audit.CouplingMetrics;
import io.hexaglue.arch.model.audit.DetectedArchitectureStyle;
import io.hexaglue.arch.model.audit.DocumentationInfo;
import io.hexaglue.arch.model.audit.LayerClassification;
import io.hexaglue.arch.model.audit.QualityMetrics;
import io.hexaglue.arch.model.audit.RoleClassification;
import io.hexaglue.arch.model.audit.RuleViolation;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.arch.model.report.ClassificationReport;
import io.hexaglue.plugin.audit.adapter.diagram.DiagramGenerator;
import io.hexaglue.plugin.audit.adapter.metric.AggregateBoundaryMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.AggregateMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.BoilerplateMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.CodeComplexityMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.CouplingMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.DomainCoverageMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.DomainPurityMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.PortCoverageMetricCalculator;
import io.hexaglue.plugin.audit.adapter.report.ConsoleRenderer;
import io.hexaglue.plugin.audit.adapter.report.HtmlRenderer;
import io.hexaglue.plugin.audit.adapter.report.JsonReportRenderer;
import io.hexaglue.plugin.audit.adapter.report.MarkdownRenderer;
import io.hexaglue.plugin.audit.adapter.report.ReportFormat;
import io.hexaglue.plugin.audit.adapter.report.ReportRenderer;
import io.hexaglue.plugin.audit.config.AuditConfiguration;
import io.hexaglue.plugin.audit.config.ConstraintRegistry;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.DiagramSet;
import io.hexaglue.plugin.audit.domain.model.report.ReportData;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.plugin.audit.domain.service.AuditOrchestrator;
import io.hexaglue.plugin.audit.domain.service.ConstraintEngine;
import io.hexaglue.plugin.audit.domain.service.MetricAggregator;
import io.hexaglue.plugin.audit.domain.service.ReportDataBuilder;
import io.hexaglue.spi.audit.AuditContext;
import io.hexaglue.spi.audit.AuditPlugin;
import io.hexaglue.spi.plugin.PluginConfig;
import io.hexaglue.spi.plugin.PluginContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * DDD Audit Plugin for HexaGlue.
 *
 * <p>This plugin validates Domain-Driven Design and hexagonal architecture
 * constraints, producing an audit snapshot with violations and quality metrics.
 *
 * <p><strong>Default Constraints:</strong>
 * <ul>
 *   <li>ddd:entity-identity - Entities must have identity fields</li>
 *   <li>ddd:aggregate-repository - Aggregate roots must have repositories</li>
 *   <li>ddd:value-object-immutable - Value objects must be immutable</li>
 * </ul>
 *
 * <p><strong>Configuration:</strong> (via plugin config)
 * <pre>
 * audit.severity.ddd:entity-identity=BLOCKER
 * </pre>
 *
 * <h2>v4.1.0 Migration</h2>
 * <p>Since v4.1.0, this plugin supports the new {@link DomainIndex}, {@link PortIndex},
 * and {@link ClassificationReport} APIs for improved classification insights and
 * quality metrics. When available, the plugin logs detailed classification
 * statistics and uses the classification report for actionable remediation.</p>
 *
 * @since 1.0.0
 * @since 4.0.0 - Added support for v4 ArchitecturalModel
 * @since 4.1.0 - Added support for new classification report and indices
 */
public class DddAuditPlugin implements AuditPlugin {

    public static final String PLUGIN_ID = "io.hexaglue.plugin.audit";

    private final ConstraintRegistry registry;

    /**
     * The v4 architectural model, captured in execute() before audit().
     * May be null if running in legacy mode.
     *
     * @since 4.0.0
     */
    private ArchitecturalModel archModel;

    /**
     * Encapsulates the complete audit execution result for report generation.
     *
     * <p>This internal record packages all data needed for report generation
     * without relying on mutable state.
     *
     * @param snapshot the SPI audit snapshot
     * @param domainResult the domain audit result (for metrics and violations)
     * @param config the audit configuration (for constraint IDs)
     * @param architectureQuery the architecture query from core (may be null)
     * @param model the architectural model for inventory building
     * @param executedConstraintIds the list of constraint IDs that were actually executed
     */
    private record AuditExecutionResult(
            AuditSnapshot snapshot,
            AuditResult domainResult,
            AuditConfiguration config,
            io.hexaglue.spi.audit.ArchitectureQuery architectureQuery,
            ArchitecturalModel model,
            List<String> executedConstraintIds) {}

    /**
     * Default constructor for ServiceLoader.
     */
    public DddAuditPlugin() {
        this.registry = ConstraintRegistry.withDefaults();
    }

    @Override
    public String id() {
        return PLUGIN_ID;
    }

    @Override
    public AuditSnapshot audit(AuditContext context) throws Exception {
        Instant startTime = Instant.now();

        // Extract context
        Codebase codebase = context.codebase();
        var diagnostics = context.diagnostics();
        var pluginConfig = context.config();

        // Load configuration
        AuditConfiguration config = AuditConfiguration.fromPluginConfig(pluginConfig);

        diagnostics.info("Executing DDD audit with %d constraints".formatted(registry.size()));

        // Build services
        ConstraintEngine constraintEngine = new ConstraintEngine(registry.allValidators());
        MetricAggregator metricAggregator = new MetricAggregator(buildCalculatorMap());
        AuditOrchestrator orchestrator = new AuditOrchestrator(constraintEngine, metricAggregator);

        // Use core's architecture query if available
        var architectureQuery = context.query().orElse(null);

        // Execute audit (all constraints, all metrics)
        AuditResult result = orchestrator.executeAudit(archModel, codebase, architectureQuery);

        // Log summary
        diagnostics.info("Audit complete: %d violations, %d metrics"
                .formatted(result.violations().size(), result.metrics().size()));

        if (result.outcome() == BuildOutcome.FAIL) {
            long blockerCount = result.blockerCount();
            long criticalCount = result.criticalCount();
            diagnostics.error(
                    "Audit FAILED: %d blocker, %d critical violations".formatted(blockerCount, criticalCount));
        }

        // Convert to SPI snapshot
        Duration duration = Duration.between(startTime, Instant.now());
        return convertToAuditSnapshot(result, codebase, duration, architectureQuery, config);
    }

    /**
     * Converts domain AuditResult to SPI AuditSnapshot.
     *
     * <p>This method enriches the snapshot with audit configuration and result data
     * needed for report generation.
     */
    private AuditSnapshot convertToAuditSnapshot(
            AuditResult result,
            Codebase codebase,
            Duration duration,
            io.hexaglue.spi.audit.ArchitectureQuery architectureQuery,
            AuditConfiguration config) {
        // Convert violations
        List<RuleViolation> ruleViolations =
                result.violations().stream().map(this::convertViolation).toList();

        // Compute quality metrics (simplified - just counts for now)
        QualityMetrics qualityMetrics = new QualityMetrics(0.0, 0.0, 0, 0.0);

        // Compute architecture metrics using the architecture query
        ArchitectureMetrics archMetrics = computeArchitectureMetrics(codebase, architectureQuery);

        // Build metadata - read version from MANIFEST.MF (set by maven-jar-plugin)
        String hexaglueVersion = getClass().getPackage().getImplementationVersion();
        if (hexaglueVersion == null) {
            hexaglueVersion = "dev"; // Fallback for IDE/test execution
        }
        AuditMetadata metadata = new AuditMetadata(Instant.now(), hexaglueVersion, duration);

        return new AuditSnapshot(
                codebase, DetectedArchitectureStyle.HEXAGONAL, ruleViolations, qualityMetrics, archMetrics, metadata);
    }

    /**
     * Converts domain Violation to SPI RuleViolation.
     */
    private RuleViolation convertViolation(Violation violation) {
        return new RuleViolation(
                violation.constraintId().value(),
                convertSeverity(violation.severity()),
                violation.message(),
                violation.affectedTypes(),
                violation.location(),
                formatEvidence(violation.evidence()));
    }

    /**
     * Formats evidence list into a human-readable string.
     *
     * <p>Each evidence item is formatted on a separate line with its description.
     * If there are involved types, they are listed after the description.
     */
    private String formatEvidence(java.util.List<io.hexaglue.plugin.audit.domain.model.Evidence> evidenceList) {
        if (evidenceList == null || evidenceList.isEmpty()) {
            return "";
        }
        return evidenceList.stream()
                .map(e -> {
                    StringBuilder sb = new StringBuilder(e.description());
                    if (!e.involvedTypes().isEmpty()) {
                        sb.append(" [")
                                .append(String.join(", ", e.involvedTypes()))
                                .append("]");
                    }
                    return sb.toString();
                })
                .collect(java.util.stream.Collectors.joining("; "));
    }

    /**
     * Maps domain severity to SPI severity.
     *
     * <p>Since SPI 5.0.0, the SPI supports the same severity levels as the domain,
     * so this is now a direct mapping by name.
     */
    private io.hexaglue.arch.model.audit.Severity convertSeverity(Severity domainSeverity) {
        return io.hexaglue.arch.model.audit.Severity.valueOf(domainSeverity.name());
    }

    /**
     * Computes architecture metrics using the architecture query.
     *
     * <p>When the core's ArchitectureQuery is available, this method leverages
     * its rich analysis capabilities for accurate metrics. Otherwise, it falls
     * back to basic counting.
     */
    private ArchitectureMetrics computeArchitectureMetrics(
            Codebase codebase, io.hexaglue.spi.audit.ArchitectureQuery architectureQuery) {
        int aggregateCount =
                codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT).size();

        // Use architecture query for cycle detection
        int circularDependencies = 0;
        if (architectureQuery != null) {
            circularDependencies = architectureQuery.findDependencyCycles().size();
        }

        // Calculate average coupling from package coupling metrics
        double avgCoupling = 0.0;
        if (architectureQuery != null) {
            List<CouplingMetrics> couplingMetrics = architectureQuery.analyzeAllPackageCoupling();
            if (!couplingMetrics.isEmpty()) {
                avgCoupling = couplingMetrics.stream()
                        .mapToDouble(CouplingMetrics::instability)
                        .average()
                        .orElse(0.0);
            }
        }

        // Cohesion metric (simplified - would need LCOM analysis for accurate value)
        double cohesion = 0.0;

        return new ArchitectureMetrics(aggregateCount, avgCoupling, cohesion, circularDependencies);
    }

    /**
     * Builds the map of metric calculators.
     *
     * <p>Provides quality metrics for aggregate size, coupling, domain coverage,
     * domain purity, port coverage, and boilerplate ratio. Each metric has defined
     * thresholds that trigger warnings when exceeded.
     *
     * @return map of metric name to calculator
     */
    private Map<String, MetricCalculator> buildCalculatorMap() {
        AggregateBoundaryMetricCalculator aggregateBoundaryMetric = new AggregateBoundaryMetricCalculator();
        AggregateMetricCalculator aggregateMetric = new AggregateMetricCalculator();
        BoilerplateMetricCalculator boilerplateMetric = new BoilerplateMetricCalculator();
        CodeComplexityMetricCalculator codeComplexityMetric = new CodeComplexityMetricCalculator();
        CouplingMetricCalculator couplingMetric = new CouplingMetricCalculator();
        DomainCoverageMetricCalculator domainCoverageMetric = new DomainCoverageMetricCalculator();
        DomainPurityMetricCalculator domainPurityMetric = new DomainPurityMetricCalculator();
        PortCoverageMetricCalculator portCoverageMetric = new PortCoverageMetricCalculator();

        Map<String, MetricCalculator> map = new HashMap<>();
        map.put(aggregateBoundaryMetric.metricName(), aggregateBoundaryMetric);
        map.put(aggregateMetric.metricName(), aggregateMetric);
        map.put(boilerplateMetric.metricName(), boilerplateMetric);
        map.put(codeComplexityMetric.metricName(), codeComplexityMetric);
        map.put(couplingMetric.metricName(), couplingMetric);
        map.put(domainCoverageMetric.metricName(), domainCoverageMetric);
        map.put(domainPurityMetric.metricName(), domainPurityMetric);
        map.put(portCoverageMetric.metricName(), portCoverageMetric);

        return map;
    }

    /**
     * Overrides execute to properly build AuditContext and generate reports.
     *
     * <p>This method adapts the generic PluginContext to AuditContext by building
     * a Codebase from the IrSnapshot. If the core's ArchitectureQuery is available,
     * it is passed through to leverage rich analysis capabilities.
     *
     * <p>The execution flow is stateless:
     * <ol>
     *   <li>Capture v4 ArchitecturalModel (if available)</li>
     *   <li>Build codebase from IR</li>
     *   <li>Load configuration</li>
     *   <li>Execute audit to produce snapshot</li>
     *   <li>Generate reports from snapshot and domain data</li>
     *   <li>Store snapshot in context outputs</li>
     * </ol>
     *
     * @since 4.0.0 - Added support for v4 ArchitecturalModel
     */
    @Override
    public void execute(PluginContext context) {
        try {
            // Capture v4 ArchitecturalModel
            this.archModel = context.model();

            // Get architecture query from core if available (needed for full graph dependencies)
            io.hexaglue.spi.audit.ArchitectureQuery coreQuery =
                    context.architectureQuery().orElse(null);

            Codebase codebase = buildCodebaseFromModel(archModel, coreQuery);

            // Load configuration
            AuditConfiguration config = AuditConfiguration.fromPluginConfig(context.config());

            // Execute audit (returns snapshot, result, and query for report generation)
            AuditExecutionResult executionResult =
                    executeDomainAudit(codebase, coreQuery, config, context.diagnostics(), archModel);

            // Generate reports using the execution result
            generateReports(executionResult, context);

            // Store snapshot in context outputs
            context.setOutput("audit-snapshot", executionResult.snapshot());

            // Report if failed
            if (!executionResult.snapshot().passed()) {
                context.diagnostics()
                        .warn("Audit found %d errors"
                                .formatted(executionResult.snapshot().errorCount()));
            }

            // Log v4 model summary and enrich audit with classification info
            logV4ModelSummary(archModel, context.diagnostics());
        } catch (Exception e) {
            context.diagnostics().error("Audit plugin execution failed: " + id(), e);
        }
    }

    /**
     * Logs a detailed summary of the v4.1 architectural model including unclassified types.
     *
     * <p>Uses the v4.1.0 {@link DomainIndex}, {@link PortIndex}, and {@link ClassificationReport}
     * APIs for element counts and classification quality metrics.</p>
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     * @since 4.0.0
     * @since 4.1.0 - Migrated to use new indices and classification report exclusively
     */
    private void logV4ModelSummary(ArchitecturalModel model, io.hexaglue.spi.plugin.DiagnosticReporter diagnostics) {
        // v4.1.0: Use new indices and classification report
        Optional<DomainIndex> domainIndexOpt = model.domainIndex();
        Optional<PortIndex> portIndexOpt = model.portIndex();
        Optional<ClassificationReport> reportOpt = model.classificationReport();

        if (domainIndexOpt.isPresent() && portIndexOpt.isPresent()) {
            DomainIndex domain = domainIndexOpt.get();
            PortIndex ports = portIndexOpt.get();

            long aggregateCount = domain.aggregateRoots().count();
            long entityCount = domain.entities().count();
            long valueObjectCount = domain.valueObjects().count();
            long identifierCount = domain.identifiers().count();
            long eventCount = domain.domainEvents().count();
            long drivingPortCount = ports.drivingPorts().count();
            long drivenPortCount = ports.drivenPorts().count();

            diagnostics.info(String.format(
                    "v4.1 Model: %d aggregates, %d entities, %d value objects, %d identifiers, %d events",
                    aggregateCount, entityCount, valueObjectCount, identifierCount, eventCount));
            diagnostics.info(String.format(
                    "Ports: %d driving ports, %d driven ports (%d repositories)",
                    drivingPortCount, drivenPortCount, ports.repositories().count()));

            // v4.1.0: Use ClassificationReport for quality metrics
            reportOpt.ifPresent(report -> {
                diagnostics.info(String.format(
                        "Classification rate: %.1f%% (total: %d, classified: %d)",
                        report.stats().classificationRate() * 100,
                        report.stats().totalTypes(),
                        report.stats().classifiedTypes()));

                if (report.hasIssues()) {
                    diagnostics.warn(String.format(
                            "%d types need attention:", report.actionRequired().size()));
                    report.actionRequired().stream().limit(5).forEach(unclassified -> {
                        String hint = unclassified.classification().remediationHints().stream()
                                .findFirst()
                                .map(h -> " - Hint: " + h.description())
                                .orElse("");
                        diagnostics.warn(String.format(
                                "  - %s [%s]: %s%s",
                                unclassified.simpleName(),
                                unclassified.category(),
                                unclassified.classification().explain(),
                                hint));
                    });
                    if (report.actionRequired().size() > 5) {
                        diagnostics.warn(String.format(
                                "  ... and %d more types need attention",
                                report.actionRequired().size() - 5));
                    }
                }
            });
        } else {
            // Fallback to registry if indices not available
            var registry = model.typeRegistry().orElseThrow();
            long aggregateCount = registry.all(AggregateRoot.class).count();
            long entityCount = registry.all(Entity.class).count();
            long valueObjectCount = registry.all(ValueObject.class).count();
            long drivingPortCount = registry.all(DrivingPort.class).count();
            long drivenPortCount = registry.all(DrivenPort.class).count();

            diagnostics.info(String.format(
                    "v5 Model: %d aggregates, %d entities, %d value objects, %d driving ports, %d driven ports",
                    aggregateCount, entityCount, valueObjectCount, drivingPortCount, drivenPortCount));
        }
    }

    /**
     * Executes the domain audit without mutable state.
     *
     * <p>This internal method orchestrates the audit execution in a stateless manner,
     * returning all needed data for report generation.
     *
     * @param codebase the codebase to audit
     * @param coreQuery the architecture query from core (may be null)
     * @param config the audit configuration
     * @param diagnostics the diagnostics handler
     * @param model the architectural model for inventory building
     * @return the audit execution result containing snapshot and domain data
     * @throws Exception if audit fails
     */
    private AuditExecutionResult executeDomainAudit(
            Codebase codebase,
            io.hexaglue.spi.audit.ArchitectureQuery coreQuery,
            AuditConfiguration config,
            io.hexaglue.spi.plugin.DiagnosticReporter diagnostics,
            ArchitecturalModel model)
            throws Exception {

        Instant startTime = Instant.now();

        // Build services
        ConstraintEngine constraintEngine = new ConstraintEngine(registry.allValidators());
        MetricAggregator metricAggregator = new MetricAggregator(buildCalculatorMap());
        AuditOrchestrator orchestrator = new AuditOrchestrator(constraintEngine, metricAggregator);

        // Get all registered constraint IDs
        List<String> executedConstraintIds = orchestrator.getExecutedConstraintIds();

        // Execute audit (all constraints, all metrics)
        AuditResult result = orchestrator.executeAudit(model, codebase, coreQuery);

        // Log summary
        diagnostics.info("Audit complete: %d violations, %d metrics"
                .formatted(result.violations().size(), result.metrics().size()));

        if (result.outcome() == BuildOutcome.FAIL) {
            long blockerCount = result.blockerCount();
            long criticalCount = result.criticalCount();
            diagnostics.error(
                    "Audit FAILED: %d blocker, %d critical violations".formatted(blockerCount, criticalCount));
        }

        // Convert to SPI snapshot
        Duration duration = Duration.between(startTime, Instant.now());
        AuditSnapshot snapshot = convertToAuditSnapshot(result, codebase, duration, coreQuery, config);

        // Return all data needed for report generation
        return new AuditExecutionResult(snapshot, result, config, coreQuery, model, executedConstraintIds);
    }

    /**
     * Generates audit reports in multiple formats.
     *
     * <p>This method uses the new v5.0.0 report generation pipeline:
     * <ol>
     *   <li>Build {@link ReportData} from audit results using {@link ReportDataBuilder}</li>
     *   <li>Generate Mermaid diagrams using {@link DiagramGenerator}</li>
     *   <li>Render to JSON, HTML, Markdown, and console using format-specific renderers</li>
     * </ol>
     *
     * <p>This ensures:
     * <ul>
     *   <li>JSON pivot as the single source of truth for structured data</li>
     *   <li>Consistent Mermaid diagrams shared between HTML and Markdown</li>
     *   <li>5-section report structure (Verdict, Architecture, Issues, Remediation, Appendix)</li>
     * </ul>
     *
     * @param executionResult the audit execution result
     * @param context the plugin context (for writer access and config)
     * @since 5.0.0
     */
    private void generateReports(AuditExecutionResult executionResult, PluginContext context) {
        try {
            // Unpack execution result
            AuditSnapshot snapshot = executionResult.snapshot();
            AuditResult domainResult = executionResult.domainResult();
            io.hexaglue.spi.audit.ArchitectureQuery architectureQuery = executionResult.architectureQuery();
            ArchitecturalModel model = executionResult.model();

            // Extract project info from model
            String projectName = inferProjectName(model);
            String projectVersion = model.project().version().orElse("1.0.0");

            // Get HexaGlue version from MANIFEST.MF
            String hexaglueVersion = getClass().getPackage().getImplementationVersion();
            if (hexaglueVersion == null) {
                hexaglueVersion = "dev";
            }
            String pluginVersion = hexaglueVersion;

            // Calculate audit duration from snapshot
            Duration duration = snapshot.metadata().analysisDuration();

            // 1. Build ReportData (structured data - JSON pivot)
            ReportDataBuilder reportDataBuilder = new ReportDataBuilder();
            ReportData reportData = reportDataBuilder.build(
                    snapshot,
                    domainResult,
                    architectureQuery,
                    model,
                    projectName,
                    projectVersion,
                    hexaglueVersion,
                    pluginVersion,
                    duration);

            // 2. Generate Mermaid diagrams (shared between HTML and Markdown)
            DiagramGenerator diagramGenerator = new DiagramGenerator();
            DiagramSet diagrams = diagramGenerator.generate(reportData, projectName);

            // 3. Generate console output (always)
            ConsoleRenderer consoleRenderer = new ConsoleRenderer();
            String consoleOutput = consoleRenderer.render(reportData);
            context.diagnostics().info(consoleOutput);

            // 4. Generate file reports based on configuration
            List<ReportFormat> enabledFormats = getEnabledFormats();
            if (!enabledFormats.isEmpty()) {
                Path baseDir = context.writer().getOutputDirectory();
                Path outputDir = baseDir.resolve("audit");
                Files.createDirectories(outputDir);

                for (ReportFormat format : enabledFormats) {
                    if (format.hasFileOutput()) {
                        String content = renderReport(format, reportData, diagrams);
                        Path outputFile = outputDir.resolve(format.defaultFilename());
                        Files.writeString(outputFile, content);
                        context.diagnostics().info("Generated %s report: %s".formatted(format.name(), outputFile));
                    }
                }
            }

            // 5. Generate documentation files if enabled
            if (isDocumentationEnabled(context.config())) {
                generateDocumentation(reportData, diagrams, context);
            }
        } catch (IOException e) {
            context.diagnostics().warn("Failed to generate audit reports: " + e.getMessage());
        }
    }

    /**
     * Renders the report in the specified format.
     *
     * @param format the output format
     * @param reportData the structured report data
     * @param diagrams the Mermaid diagrams
     * @return the rendered report content
     * @since 5.0.0
     */
    private String renderReport(ReportFormat format, ReportData reportData, DiagramSet diagrams) {
        ReportRenderer renderer =
                switch (format) {
                    case JSON -> new JsonReportRenderer();
                    case HTML -> new HtmlRenderer();
                    case MARKDOWN -> new MarkdownRenderer();
                    case CONSOLE -> new ConsoleRenderer();
                };
        return renderer.render(reportData, diagrams);
    }

    /**
     * Generates architecture documentation files.
     *
     * @param reportData the structured report data
     * @param diagrams the Mermaid diagrams
     * @param context the plugin context
     * @throws IOException if file writing fails
     * @since 5.0.0
     */
    private void generateDocumentation(ReportData reportData, DiagramSet diagrams, PluginContext context)
            throws IOException {
        Path baseDir = context.writer().getOutputDirectory();
        Path docsDir = baseDir.resolve("audit").resolve("docs");
        Files.createDirectories(docsDir);

        // Write individual diagram files for easy embedding
        if (diagrams.scoreRadar() != null) {
            Files.writeString(docsDir.resolve("score-radar.mmd"), diagrams.scoreRadar());
        }
        if (diagrams.c4Context() != null) {
            Files.writeString(docsDir.resolve("c4-context.mmd"), diagrams.c4Context());
        }
        if (diagrams.c4Component() != null) {
            Files.writeString(docsDir.resolve("c4-component.mmd"), diagrams.c4Component());
        }
        if (diagrams.domainModel() != null) {
            Files.writeString(docsDir.resolve("domain-model.mmd"), diagrams.domainModel());
        }
        if (diagrams.violationsPie() != null) {
            Files.writeString(docsDir.resolve("violations-pie.mmd"), diagrams.violationsPie());
        }
        if (diagrams.packageZones() != null) {
            Files.writeString(docsDir.resolve("package-zones.mmd"), diagrams.packageZones());
        }

        context.diagnostics().info("Generated architecture documentation in: " + docsDir);
    }

    /**
     * Checks if documentation generation is enabled.
     *
     * <p>Documentation generation is enabled by setting "generateDocs" to "true"
     * in the plugin configuration.
     *
     * @param config the plugin configuration
     * @return true if documentation generation is enabled
     */
    private boolean isDocumentationEnabled(PluginConfig config) {
        return config.getBoolean("generateDocs", false);
    }

    /**
     * Gets project name from architectural model.
     *
     * <p>Uses the project name from the model's project context which is populated
     * by the Maven plugin from the project's pom.xml. Falls back to "Unknown Project"
     * if not explicitly set.
     *
     * @param model the architectural model
     * @return the project name
     */
    private String inferProjectName(ArchitecturalModel model) {
        // Use project name from model's project context (set by Maven plugin)
        String name = model.project().name();
        return name != null ? name : "Unknown Project";
    }

    /**
     * Returns all report formats (JSON, HTML, Markdown).
     *
     * <p>Console output is always generated separately.
     *
     * @return list of all report formats
     * @since 5.1.0
     */
    private List<ReportFormat> getEnabledFormats() {
        return List.of(ReportFormat.JSON, ReportFormat.HTML, ReportFormat.MARKDOWN);
    }

    /**
     * Checks if a type is primitive or a standard library type.
     *
     * <p>These types are excluded from dependency analysis as they don't
     * represent architectural dependencies.
     *
     * @param type the fully-qualified type name
     * @return true if the type should be excluded from dependency analysis
     */
    private boolean isPrimitive(String type) {
        return Set.of(
                                "int",
                                "long",
                                "double",
                                "float",
                                "boolean",
                                "byte",
                                "short",
                                "char",
                                "String",
                                "Integer",
                                "Long",
                                "Double",
                                "Float",
                                "Boolean",
                                "BigDecimal",
                                "BigInteger",
                                "LocalDate",
                                "LocalDateTime",
                                "Instant",
                                "UUID")
                        .contains(type)
                || type.startsWith("java.");
    }

    /**
     * Infers the base package from code units.
     */
    private String inferBasePackage(List<CodeUnit> units) {
        if (units.isEmpty()) {
            return "";
        }

        // Find common prefix of all packages
        String commonPrefix = units.get(0).packageName();

        for (CodeUnit unit : units) {
            while (!unit.packageName().startsWith(commonPrefix)) {
                int lastDot = commonPrefix.lastIndexOf('.');
                if (lastDot < 0) {
                    return "";
                }
                commonPrefix = commonPrefix.substring(0, lastDot);
            }
        }

        return commonPrefix;
    }

    // ===== v4 Model Support =====

    /**
     * Builds a Codebase from v4.1 ArchitecturalModel.
     *
     * <p>This converts the v4 model elements (DomainEntity, ValueObject, DrivenPort, etc.)
     * into code units that can be audited. Uses {@link ArchitecturalModel#registry()} instead
     * of deprecated convenience methods.</p>
     *
     * @param model the v4 architectural model
     * @return the codebase for auditing
     * @since 4.0.0
     * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
     */
    private Codebase buildCodebaseFromModel(ArchitecturalModel model, io.hexaglue.spi.audit.ArchitectureQuery query) {
        List<CodeUnit> units = new ArrayList<>();
        Map<String, Set<String>> dependencies = new HashMap<>();

        var registry = model.typeRegistry().orElseThrow();

        // Convert aggregate roots to code units
        registry.all(AggregateRoot.class).forEach(aggregate -> {
            CodeUnit unit = toCodeUnitV5(aggregate);
            units.add(unit);
            dependencies.put(aggregate.id().qualifiedName(), extractDependenciesV5(aggregate));
        });

        // Convert entities to code units
        registry.all(Entity.class).forEach(entity -> {
            CodeUnit unit = toCodeUnitV5(entity);
            units.add(unit);
            dependencies.put(entity.id().qualifiedName(), extractDependenciesV5(entity));
        });

        // Convert value objects to code units
        registry.all(ValueObject.class).forEach(vo -> {
            CodeUnit unit = toCodeUnitV5(vo);
            units.add(unit);
            dependencies.put(vo.id().qualifiedName(), extractDependenciesV5(vo));
        });

        // Convert identifiers to code units
        registry.all(Identifier.class).forEach(id -> {
            CodeUnit unit = toCodeUnitV5(id);
            units.add(unit);
            dependencies.put(id.id().qualifiedName(), extractDependenciesV5(id));
        });

        // Convert domain events to code units
        registry.all(DomainEvent.class).forEach(event -> {
            CodeUnit unit = toCodeUnitV5(event);
            units.add(unit);
            dependencies.put(event.id().qualifiedName(), extractDependenciesV5(event));
        });

        // Convert domain services to code units
        registry.all(DomainService.class).forEach(service -> {
            CodeUnit unit = toCodeUnitV5(service);
            units.add(unit);
            dependencies.put(service.id().qualifiedName(), extractDependenciesV5(service));
        });

        // Convert application services to code units
        registry.all(ApplicationService.class).forEach(appService -> {
            CodeUnit unit = toCodeUnitV5(appService);
            units.add(unit);
            dependencies.put(appService.id().qualifiedName(), extractDependenciesV5(appService));
        });

        // Convert driving ports to code units
        registry.all(DrivingPort.class).forEach(port -> {
            CodeUnit unit = toCodeUnitV5(port);
            units.add(unit);
            dependencies.put(port.id().qualifiedName(), extractDependenciesV5(port));
        });

        // Convert driven ports to code units
        registry.all(DrivenPort.class).forEach(port -> {
            CodeUnit unit = toCodeUnitV5(port);
            units.add(unit);
            dependencies.put(port.id().qualifiedName(), extractDependenciesV5(port));
        });

        // Enrich with dependencies from excluded types (not in registry but in full graph)
        if (query != null) {
            Map<String, Set<String>> fullGraphDeps = query.allTypeDependencies();
            for (Map.Entry<String, Set<String>> entry : fullGraphDeps.entrySet()) {
                if (!dependencies.containsKey(entry.getKey())) {
                    dependencies.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return new Codebase("audit-target", inferBasePackage(units), units, dependencies);
    }

    /**
     * Converts a v5 AggregateRoot to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(AggregateRoot aggregate) {
        var structure = aggregate.structure();

        // Build field declarations
        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(),
                        field.type().qualifiedName(),
                        Set.of(),
                        field.roles().contains(io.hexaglue.arch.model.FieldRole.IDENTITY) ? Set.of("Id") : Set.of()))
                .toList();

        // Build method declarations
        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);

        // Determine code unit kind
        CodeUnitKind unitKind = structure.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        return new CodeUnit(
                aggregate.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                RoleClassification.AGGREGATE_ROOT,
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 Entity to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(Entity entity) {
        var structure = entity.structure();

        // Build field declarations
        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(),
                        field.type().qualifiedName(),
                        Set.of(),
                        field.roles().contains(io.hexaglue.arch.model.FieldRole.IDENTITY) ? Set.of("Id") : Set.of()))
                .toList();

        // Build method declarations
        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);

        // Determine code unit kind
        CodeUnitKind unitKind = structure.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        return new CodeUnit(
                entity.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                RoleClassification.ENTITY,
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 ValueObject to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(ValueObject vo) {
        var structure = vo.structure();

        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(), field.type().qualifiedName(), Set.of(), Set.of()))
                .toList();

        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);
        CodeUnitKind unitKind = structure.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        return new CodeUnit(
                vo.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT,
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 Identifier to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(Identifier identifier) {
        var structure = identifier.structure();

        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(), field.type().qualifiedName(), Set.of(), Set.of()))
                .toList();

        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);
        CodeUnitKind unitKind = structure.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        return new CodeUnit(
                identifier.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT, // Identifiers are value objects semantically
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 DomainEvent to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(DomainEvent event) {
        var structure = event.structure();

        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(), field.type().qualifiedName(), Set.of(), Set.of()))
                .toList();

        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);
        CodeUnitKind unitKind = structure.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        return new CodeUnit(
                event.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT, // Events are immutable facts
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 DomainService to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(DomainService service) {
        var structure = service.structure();

        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(), field.type().qualifiedName(), Set.of(), Set.of()))
                .toList();

        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);

        return new CodeUnit(
                service.id().qualifiedName(),
                CodeUnitKind.CLASS,
                LayerClassification.DOMAIN,
                RoleClassification.SERVICE,
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 ApplicationService to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(ApplicationService appService) {
        var structure = appService.structure();

        List<io.hexaglue.arch.model.audit.FieldDeclaration> fieldDecls = structure.fields().stream()
                .map(field -> new io.hexaglue.arch.model.audit.FieldDeclaration(
                        field.name(), field.type().qualifiedName(), Set.of(), Set.of()))
                .toList();

        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls =
                structure.methods().stream().map(this::toMethodDeclarationV5).toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);

        return new CodeUnit(
                appService.id().qualifiedName(),
                CodeUnitKind.CLASS,
                LayerClassification.APPLICATION,
                RoleClassification.SERVICE,
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 DrivingPort to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(DrivingPort port) {
        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls = port.structure().methods().stream()
                .map(this::toMethodDeclarationV5)
                .toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), 0, 100.0);

        return new CodeUnit(
                port.id().qualifiedName(),
                CodeUnitKind.INTERFACE,
                LayerClassification.APPLICATION,
                RoleClassification.PORT,
                methodDecls,
                List.of(),
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 DrivenPort to a CodeUnit.
     */
    private CodeUnit toCodeUnitV5(DrivenPort port) {
        List<io.hexaglue.arch.model.audit.MethodDeclaration> methodDecls = port.structure().methods().stream()
                .map(this::toMethodDeclarationV5)
                .toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), 0, 100.0);

        // Determine role based on port type
        RoleClassification role = port.portType() == io.hexaglue.arch.model.DrivenPortType.REPOSITORY
                ? RoleClassification.REPOSITORY
                : RoleClassification.PORT;

        return new CodeUnit(
                port.id().qualifiedName(),
                CodeUnitKind.INTERFACE,
                LayerClassification.APPLICATION,
                role,
                methodDecls,
                List.of(),
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v5 Method to a MethodDeclaration.
     */
    private io.hexaglue.arch.model.audit.MethodDeclaration toMethodDeclarationV5(io.hexaglue.arch.model.Method method) {
        String returnType = method.returnType() != null ? method.returnType().qualifiedName() : "void";
        List<String> paramTypes =
                method.parameters().stream().map(p -> p.type().qualifiedName()).toList();

        // Extract modifiers
        Set<String> modifiers = method.modifiers().stream()
                .map(Enum::name)
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());

        return new io.hexaglue.arch.model.audit.MethodDeclaration(
                method.name(), returnType, paramTypes, modifiers, Set.of(), 1 // complexity
                );
    }

    /**
     * Extracts dependencies from a v5 AggregateRoot.
     */
    private Set<String> extractDependenciesV5(AggregateRoot aggregate) {
        Set<String> deps = new HashSet<>();
        var structure = aggregate.structure();
        // From fields
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        // From annotations
        structure.annotations().forEach(ann -> deps.add(ann.qualifiedName()));
        return deps;
    }

    /**
     * Extracts dependencies from a v5 Entity.
     */
    private Set<String> extractDependenciesV5(Entity entity) {
        Set<String> deps = new HashSet<>();
        var structure = entity.structure();
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        structure.annotations().forEach(ann -> deps.add(ann.qualifiedName()));
        return deps;
    }

    /**
     * Extracts dependencies from a v5 ValueObject.
     */
    private Set<String> extractDependenciesV5(ValueObject vo) {
        Set<String> deps = new HashSet<>();
        var structure = vo.structure();
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        return deps;
    }

    /**
     * Extracts dependencies from a v5 Identifier.
     */
    private Set<String> extractDependenciesV5(Identifier identifier) {
        Set<String> deps = new HashSet<>();
        var structure = identifier.structure();
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        return deps;
    }

    /**
     * Extracts dependencies from a v5 DomainEvent.
     */
    private Set<String> extractDependenciesV5(DomainEvent event) {
        Set<String> deps = new HashSet<>();
        var structure = event.structure();
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        return deps;
    }

    /**
     * Extracts dependencies from a v5 DomainService.
     */
    private Set<String> extractDependenciesV5(DomainService service) {
        Set<String> deps = new HashSet<>();
        var structure = service.structure();
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        return deps;
    }

    /**
     * Extracts dependencies from a v5 ApplicationService.
     */
    private Set<String> extractDependenciesV5(ApplicationService appService) {
        Set<String> deps = new HashSet<>();
        var structure = appService.structure();
        structure.fields().forEach(field -> {
            if (!isPrimitive(field.type().qualifiedName())) {
                deps.add(field.type().qualifiedName());
            }
        });
        structure.annotations().forEach(ann -> deps.add(ann.qualifiedName()));
        return deps;
    }

    /**
     * Extracts dependencies from a v5 DrivingPort.
     */
    private Set<String> extractDependenciesV5(DrivingPort port) {
        Set<String> deps = new HashSet<>();
        // Ports are interfaces, extract from method signatures
        port.structure().methods().forEach(method -> {
            if (method.returnType() != null && !isPrimitive(method.returnType().qualifiedName())) {
                deps.add(method.returnType().qualifiedName());
            }
            method.parameters().forEach(param -> {
                if (!isPrimitive(param.type().qualifiedName())) {
                    deps.add(param.type().qualifiedName());
                }
            });
        });
        return deps;
    }

    /**
     * Extracts dependencies from a v5 DrivenPort.
     */
    private Set<String> extractDependenciesV5(DrivenPort port) {
        Set<String> deps = new HashSet<>();
        port.structure().methods().forEach(method -> {
            if (method.returnType() != null && !isPrimitive(method.returnType().qualifiedName())) {
                deps.add(method.returnType().qualifiedName());
            }
            method.parameters().forEach(param -> {
                if (!isPrimitive(param.type().qualifiedName())) {
                    deps.add(param.type().qualifiedName());
                }
            });
        });
        return deps;
    }
}
