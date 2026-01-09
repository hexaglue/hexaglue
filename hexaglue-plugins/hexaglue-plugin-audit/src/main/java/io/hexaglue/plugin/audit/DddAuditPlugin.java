/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.hexaglue.plugin.audit;

import io.hexaglue.plugin.audit.adapter.analyzer.DefaultArchitectureQuery;
import io.hexaglue.plugin.audit.adapter.metric.AggregateMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.CouplingMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.DomainCoverageMetricCalculator;
import io.hexaglue.plugin.audit.adapter.metric.PortCoverageMetricCalculator;
import io.hexaglue.plugin.audit.adapter.report.ConsoleReportGenerator;
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
import io.hexaglue.spi.core.SourceLocation;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
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
import java.util.stream.Collectors;

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
 */
public class DddAuditPlugin implements AuditPlugin {

    public static final String PLUGIN_ID = "io.hexaglue.plugin.audit.ddd";

    private final ConstraintRegistry registry;

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

        // Use core's architecture query if available, otherwise create fallback
        var architectureQuery = context.query()
                .orElseGet(() -> new DefaultArchitectureQuery(codebase));

        // Execute audit with architecture query
        AuditResult result = orchestrator.executeAudit(
                codebase,
                architectureQuery,
                config.enabledConstraints(),
                config.enabledMetrics(),
                config.allowCriticalViolations());

        // Store result for report generation (if called from execute method)
        this.lastAuditResult = result;
        this.lastAuditConfig = config;

        // Log summary
        diagnostics.info("Audit complete: %d violations, %d metrics"
                .formatted(result.violations().size(), result.metrics().size()));

        if (result.outcome() == BuildOutcome.FAIL) {
            long blockerCount = result.blockerCount();
            long criticalCount = result.criticalCount();
            diagnostics.error("Audit FAILED: %d blocker, %d critical violations"
                    .formatted(blockerCount, criticalCount));
        }

