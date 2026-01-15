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
import io.hexaglue.plugin.audit.adapter.metric.AggregateBoundaryMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.AggregateMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.BoilerplateMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.CouplingMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.DomainCoverageMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.DomainPurityMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.PortCoverageMetricCalculator;
import io.hexaglue.plugin.audit.adapter.report.ConsoleReportGenerator;
import io.hexaglue.plugin.audit.adapter.report.DocumentationGenerator;
import io.hexaglue.plugin.audit.adapter.report.ReportFormat;
import io.hexaglue.plugin.audit.adapter.report.ReportGenerator;
import io.hexaglue.plugin.audit.adapter.report.ReportGeneratorFactory;
import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.config.AuditConfiguration;
import io.hexaglue.plugin.audit.config.ConstraintRegistry;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.BuildOutcome;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.port.driving.MetricCalculator;
import io.hexaglue.plugin.audit.domain.service.AuditOrchestrator;
import io.hexaglue.plugin.audit.domain.service.ConstraintEngine;
import io.hexaglue.plugin.audit.domain.service.MetricAggregator;
import io.hexaglue.spi.arch.PluginContexts;
import io.hexaglue.spi.audit.ArchitectureMetrics;
import io.hexaglue.spi.audit.AuditContext;
import io.hexaglue.spi.audit.AuditMetadata;
import io.hexaglue.spi.audit.AuditPlugin;
import io.hexaglue.spi.audit.AuditSnapshot;
import io.hexaglue.spi.audit.CodeMetrics;
import io.hexaglue.spi.audit.CodeUnit;
import io.hexaglue.spi.audit.CodeUnitKind;
import io.hexaglue.spi.audit.Codebase;
import io.hexaglue.spi.audit.CouplingMetrics;
import io.hexaglue.spi.audit.DetectedArchitectureStyle;
import io.hexaglue.spi.audit.DocumentationInfo;
import io.hexaglue.spi.audit.LayerClassification;
import io.hexaglue.spi.audit.QualityMetrics;
import io.hexaglue.spi.audit.RoleClassification;
import io.hexaglue.spi.audit.RuleViolation;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
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
 * audit.allowCriticalViolations=false
 * audit.enabledConstraints=ddd:entity-identity,ddd:aggregate-repository
 * audit.severity.ddd:entity-identity=BLOCKER
 * </pre>
 *
 * @since 1.0.0
 * @since 4.0.0 - Added support for v4 ArchitecturalModel
 */
public class DddAuditPlugin implements AuditPlugin {

    public static final String PLUGIN_ID = "io.hexaglue.plugin.audit.ddd";

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
     * @param ir the IR snapshot for inventory building
     */
    private record AuditExecutionResult(
            AuditSnapshot snapshot,
            AuditResult domainResult,
            AuditConfiguration config,
            io.hexaglue.spi.audit.ArchitectureQuery architectureQuery,
            IrSnapshot ir) {}

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

        // Execute audit with architecture query
        AuditResult result = orchestrator.executeAudit(
                codebase,
                architectureQuery,
                config.enabledConstraints(),
                config.enabledMetrics(),
                config.allowCriticalViolations());

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