        // Convert to SPI snapshot
        Duration duration = Duration.between(startTime, Instant.now());
        return convertToAuditSnapshot(result, codebase, duration, architectureQuery);
    }

    // Transient state for report generation
    private AuditResult lastAuditResult;
    private AuditConfiguration lastAuditConfig;

    // Store architecture query for report generation
    private io.hexaglue.spi.audit.ArchitectureQuery lastArchitectureQuery;

    /**
     * Converts domain AuditResult to SPI AuditSnapshot.
     */
    private AuditSnapshot convertToAuditSnapshot(
            AuditResult result,
            Codebase codebase,
            Duration duration,
            io.hexaglue.spi.audit.ArchitectureQuery architectureQuery) {
        // Convert violations
        List<RuleViolation> ruleViolations =
                result.violations().stream().map(this::convertViolation).toList();

        // Compute quality metrics (simplified - just counts for now)
        QualityMetrics qualityMetrics = new QualityMetrics(0.0, 0.0, 0, 0.0);

        // Compute architecture metrics using the architecture query
        ArchitectureMetrics archMetrics = computeArchitectureMetrics(codebase, architectureQuery);

        // Store for report generation
        this.lastArchitectureQuery = architectureQuery;

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
                violation.constraintId().value(), convertSeverity(violation.severity()), violation.message(),
                violation.location());
    }

    /**
     * Converts domain Severity to SPI Severity.
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
        int aggregateCount = codebase.unitsWithRole(RoleClassification.AGGREGATE_ROOT).size();

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
     * and port coverage. Each metric has defined thresholds that trigger warnings
     * when exceeded.
     *
     * @return map of metric name to calculator
     */
    private Map<String, MetricCalculator> buildCalculatorMap() {
        AggregateMetricCalculator aggregateMetric = new AggregateMetricCalculator();
        CouplingMetricCalculator couplingMetric = new CouplingMetricCalculator();
        DomainCoverageMetricCalculator domainCoverageMetric = new DomainCoverageMetricCalculator();
        PortCoverageMetricCalculator portCoverageMetric = new PortCoverageMetricCalculator();

        return Map.of(
                aggregateMetric.metricName(), aggregateMetric,
                couplingMetric.metricName(), couplingMetric,
                domainCoverageMetric.metricName(), domainCoverageMetric,
                portCoverageMetric.metricName(), portCoverageMetric);
    }

    /**
     * Overrides execute to properly build AuditContext.
     *
     * <p>This method adapts the generic PluginContext to AuditContext by building
     * a Codebase from the IrSnapshot. If the core's ArchitectureQuery is available,
     * it is passed through to leverage rich analysis capabilities.
     */
    @Override
    public void execute(PluginContext context) {
        try {
            // Build codebase from IR
            Codebase codebase = buildCodebaseFromIr(context.ir());

            // Get architecture query from core if available
            io.hexaglue.spi.audit.ArchitectureQuery coreQuery =
                    context.architectureQuery().orElse(null);

            // Create audit context with core's architecture query
            AuditContext auditContext = new AuditContext(
                    codebase, List.of(), context.diagnostics(), context.config(), coreQuery);

            // Execute audit
            AuditSnapshot snapshot = audit(auditContext);

            // Generate reports (uses lastAuditResult and lastAuditConfig set by audit method)
            if (lastAuditResult != null && lastAuditConfig != null) {
                generateReports(snapshot, context, lastAuditResult, lastAuditConfig);
            }

            // Report if failed
            if (!snapshot.passed()) {
                context.diagnostics().warn("Audit found %d errors".formatted(snapshot.errorCount()));
            }
        } catch (Exception e) {
            context.diagnostics().error("Audit plugin execution failed: " + id(), e);
        }
    }

    /**
     * Generates audit reports in multiple formats.
     *
     * <p>This method uses the stored architecture query to include rich
     * architecture analysis data (cycles, layer violations, coupling metrics)
     * in the reports when available from the core.
     *
     * @param snapshot the audit snapshot
     * @param context  the plugin context
     * @param result   the domain audit result
     * @param config   the audit configuration
     */
    private void generateReports(
            AuditSnapshot snapshot, PluginContext context, AuditResult result, AuditConfiguration config) {
        try {
            // Build unified report model
            List<String> constraintIds = new ArrayList<>(config.enabledConstraints());

            // Extract project name from IR or use a default
            String projectName = inferProjectName(context.ir());

            // Build report with architecture analysis from core
            AuditReport report = AuditReport.from(
                    snapshot, projectName, result.metrics(), constraintIds, lastArchitectureQuery);

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
                                        .formatted(
                                                format.name(),
                                                outputDir.resolve(format.defaultFilename())));
                    }
                }
            }
        } catch (IOException e) {
            context.diagnostics().warn("Failed to generate audit reports: " + e.getMessage());
        }
    }

    /**
     * Infers project name from IR snapshot.
     *
     * @param ir the IR snapshot
     * @return inferred project name
     */
    private String inferProjectName(IrSnapshot ir) {
        // Try to extract from base package
        if (!ir.domain().types().isEmpty()) {
            String firstType = ir.domain().types().get(0).qualifiedName();
            int firstDot = firstType.indexOf('.');
            if (firstDot > 0) {
                return firstType.substring(0, firstDot);
            }
        }
        return "audit-target";
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
     * be audited.
     */
    private Codebase buildCodebaseFromIr(IrSnapshot ir) {
        List<CodeUnit> units = new ArrayList<>();

        // Convert domain types to code units
        for (DomainType type : ir.domain().types()) {
            List<io.hexaglue.spi.audit.FieldDeclaration> fieldDecls = type.properties().stream()
                    .map(prop -> new io.hexaglue.spi.audit.FieldDeclaration(
                            prop.name(), prop.type().qualifiedName(), Set.of(), Set.of()))
                    .toList();

            CodeMetrics codeMetrics =
                    new CodeMetrics(0, 0, 0, fieldDecls.size(), 100.0);

            units.add(new CodeUnit(
                    type.qualifiedName(),
                    CodeUnitKind.CLASS,
                    LayerClassification.DOMAIN,
                    roleFromDomainKind(type.kind()),
                    List.of(), // methods - not available in current IR
                    fieldDecls,
                    codeMetrics,
                    new DocumentationInfo(false, 0, List.of())));
        }

        // Convert ports to code units
        for (Port port : ir.ports().ports()) {
            List<io.hexaglue.spi.audit.MethodDeclaration> methodDecls = port.methods().stream()
                    .map(method -> new io.hexaglue.spi.audit.MethodDeclaration(
                            method.name(),
                            method.returnType(),
                            method.parameters(), // List<String> already
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

        // Build dependency map from IR
        Map<String, java.util.Set<String>> dependencies = extractDependencies(ir);

        return new Codebase("audit-target", inferBasePackage(units), units, dependencies);
    }

    /**
     * Converts DomainKind to RoleClassification.
     */
    private RoleClassification roleFromDomainKind(DomainKind kind) {
        return switch (kind) {
            case AGGREGATE_ROOT -> RoleClassification.AGGREGATE_ROOT;
            case ENTITY -> RoleClassification.ENTITY;
            case VALUE_OBJECT -> RoleClassification.VALUE_OBJECT;
            case DOMAIN_SERVICE -> RoleClassification.SERVICE;
            case DOMAIN_EVENT -> RoleClassification.UNKNOWN; // No EVENT in RoleClassification
            default -> RoleClassification.UNKNOWN;
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
}