        // Build metadata
        AuditMetadata metadata = new AuditMetadata(Instant.now(), "1.0.0", duration);

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
                violation.location());
    }

    /**
     * Maps plugin's fine-grained severity levels to SPI's coarse-grained levels.
     *
     * <p>Mapping:
     * <ul>
     *   <li>{@code BLOCKER, CRITICAL} → {@code Severity.ERROR}</li>
     *   <li>{@code MAJOR} → {@code Severity.WARNING}</li>
     *   <li>{@code MINOR, INFO} → {@code Severity.INFO}</li>
     * </ul>
     */
    private io.hexaglue.spi.audit.Severity convertSeverity(Severity domainSeverity) {
        return switch (domainSeverity) {
            case BLOCKER, CRITICAL -> io.hexaglue.spi.audit.Severity.ERROR;
            case MAJOR -> io.hexaglue.spi.audit.Severity.WARNING;
            case MINOR, INFO -> io.hexaglue.spi.audit.Severity.INFO;
        };
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
        CouplingMetricCalculator couplingMetric = new CouplingMetricCalculator();
        DomainCoverageMetricCalculator domainCoverageMetric = new DomainCoverageMetricCalculator();
        DomainPurityMetricCalculator domainPurityMetric = new DomainPurityMetricCalculator();
        PortCoverageMetricCalculator portCoverageMetric = new PortCoverageMetricCalculator();

        Map<String, MetricCalculator> map = new HashMap<>();
        map.put(aggregateBoundaryMetric.metricName(), aggregateBoundaryMetric);
        map.put(aggregateMetric.metricName(), aggregateMetric);
        map.put(boilerplateMetric.metricName(), boilerplateMetric);
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
            // Capture v4 ArchitecturalModel if available
            this.archModel = PluginContexts.getModel(context).orElse(null);

            // Build codebase from best available source (v4 model preferred)
            Codebase codebase =
                    archModel != null ? buildCodebaseFromModel(archModel) : buildCodebaseFromIr(context.ir());

            // Get architecture query from core if available
            io.hexaglue.spi.audit.ArchitectureQuery coreQuery =
                    context.architectureQuery().orElse(null);

            // Load configuration
            AuditConfiguration config = AuditConfiguration.fromPluginConfig(context.config());

            // Execute audit (returns snapshot, result, and query for report generation)
            AuditExecutionResult executionResult =
                    executeDomainAudit(codebase, coreQuery, config, context.diagnostics(), context.ir());

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
            if (archModel != null) {
                logV4ModelSummary(archModel, context.diagnostics());
            } else {
                context.diagnostics().info("Using legacy IrSnapshot for audit (v4 ArchitecturalModel not available)");
            }
        } catch (Exception e) {
            context.diagnostics().error("Audit plugin execution failed: " + id(), e);
        }
    }

    /**
     * Logs a detailed summary of the v4 architectural model including unclassified types.
     *
     * <p>When v4 model is available, this provides:
     * <ul>
     *   <li>Element counts by type</li>
     *   <li>Unclassified type warnings with remediation hints</li>
     *   <li>Classification confidence information</li>
     * </ul>
     *
     * @param model the architectural model
     * @param diagnostics the diagnostic reporter
     * @since 4.0.0
     */
    private void logV4ModelSummary(ArchitecturalModel model, io.hexaglue.spi.plugin.DiagnosticReporter diagnostics) {
        // Basic counts
        long aggregateCount = model.aggregates().count();
        long entityCount =
                model.domainEntities().filter(e -> !e.isAggregateRoot()).count();
        long valueObjectCount = model.valueObjects().count();
        long drivingPortCount = model.drivingPorts().count();
        long drivenPortCount = model.drivenPorts().count();
        long unclassifiedCount = model.unclassifiedCount();

        diagnostics.info(String.format(
                "v4 Model: %d aggregates, %d entities, %d value objects, %d driving ports, %d driven ports",
                aggregateCount, entityCount, valueObjectCount, drivingPortCount, drivenPortCount));

        // Warn about unclassified types with remediation hints
        if (unclassifiedCount > 0) {
            diagnostics.warn(String.format("Found %d unclassified types:", unclassifiedCount));
            model.unclassifiedTypes().limit(5).forEach(unclassified -> {
                String hint = unclassified.classificationTrace().remediationHints().stream()
                        .findFirst()
                        .map(h -> " - Hint: " + h.description())
                        .orElse("");
                diagnostics.warn(String.format(
                        "  - %s: %s%s",
                        unclassified.id().simpleName(),
                        unclassified.classificationTrace().explain(),
                        hint));
            });
            if (unclassifiedCount > 5) {
                diagnostics.warn(String.format("  ... and %d more unclassified types", unclassifiedCount - 5));
            }
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
     * @param ir the IR snapshot for inventory building
     * @return the audit execution result containing snapshot and domain data
     * @throws Exception if audit fails
     */
    private AuditExecutionResult executeDomainAudit(
            Codebase codebase,
            io.hexaglue.spi.audit.ArchitectureQuery coreQuery,
            AuditConfiguration config,
            io.hexaglue.spi.plugin.DiagnosticReporter diagnostics,
            IrSnapshot ir)
            throws Exception {

        Instant startTime = Instant.now();

        // Build services
        ConstraintEngine constraintEngine = new ConstraintEngine(registry.allValidators());
        MetricAggregator metricAggregator = new MetricAggregator(buildCalculatorMap());
        AuditOrchestrator orchestrator = new AuditOrchestrator(constraintEngine, metricAggregator);

        // Execute audit with architecture query
        AuditResult result = orchestrator.executeAudit(
                codebase,
                coreQuery,
                config.enabledConstraints(),
                config.enabledMetrics(),
                config.allowCriticalViolations());

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
        return new AuditExecutionResult(snapshot, result, config, coreQuery, ir);
    }

    /**
     * Generates audit reports in multiple formats.
     *
     * <p>This method includes architecture analysis data (cycles, layer violations,
     * coupling metrics) in the reports when the architecture query is available from core.
     *
     * <p>All needed data is passed as parameters - no mutable state is used.
     *
     * @param executionResult the audit execution result
     * @param context the plugin context (for writer access and config)
     */
    private void generateReports(AuditExecutionResult executionResult, PluginContext context) {
        try {
            // Unpack execution result
            AuditSnapshot snapshot = executionResult.snapshot();
            AuditResult domainResult = executionResult.domainResult();
            AuditConfiguration config = executionResult.config();
            io.hexaglue.spi.audit.ArchitectureQuery architectureQuery = executionResult.architectureQuery();
            IrSnapshot ir = executionResult.ir();

            // Build unified report model
            List<String> constraintIds = new ArrayList<>(config.enabledConstraints());

            // Extract project name from IR or use codebase name
            String projectName = inferProjectName(ir);

            // Build complete report with all enriched data
            AuditReport report = AuditReport.fromComplete(
                    snapshot,
                    projectName,
                    domainResult.metrics(),
                    constraintIds,
                    architectureQuery,
                    ir,
                    domainResult.violations());

            // Always generate console output
            ConsoleReportGenerator consoleGenerator = new ConsoleReportGenerator();
            String consoleOutput = consoleGenerator.generate(report);
            context.diagnostics().info(consoleOutput);

            // Generate file reports based on configuration
            List<ReportFormat> enabledFormats = getEnabledFormats(context.config());
            if (!enabledFormats.isEmpty()) {
                // Use writer's output directory
                Path baseDir = context.writer().getOutputDirectory();
                Path outputDir = baseDir.resolve("audit");
                Files.createDirectories(outputDir);

                for (ReportFormat format : enabledFormats) {
                    if (format.hasFileOutput()) {
                        ReportGenerator generator = ReportGeneratorFactory.create(format);
                        generator.writeToFile(report, outputDir);
                        context.diagnostics()
                                .info("Generated %s report: %s"
                                        .formatted(format.name(), outputDir.resolve(format.defaultFilename())));
                    }
                }
            }

            // Generate documentation files if enabled
            if (isDocumentationEnabled(context.config())) {
                Path baseDir = context.writer().getOutputDirectory();
                Path docsDir = baseDir.resolve("audit").resolve("docs");
                DocumentationGenerator docGenerator = new DocumentationGenerator();
                docGenerator.generateAll(report, docsDir);
                context.diagnostics().info("Generated architecture documentation in: " + docsDir);
            }
        } catch (IOException e) {
            context.diagnostics().warn("Failed to generate audit reports: " + e.getMessage());
        }
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
     * Gets project name from IR metadata.
     *
     * <p>Uses the project name from IrMetadata which is populated by the Maven plugin
     * from the project's pom.xml. Falls back to the inferred name from metadata
     * if not explicitly set.
     *
     * @param ir the IR snapshot
     * @return the project name
     */
    private String inferProjectName(IrSnapshot ir) {
        // Use project name from IR metadata (set by Maven plugin)
        return ir.metadata().projectName();
    }

    /**
     * Extracts enabled report formats from plugin configuration.
     *
     * @param config the plugin configuration
     * @return list of enabled report formats
     */
    private List<ReportFormat> getEnabledFormats(io.hexaglue.spi.plugin.PluginConfig config) {
        String formatsConfig = config.getString("audit.reportFormats").orElse("json,html,markdown");
        if (formatsConfig.isBlank()) {
            return List.of();
        }

        List<ReportFormat> formats = new ArrayList<>();
        for (String formatName : formatsConfig.split(",")) {
            String trimmed = formatName.trim().toUpperCase();
            try {
                ReportFormat format = ReportFormat.valueOf(trimmed);
                if (format != ReportFormat.CONSOLE) { // Console is always output separately
                    formats.add(format);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid format names
            }
        }
        return formats;
    }

    /**
     * Builds a Codebase from IrSnapshot.
     *
     * <p>This converts the domain model and port model into code units that can
     * be audited. The conversion handles:
     * <ul>
     *   <li>Layer classification based on DomainKind</li>
     *   <li>Role mapping for validators</li>
     *   <li>Field annotations for identity detection</li>
     *   <li>Synthetic method generation for setter detection</li>
     * </ul>
     *
     * @deprecated Use {@link #buildCodebaseFromModel(ArchitecturalModel)} for v4 model support
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    private Codebase buildCodebaseFromIr(IrSnapshot ir) {
        List<CodeUnit> units = new ArrayList<>();

        // Convert domain types to code units
        for (DomainType type : ir.domain().types()) {
            // Build field declarations with annotations for identity detection
            List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = type.properties().stream()
                    .map(prop -> new io.hexaglue.spi.audit.FieldDeclaration(
                            prop.name(),
                            prop.type().qualifiedName(),
                            Set.of(), // modifiers
                            prop.isIdentity() ? Set.of("Id") : Set.of()))
                    .toList();

            // Synthesize method declarations from properties for setter detection
            List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls =
                    synthesizeMethodsFromProperties(type.properties(), type.construct());

            CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);

            // Determine layer based on DomainKind
            LayerClassification layer = layerFromDomainKind(type.kind());

            // Determine code unit kind
            CodeUnitKind unitKind = type.construct() == io.hexaglue.spi.ir.JavaConstruct.RECORD
                    ? CodeUnitKind.RECORD
                    : CodeUnitKind.CLASS;

            units.add(new CodeUnit(
                    type.qualifiedName(),
                    unitKind,
                    layer,
                    roleFromDomainKind(type.kind()),
                    methodDecls,
                    fieldDecls,
                    codeMetrics,
                    new DocumentationInfo(false, 0, List.of())));
        }

        // Convert ports to code units
        for (Port port : ir.ports().ports()) {
            List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = port.methods().stream()
                    .map(method -> new io.hexaglue.spi.audit.MethodDeclaration(
                            method.name(),
                            method.returnType().qualifiedName(),
                            method.parameters().stream()
                                    .map(p -> p.type().qualifiedName())
                                    .toList(),
                            Set.of(), // modifiers
                            Set.of(), // annotations
                            0 // complexity
                            ))
                    .toList();

            CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), 0, 100.0);

            units.add(new CodeUnit(
                    port.qualifiedName(),
                    CodeUnitKind.INTERFACE,
                    LayerClassification.APPLICATION,
                    port.isRepository() ? RoleClassification.REPOSITORY : RoleClassification.PORT,
                    methodDecls,
                    List.of(), // fields
                    codeMetrics,
                    new DocumentationInfo(false, 0, List.of())));
        }

        // Build dependency map from IR (includes annotations for purity checking)
        Map<String, java.util.Set<String>> dependencies = extractDependencies(ir);

        return new Codebase("audit-target", inferBasePackage(units), units, dependencies);
    }

    /**
     * Synthesizes method declarations from domain properties.
     *
     * <p>For non-record types, generates getter method declarations based on property names.
     * This provides method information for validators that need it.
     *
     * <p>Note: We do NOT synthesize setter methods because:
     * <ul>
     *   <li>We cannot reliably detect if setters actually exist in the source</li>
     *   <li>Synthesizing setters for all classes would create false positives</li>
     *   <li>The ValueObjectImmutabilityValidator requires actual method info from the IR</li>
     * </ul>
     *
     * <p>Records are assumed to have no setters (immutable by design) and only have
     * accessor methods matching property names.
     *
     * @param properties the domain properties
     * @param construct the Java construct (RECORD, CLASS, etc.)
     * @return synthesized method declarations (getters only)
     */
    private List<io.hexaglue.spi.audit.MethodDeclaration> synthesizeMethodsFromProperties(
            List<io.hexaglue.spi.ir.DomainProperty> properties, io.hexaglue.spi.ir.JavaConstruct construct) {

        List<io.hexaglue.spi.audit.MethodDeclaration> methods = new ArrayList<>();

        for (io.hexaglue.spi.ir.DomainProperty prop : properties) {
            String propName = prop.name();

            if (construct == io.hexaglue.spi.ir.JavaConstruct.RECORD) {
                // Records have accessor methods with the same name as the property
                methods.add(new io.hexaglue.spi.audit.MethodDeclaration(
                        propName, prop.type().qualifiedName(), List.of(), Set.of("public"), Set.of(), 1));
            } else {
                // Regular classes typically have getXxx() methods
                String capitalizedName = propName.substring(0, 1).toUpperCase() + propName.substring(1);
                methods.add(new io.hexaglue.spi.audit.MethodDeclaration(
                        "get" + capitalizedName,
                        prop.type().qualifiedName(),
                        List.of(),
                        Set.of("public"),
                        Set.of(),
                        1));
            }
        }

        return methods;
    }

    /**
     * Determines the architectural layer from DomainKind.
     *
     * <p>Mapping:
     * <ul>
     *   <li>AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, IDENTIFIER, DOMAIN_EVENT, DOMAIN_SERVICE → DOMAIN</li>
     *   <li>APPLICATION_SERVICE, INBOUND_ONLY, OUTBOUND_ONLY, SAGA → APPLICATION</li>
     *   <li>UNCLASSIFIED → UNKNOWN (cannot determine layer)</li>
     * </ul>
     *
     * @param kind the domain kind
     * @return the corresponding layer classification
     */
    private LayerClassification layerFromDomainKind(DomainKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, IDENTIFIER, DOMAIN_EVENT, DOMAIN_SERVICE ->
                LayerClassification.DOMAIN;
            case APPLICATION_SERVICE, INBOUND_ONLY, OUTBOUND_ONLY, SAGA -> LayerClassification.APPLICATION;
            case UNCLASSIFIED -> LayerClassification.UNKNOWN;
        };
    }

    /**
     * Converts DomainKind to RoleClassification.
     *
     * <p>Mapping:
     * <ul>
     *   <li>AGGREGATE_ROOT → AGGREGATE_ROOT</li>
     *   <li>ENTITY → ENTITY</li>
     *   <li>VALUE_OBJECT, IDENTIFIER → VALUE_OBJECT</li>
     *   <li>DOMAIN_EVENT → VALUE_OBJECT (events are immutable facts, semantically value objects)</li>
     *   <li>DOMAIN_SERVICE, APPLICATION_SERVICE → SERVICE</li>
     *   <li>UNCLASSIFIED, INBOUND_ONLY, OUTBOUND_ONLY, SAGA → UNKNOWN</li>
     * </ul>
     */
    private RoleClassification roleFromDomainKind(DomainKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT -> RoleClassification.AGGREGATE_ROOT;
            case ENTITY -> RoleClassification.ENTITY;
            case VALUE_OBJECT, IDENTIFIER -> RoleClassification.VALUE_OBJECT;
            case DOMAIN_EVENT -> RoleClassification.VALUE_OBJECT; // Events are immutable facts
            case DOMAIN_SERVICE, APPLICATION_SERVICE -> RoleClassification.SERVICE;
            case UNCLASSIFIED, INBOUND_ONLY, OUTBOUND_ONLY, SAGA -> RoleClassification.UNKNOWN;
        };
    }

    /**
     * Extracts dependency relationships from the IR snapshot.
     *
     * <p>This method builds a map where each key is a fully-qualified type name,
     * and the value is a set of fully-qualified type names that it depends on.
     *
     * <p>Dependencies are extracted from:
     * <ul>
     *   <li>Domain relations (OneToMany, ManyToOne, etc.)</li>
     *   <li>Properties with non-primitive types</li>
     *   <li>Annotations on the type (for domain purity checking)</li>
     * </ul>
     *
     * @param ir the IR snapshot
     * @return map of type name to its dependencies
     */
    private Map<String, Set<String>> extractDependencies(IrSnapshot ir) {
        Map<String, Set<String>> deps = new HashMap<>();

        // Extract from domain types
        for (DomainType type : ir.domain().types()) {
            Set<String> typeDeps = new HashSet<>();

            // From relations
            for (io.hexaglue.spi.ir.DomainRelation rel : type.relations()) {
                typeDeps.add(rel.targetTypeFqn());
            }

            // From properties (non-primitive types)
            for (io.hexaglue.spi.ir.DomainProperty prop : type.properties()) {
                String propType = prop.type().qualifiedName();
                if (!isPrimitive(propType)) {
                    typeDeps.add(propType);
                }
            }

            // From annotations (for domain purity validation)
            // Annotations like jakarta.validation.constraints.Email indicate
            // infrastructure dependencies in the domain layer
            for (String annotation : type.annotations()) {
                typeDeps.add(annotation);
            }

            deps.put(type.qualifiedName(), typeDeps);
        }

        return deps;
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
     * Builds a Codebase from v4 ArchitecturalModel.
     *
     * <p>This converts the v4 model elements (DomainEntity, ValueObject, DrivenPort, etc.)
     * into code units that can be audited.
     *
     * @param model the v4 architectural model
     * @return the codebase for auditing
     * @since 4.0.0
     */
    private Codebase buildCodebaseFromModel(ArchitecturalModel model) {
        List<CodeUnit> units = new ArrayList<>();
        Map<String, Set<String>> dependencies = new HashMap<>();

        // Convert domain entities to code units
        model.domainEntities().forEach(entity -> {
            CodeUnit unit = toCodeUnitV4(entity);
            units.add(unit);
            dependencies.put(entity.id().qualifiedName(), extractDependenciesV4(entity));
        });

        // Convert value objects to code units
        model.valueObjects().forEach(vo -> {
            CodeUnit unit = toCodeUnitV4(vo);
            units.add(unit);
            dependencies.put(vo.id().qualifiedName(), extractDependenciesV4(vo));
        });

        // Convert identifiers to code units
        model.identifiers().forEach(id -> {
            CodeUnit unit = toCodeUnitV4(id);
            units.add(unit);
            dependencies.put(id.id().qualifiedName(), extractDependenciesV4(id));
        });

        // Convert domain events to code units
        model.domainEvents().forEach(event -> {
            CodeUnit unit = toCodeUnitV4(event);
            units.add(unit);
            dependencies.put(event.id().qualifiedName(), extractDependenciesV4(event));
        });

        // Convert domain services to code units
        model.domainServices().forEach(service -> {
            CodeUnit unit = toCodeUnitV4(service);
            units.add(unit);
            dependencies.put(service.id().qualifiedName(), extractDependenciesV4(service));
        });

        // Convert application services to code units
        model.applicationServices().forEach(appService -> {
            CodeUnit unit = toCodeUnitV4(appService);
            units.add(unit);
            dependencies.put(appService.id().qualifiedName(), extractDependenciesV4(appService));
        });

        // Convert driving ports to code units
        model.drivingPorts().forEach(port -> {
            CodeUnit unit = toCodeUnitV4(port);
            units.add(unit);
            dependencies.put(port.id().qualifiedName(), extractDependenciesV4(port));
        });

        // Convert driven ports to code units
        model.drivenPorts().forEach(port -> {
            CodeUnit unit = toCodeUnitV4(port);
            units.add(unit);
            dependencies.put(port.id().qualifiedName(), extractDependenciesV4(port));
        });

        return new Codebase("audit-target", inferBasePackage(units), units, dependencies);
    }

    /**
     * Converts a v4 DomainEntity to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.domain.DomainEntity entity) {
        io.hexaglue.syntax.TypeSyntax syntax = entity.syntax();

        // Build field declarations
        List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = syntax != null && syntax.fields() != null
                ? syntax.fields().stream()
                        .map(field -> new io.hexaglue.spi.audit.FieldDeclaration(
                                field.name(),
                                field.type() != null ? field.type().qualifiedName() : "Object",
                                Set.of(),
                                isIdentityFieldV4(field, entity) ? Set.of("Id") : Set.of()))
                        .toList()
                : List.of();

        // Build method declarations
        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = syntax != null && syntax.methods() != null
                ? syntax.methods().stream().map(this::toMethodDeclarationV4).toList()
                : List.of();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);

        // Determine code unit kind
        CodeUnitKind unitKind = syntax != null && syntax.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        // Determine role - aggregate root or entity
        RoleClassification role =
                entity.isAggregateRoot() ? RoleClassification.AGGREGATE_ROOT : RoleClassification.ENTITY;

        return new CodeUnit(
                entity.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                role,
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v4 ValueObject to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.domain.ValueObject vo) {
        io.hexaglue.syntax.TypeSyntax syntax = vo.syntax();

        List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = syntax != null && syntax.fields() != null
                ? syntax.fields().stream()
                        .map(field -> new io.hexaglue.spi.audit.FieldDeclaration(
                                field.name(),
                                field.type() != null ? field.type().qualifiedName() : "Object",
                                Set.of(),
                                Set.of()))
                        .toList()
                : List.of();

        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = syntax != null && syntax.methods() != null
                ? syntax.methods().stream().map(this::toMethodDeclarationV4).toList()
                : List.of();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);
        CodeUnitKind unitKind = syntax != null && syntax.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

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
     * Converts a v4 Identifier to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.domain.Identifier id) {
        io.hexaglue.syntax.TypeSyntax syntax = id.syntax();

        List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = syntax != null && syntax.fields() != null
                ? syntax.fields().stream()
                        .map(field -> new io.hexaglue.spi.audit.FieldDeclaration(
                                field.name(),
                                field.type() != null ? field.type().qualifiedName() : "Object",
                                Set.of(),
                                Set.of()))
                        .toList()
                : List.of();

        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = syntax != null && syntax.methods() != null
                ? syntax.methods().stream().map(this::toMethodDeclarationV4).toList()
                : List.of();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);
        CodeUnitKind unitKind = syntax != null && syntax.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

        return new CodeUnit(
                id.id().qualifiedName(),
                unitKind,
                LayerClassification.DOMAIN,
                RoleClassification.VALUE_OBJECT, // Identifiers are value objects semantically
                methodDecls,
                fieldDecls,
                codeMetrics,
                new DocumentationInfo(false, 0, List.of()));
    }

    /**
     * Converts a v4 DomainEvent to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.domain.DomainEvent event) {
        io.hexaglue.syntax.TypeSyntax syntax = event.syntax();

        List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = syntax != null && syntax.fields() != null
                ? syntax.fields().stream()
                        .map(field -> new io.hexaglue.spi.audit.FieldDeclaration(
                                field.name(),
                                field.type() != null ? field.type().qualifiedName() : "Object",
                                Set.of(),
                                Set.of()))
                        .toList()
                : List.of();

        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = syntax != null && syntax.methods() != null
                ? syntax.methods().stream().map(this::toMethodDeclarationV4).toList()
                : List.of();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), fieldDecls.size(), 100.0);
        CodeUnitKind unitKind = syntax != null && syntax.isRecord() ? CodeUnitKind.RECORD : CodeUnitKind.CLASS;

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
     * Converts a v4 DomainService to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.domain.DomainService service) {
        io.hexaglue.syntax.TypeSyntax syntax = service.syntax();

        List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = syntax != null && syntax.fields() != null
                ? syntax.fields().stream()
                        .map(field -> new io.hexaglue.spi.audit.FieldDeclaration(
                                field.name(),
                                field.type() != null ? field.type().qualifiedName() : "Object",
                                Set.of(),
                                Set.of()))
                        .toList()
                : List.of();

        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = syntax != null && syntax.methods() != null
                ? syntax.methods().stream().map(this::toMethodDeclarationV4).toList()
                : List.of();

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
     * Converts a v4 ApplicationService to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.ports.ApplicationService appService) {
        io.hexaglue.syntax.TypeSyntax syntax = appService.syntax();

        List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = syntax != null && syntax.fields() != null
                ? syntax.fields().stream()
                        .map(field -> new io.hexaglue.spi.audit.FieldDeclaration(
                                field.name(),
                                field.type() != null ? field.type().qualifiedName() : "Object",
                                Set.of(),
                                Set.of()))
                        .toList()
                : List.of();

        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = syntax != null && syntax.methods() != null
                ? syntax.methods().stream().map(this::toMethodDeclarationV4).toList()
                : List.of();

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
     * Converts a v4 DrivingPort to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.ports.DrivingPort port) {
        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = port.operations().stream()
                .map(op -> new io.hexaglue.spi.audit.MethodDeclaration(
                        op.name(),
                        op.returnType() != null ? op.returnType().qualifiedName() : "void",
                        op.parameterTypes().stream()
                                .map(io.hexaglue.syntax.TypeRef::qualifiedName)
                                .toList(),
                        Set.of(),
                        Set.of(),
                        0))
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
     * Converts a v4 DrivenPort to a CodeUnit.
     */
    private CodeUnit toCodeUnitV4(io.hexaglue.arch.ports.DrivenPort port) {
        List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = port.operations().stream()
                .map(op -> new io.hexaglue.spi.audit.MethodDeclaration(
                        op.name(),
                        op.returnType() != null ? op.returnType().qualifiedName() : "void",
                        op.parameterTypes().stream()
                                .map(io.hexaglue.syntax.TypeRef::qualifiedName)
                                .toList(),
                        Set.of(),
                        Set.of(),
                        0))
                .toList();

        CodeMetrics codeMetrics = new CodeMetrics(0, 0, methodDecls.size(), 0, 100.0);

        // Determine role based on classification
        RoleClassification role = port.classification() == io.hexaglue.arch.ports.PortClassification.REPOSITORY
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
     * Converts a v4 MethodSyntax to a MethodDeclaration.
     */
    private io.hexaglue.spi.audit.MethodDeclaration toMethodDeclarationV4(io.hexaglue.syntax.MethodSyntax method) {
        String returnType = method.returnType() != null ? method.returnType().qualifiedName() : "void";
        List<String> paramTypes = method.parameters().stream()
                .map(p -> p.type() != null ? p.type().qualifiedName() : "Object")
                .toList();

        // Extract modifiers
        Set<String> modifiers = method.modifiers() != null
                ? method.modifiers().stream()
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(java.util.stream.Collectors.toSet())
                : Set.of();

        return new io.hexaglue.spi.audit.MethodDeclaration(
                method.name(), returnType, paramTypes, modifiers, Set.of(), 1 // complexity
                );
    }

    /**
     * Checks if a field is the identity field for a DomainEntity.
     */
    private boolean isIdentityFieldV4(
            io.hexaglue.syntax.FieldSyntax field, io.hexaglue.arch.domain.DomainEntity entity) {
        return entity.hasIdentity()
                && entity.identityField() != null
                && entity.identityField().equals(field.name());
    }

    /**
     * Extracts dependencies from a v4 DomainEntity.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.domain.DomainEntity entity) {
        Set<String> deps = new HashSet<>();
        io.hexaglue.syntax.TypeSyntax syntax = entity.syntax();
        if (syntax != null) {
            // From fields
            if (syntax.fields() != null) {
                syntax.fields().forEach(field -> {
                    if (field.type() != null && !isPrimitive(field.type().qualifiedName())) {
                        deps.add(field.type().qualifiedName());
                    }
                });
            }
            // From annotations
            if (syntax.annotations() != null) {
                syntax.annotations().forEach(ann -> deps.add(ann.qualifiedName()));
            }
        }
        return deps;
    }

    /**
     * Extracts dependencies from a v4 ValueObject.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.domain.ValueObject vo) {
        Set<String> deps = new HashSet<>();
        io.hexaglue.syntax.TypeSyntax syntax = vo.syntax();
        if (syntax != null && syntax.fields() != null) {
            syntax.fields().forEach(field -> {
                if (field.type() != null && !isPrimitive(field.type().qualifiedName())) {
                    deps.add(field.type().qualifiedName());
                }
            });
        }
        return deps;
    }

    /**
     * Extracts dependencies from a v4 Identifier.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.domain.Identifier id) {
        Set<String> deps = new HashSet<>();
        io.hexaglue.syntax.TypeSyntax syntax = id.syntax();
        if (syntax != null && syntax.fields() != null) {
            syntax.fields().forEach(field -> {
                if (field.type() != null && !isPrimitive(field.type().qualifiedName())) {
                    deps.add(field.type().qualifiedName());
                }
            });
        }
        return deps;
    }

    /**
     * Extracts dependencies from a v4 DomainEvent.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.domain.DomainEvent event) {
        Set<String> deps = new HashSet<>();
        io.hexaglue.syntax.TypeSyntax syntax = event.syntax();
        if (syntax != null && syntax.fields() != null) {
            syntax.fields().forEach(field -> {
                if (field.type() != null && !isPrimitive(field.type().qualifiedName())) {
                    deps.add(field.type().qualifiedName());
                }
            });
        }
        return deps;
    }

    /**
     * Extracts dependencies from a v4 DomainService.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.domain.DomainService service) {
        Set<String> deps = new HashSet<>();
        io.hexaglue.syntax.TypeSyntax syntax = service.syntax();
        if (syntax != null && syntax.fields() != null) {
            syntax.fields().forEach(field -> {
                if (field.type() != null && !isPrimitive(field.type().qualifiedName())) {
                    deps.add(field.type().qualifiedName());
                }
            });
        }
        return deps;
    }

    /**
     * Extracts dependencies from a v4 ApplicationService.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.ports.ApplicationService appService) {
        Set<String> deps = new HashSet<>();
        io.hexaglue.syntax.TypeSyntax syntax = appService.syntax();
        if (syntax != null && syntax.fields() != null) {
            syntax.fields().forEach(field -> {
                if (field.type() != null && !isPrimitive(field.type().qualifiedName())) {
                    deps.add(field.type().qualifiedName());
                }
            });
        }
        return deps;
    }

    /**
     * Extracts dependencies from a v4 DrivingPort.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.ports.DrivingPort port) {
        Set<String> deps = new HashSet<>();
        // Ports are interfaces, extract from method signatures
        port.operations().forEach(op -> {
            if (op.returnType() != null && !isPrimitive(op.returnType().qualifiedName())) {
                deps.add(op.returnType().qualifiedName());
            }
            op.parameterTypes().forEach(param -> {
                if (!isPrimitive(param.qualifiedName())) {
                    deps.add(param.qualifiedName());
                }
            });
        });
        return deps;
    }

    /**
     * Extracts dependencies from a v4 DrivenPort.
     */
    private Set<String> extractDependenciesV4(io.hexaglue.arch.ports.DrivenPort port) {
        Set<String> deps = new HashSet<>();
        port.operations().forEach(op -> {
            if (op.returnType() != null && !isPrimitive(op.returnType().qualifiedName())) {
                deps.add(op.returnType().qualifiedName());
            }
            op.parameterTypes().forEach(param -> {
                if (!isPrimitive(param.qualifiedName())) {
                    deps.add(param.qualifiedName());
                }
            });
        });
        return deps;
    }
}
