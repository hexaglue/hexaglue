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
import io.hexaglue.arch.model.ApplicationService;
import io.hexaglue.arch.model.CommandHandler;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Method;
import io.hexaglue.arch.model.MethodRole;
import io.hexaglue.arch.model.QueryHandler;
import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.TypeRegistry;
import io.hexaglue.arch.model.TypeStructure;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.audit.AuditSnapshot;
import io.hexaglue.arch.model.audit.BoundedContextInfo;
import io.hexaglue.arch.model.graph.RelationType;
import io.hexaglue.arch.model.index.CompositionIndex;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.ModuleDescriptor;
import io.hexaglue.arch.model.index.ModuleIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.audit.adapter.diagram.MermaidTypeConverter;
import io.hexaglue.plugin.audit.adapter.report.model.HealthScore;
import io.hexaglue.plugin.audit.domain.model.AuditResult;
import io.hexaglue.plugin.audit.domain.model.Metric;
import io.hexaglue.plugin.audit.domain.model.Severity;
import io.hexaglue.plugin.audit.domain.model.Violation;
import io.hexaglue.plugin.audit.domain.model.report.*;
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
                projectName, projectVersion, Instant.now(), formatDuration(duration), hexaglueVersion, pluginVersion);

        // Extract classified packages for health score filtering
        Set<String> classifiedPackages = extractClassifiedPackages(model);

        // Calculate health score (filtered to classified packages only)
        HealthScore healthScore =
                healthScoreCalculator.calculate(auditResult.violations(), architectureQuery, classifiedPackages);

        // Build sections
        Verdict verdict = buildVerdict(auditResult, healthScore);
        ArchitectureOverview architecture = buildArchitecture(model, architectureQuery, auditResult.violations());
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
        long blockers = violations.stream()
                .filter(v -> v.severity() == Severity.BLOCKER)
                .count();
        long criticals = violations.stream()
                .filter(v -> v.severity() == Severity.CRITICAL)
                .count();

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
            int score = healthScore.overall();
            if (score < 60) {
                return "Your application has significant architectural issues that require attention.";
            }
            if (score < 70) {
                return "Your application has notable architectural issues that should be addressed.";
            }
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

    private ArchitectureOverview buildArchitecture(
            ArchitecturalModel model, ArchitectureQuery architectureQuery, List<Violation> violations) {
        DomainIndex domainIndex = model.domainIndex().orElse(null);
        PortIndex portIndex = model.portIndex().orElse(null);
        TypeRegistry registry = model.typeRegistry().orElse(null);
        CompositionIndex compositionIndex = model.compositionIndex().orElse(null);

        // Build inventory
        List<BoundedContextInventory> bcInventories = new ArrayList<>();
        if (architectureQuery != null && domainIndex != null) {
            for (BoundedContextInfo bc : architectureQuery.findBoundedContexts()) {
                // Count domain types by filtering those belonging to this bounded context
                int aggregateCount = (int) domainIndex
                        .aggregateRoots()
                        .filter(ar -> bc.containsType(ar.id().qualifiedName()))
                        .count();
                int entityCount = (int) domainIndex
                        .entities()
                        .filter(e -> bc.containsType(e.id().qualifiedName()))
                        .count();
                int voCount = (int) domainIndex
                        .valueObjects()
                        .filter(vo -> bc.containsType(vo.id().qualifiedName()))
                        .count();
                int eventCount = (int) domainIndex
                        .domainEvents()
                        .filter(de -> bc.containsType(de.id().qualifiedName()))
                        .count();
                bcInventories.add(
                        new BoundedContextInventory(bc.name(), aggregateCount, entityCount, voCount, eventCount));
            }
        }

        InventoryTotals totals = calculateTotals(domainIndex, portIndex, registry);
        Inventory inventory = new Inventory(bcInventories, totals);

        // Build component details (including application layer from registry)
        ComponentDetails components =
                buildComponentDetails(domainIndex, portIndex, registry, compositionIndex, architectureQuery);

        // Build relationships (cycles from violations + compositions from CompositionIndex)
        List<Relationship> relationships = buildRelationships(violations, compositionIndex);

        // Build type violations for diagram visualization
        List<TypeViolation> typeViolations = buildTypeViolations(violations);

        // Build module topology for multi-module projects
        ModuleTopology moduleTopology = buildModuleTopology(model);

        String summary = String.format(
                "Analyzed %d types across %d bounded context%s",
                totals.total(), bcInventories.size(), bcInventories.size() != 1 ? "s" : "");

        return new ArchitectureOverview(
                summary, inventory, components, DiagramsInfo.defaults(), relationships, typeViolations, moduleTopology);
    }

    private InventoryTotals calculateTotals(DomainIndex domainIndex, PortIndex portIndex, TypeRegistry registry) {
        int aggregates =
                domainIndex != null ? (int) domainIndex.aggregateRoots().count() : 0;
        int entities = domainIndex != null ? (int) domainIndex.entities().count() : 0;
        int valueObjects =
                domainIndex != null ? (int) domainIndex.valueObjects().count() : 0;
        int identifiers = domainIndex != null ? (int) domainIndex.identifiers().count() : 0;
        int domainEvents =
                domainIndex != null ? (int) domainIndex.domainEvents().count() : 0;
        int domainServices =
                domainIndex != null ? (int) domainIndex.domainServices().count() : 0;
        int applicationServices =
                registry != null ? (int) registry.all(ApplicationService.class).count() : 0;
        int commandHandlers =
                registry != null ? (int) registry.all(CommandHandler.class).count() : 0;
        int queryHandlers =
                registry != null ? (int) registry.all(QueryHandler.class).count() : 0;
        int drivingPorts = portIndex != null ? (int) portIndex.drivingPorts().count() : 0;
        int drivenPorts = portIndex != null ? (int) portIndex.drivenPorts().count() : 0;

        return new InventoryTotals(
                aggregates,
                entities,
                valueObjects,
                identifiers,
                domainEvents,
                domainServices,
                applicationServices,
                commandHandlers,
                queryHandlers,
                drivingPorts,
                drivenPorts);
    }

    private ComponentDetails buildComponentDetails(
            DomainIndex domainIndex,
            PortIndex portIndex,
            TypeRegistry registry,
            CompositionIndex compositionIndex,
            ArchitectureQuery architectureQuery) {

        // Domain Layer components
        List<AggregateComponent> aggregates = new ArrayList<>();
        List<EntityComponent> entities = new ArrayList<>();
        List<ValueObjectComponent> valueObjects = new ArrayList<>();
        List<IdentifierComponent> identifiers = new ArrayList<>();
        List<DomainEventComponent> domainEvents = new ArrayList<>();
        List<DomainServiceComponent> domainServices = new ArrayList<>();

        // Application Layer components
        List<ApplicationServiceComponent> applicationServices = new ArrayList<>();
        List<CommandHandlerComponent> commandHandlers = new ArrayList<>();
        List<QueryHandlerComponent> queryHandlers = new ArrayList<>();

        // Ports Layer components
        List<PortComponent> drivingPorts = new ArrayList<>();
        List<PortComponent> drivenPorts = new ArrayList<>();

        // Adapters
        List<AdapterComponent> adapters = new ArrayList<>();

        if (domainIndex != null) {
            // Aggregates
            for (AggregateRoot agg : domainIndex.aggregateRoots().toList()) {
                List<FieldDetail> fieldDetails = extractFieldDetails(agg.structure());
                List<MethodDetail> methodDetails = extractMethodDetails(agg.structure());
                aggregates.add(AggregateComponent.of(
                        agg.id().simpleName(),
                        extractPackage(agg.id().qualifiedName()),
                        agg.structure().fields().size(),
                        List.of(), // References extracted from relationships
                        List.of(), // Ports extracted from relationships
                        fieldDetails,
                        methodDetails));
            }

            // Entities
            for (Entity entity : domainIndex.entities().toList()) {
                String identityField = entity.identityField().map(f -> f.name()).orElse(null);
                String owningAggregate = entity.owningAggregate()
                        .map(ref -> extractSimpleName(ref.qualifiedName()))
                        .orElse(null);
                List<FieldDetail> fieldDetails = extractFieldDetails(entity.structure());
                entities.add(EntityComponent.of(
                        entity.id().simpleName(),
                        extractPackage(entity.id().qualifiedName()),
                        entity.structure().fields().size(),
                        identityField,
                        owningAggregate,
                        fieldDetails));
            }

            // Value Objects
            for (ValueObject vo : domainIndex.valueObjects().toList()) {
                List<FieldDetail> fieldDetails = extractFieldDetails(vo.structure());
                valueObjects.add(ValueObjectComponent.of(
                        vo.id().simpleName(), extractPackage(vo.id().qualifiedName()), fieldDetails));
            }

            // Identifiers
            for (Identifier id : domainIndex.identifiers().toList()) {
                identifiers.add(new IdentifierComponent(
                        id.id().simpleName(),
                        extractPackage(id.id().qualifiedName()),
                        id.wrappedType().qualifiedName()));
            }

            // Domain Events
            for (DomainEvent event : domainIndex.domainEvents().toList()) {
                String publishedBy = event.sourceAggregate()
                        .map(ref -> extractSimpleName(ref.qualifiedName()))
                        .orElse(null);
                domainEvents.add(DomainEventComponent.of(
                        event.id().simpleName(),
                        extractPackage(event.id().qualifiedName()),
                        event.structure().fields().size(),
                        publishedBy));
            }

            // Domain Services
            for (DomainService svc : domainIndex.domainServices().toList()) {
                List<String> usedAggregates = svc.injectedPorts().stream()
                        .map(ref -> extractSimpleName(ref.qualifiedName()))
                        .toList();
                domainServices.add(DomainServiceComponent.of(
                        svc.id().simpleName(),
                        extractPackage(svc.id().qualifiedName()),
                        svc.operations().size(),
                        usedAggregates));
            }
        }

        // Application Layer from TypeRegistry
        if (registry != null) {
            // Application Services
            for (ApplicationService svc : registry.all(ApplicationService.class).toList()) {
                List<MethodDetail> methodDetails = extractMethodDetails(svc.structure());
                applicationServices.add(ApplicationServiceComponent.of(
                        svc.id().simpleName(),
                        extractPackage(svc.id().qualifiedName()),
                        svc.structure().methods().size(),
                        List.of(), // orchestrates
                        List.of(), // usesPorts
                        methodDetails));
            }

            // Command Handlers
            for (CommandHandler handler : registry.all(CommandHandler.class).toList()) {
                commandHandlers.add(CommandHandlerComponent.of(
                        handler.id().simpleName(), extractPackage(handler.id().qualifiedName())));
            }

            // Query Handlers
            for (QueryHandler handler : registry.all(QueryHandler.class).toList()) {
                queryHandlers.add(QueryHandlerComponent.of(
                        handler.id().simpleName(), extractPackage(handler.id().qualifiedName())));
            }
        }

        // Ports Layer from PortIndex
        if (portIndex != null) {
            for (DrivingPort port : portIndex.drivingPorts().toList()) {
                List<MethodDetail> methodDetails = extractMethodDetails(port.structure());
                Optional<TypeId> adapterId = findAdapterForPort(port.id(), compositionIndex, architectureQuery);
                boolean hasAdapter = adapterId.isPresent();
                String adapterName = adapterId.map(id -> id.simpleName()).orElse(null);

                drivingPorts.add(PortComponent.driving(
                        port.id().simpleName(),
                        extractPackage(port.id().qualifiedName()),
                        port.structure().methods().size(),
                        hasAdapter,
                        adapterName,
                        List.of(),
                        methodDetails));

                adapterId.ifPresent(id -> adapters.add(new AdapterComponent(
                        id.simpleName(),
                        extractPackage(id.qualifiedName()),
                        port.id().simpleName(),
                        AdapterComponent.AdapterType.DRIVING)));
            }

            for (DrivenPort port : portIndex.drivenPorts().toList()) {
                List<MethodDetail> methodDetails = extractMethodDetails(port.structure());
                Optional<TypeId> adapterId = findAdapterForPort(port.id(), compositionIndex, architectureQuery);
                boolean hasAdapter = adapterId.isPresent();
                String adapterName = adapterId.map(id -> id.simpleName()).orElse(null);

                drivenPorts.add(PortComponent.driven(
                        port.id().simpleName(),
                        extractPackage(port.id().qualifiedName()),
                        port.portType().name(),
                        port.structure().methods().size(),
                        hasAdapter,
                        adapterName,
                        methodDetails));

                adapterId.ifPresent(id -> adapters.add(new AdapterComponent(
                        id.simpleName(),
                        extractPackage(id.qualifiedName()),
                        port.id().simpleName(),
                        AdapterComponent.AdapterType.DRIVEN)));
            }
        }

        return new ComponentDetails(
                aggregates,
                entities,
                valueObjects,
                identifiers,
                domainEvents,
                domainServices,
                applicationServices,
                commandHandlers,
                queryHandlers,
                drivingPorts,
                drivenPorts,
                adapters);
    }

    /**
     * Finds the first adapter implementing a given port.
     *
     * <p>Uses two strategies:
     * <ol>
     *   <li><strong>CompositionIndex</strong>: queries the relationship graph for
     *       IMPLEMENTS relationships targeting the port</li>
     *   <li><strong>ApplicationGraph via ArchitectureQuery</strong>: searches the full
     *       graph for implementors (covers adapters in excluded packages)</li>
     * </ol>
     *
     * @param portId the port's type identifier
     * @param compositionIndex the composition index (may be null)
     * @param architectureQuery the architecture query for full graph access (may be null)
     * @return the adapter TypeId if found
     * @since 5.0.0
     */
    private Optional<TypeId> findAdapterForPort(
            TypeId portId, CompositionIndex compositionIndex, ArchitectureQuery architectureQuery) {
        // Strategy 1: Check CompositionIndex (classified types only)
        if (compositionIndex != null) {
            Optional<TypeId> found = compositionIndex
                    .graph()
                    .to(portId)
                    .filter(r -> r.type() == RelationType.IMPLEMENTS)
                    .map(r -> r.source())
                    .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }

        // Strategy 2: Check full ApplicationGraph via ArchitectureQuery (handles excluded packages)
        if (architectureQuery != null) {
            List<String> implementors = architectureQuery.findImplementors(portId.qualifiedName());
            if (!implementors.isEmpty()) {
                return Optional.of(TypeId.of(implementors.get(0)));
            }
        }

        return Optional.empty();
    }

    /**
     * Builds relationships from violations and composition index.
     *
     * <p>This method combines:
     * <ul>
     *   <li>Cycle relationships from violations (ddd:aggregate-cycle)</li>
     *   <li>Composition relationships from CompositionIndex (OWNS, CONTAINS, REFERENCES)</li>
     * </ul>
     *
     * @param violations list of audit violations
     * @param compositionIndex composition index for cross-package relationships (may be null)
     * @return list of relationships for diagram rendering
     * @since 5.0.0
     */
    private List<Relationship> buildRelationships(List<Violation> violations, CompositionIndex compositionIndex) {
        List<Relationship> relationships = new ArrayList<>();
        Set<String> addedRelationships = new HashSet<>();

        // 1. Add cycle relationships from violations (ddd:aggregate-cycle)
        for (Violation violation : violations) {
            if ("ddd:aggregate-cycle".equals(violation.constraintId().value())) {
                // Extract cycle path from violation message
                // Format: "Circular dependency between aggregates: A -> B -> A"
                String message = violation.message();
                List<String> cyclePath = extractCyclePathFromMessage(message);

                for (int i = 0; i < cyclePath.size() - 1; i++) {
                    String from = cyclePath.get(i);
                    String to = cyclePath.get(i + 1);
                    String key = from + "->" + to;
                    String reverseKey = to + "->" + from;

                    // Avoid duplicate relationships
                    if (!addedRelationships.contains(key) && !addedRelationships.contains(reverseKey)) {
                        relationships.add(Relationship.cycle(from, to, "references"));
                        addedRelationships.add(key);
                    }
                }
            }
        }

        // 2. Add composition relationships from CompositionIndex (cross-package support)
        if (compositionIndex != null) {
            compositionIndex.graph().all().forEach(rel -> {
                String from = rel.source().simpleName();
                String to = rel.target().simpleName();

                // Skip self-referencing relationships (e.g., Order emits Order)
                if (from.equals(to)) {
                    return;
                }

                // Skip MapStruct-generated implementations (noise in architectural view)
                if (rel.type() == RelationType.IMPLEMENTS
                        && rel.source().simpleName().endsWith("MapperImpl")) {
                    return;
                }

                String key = from + "->" + to;

                // Skip if already added (cycles take priority)
                if (addedRelationships.contains(key)) {
                    return;
                }

                // Map RelationType to relationship type string
                String type = mapRelationTypeToString(rel.type());
                if (type != null) {
                    relationships.add(Relationship.of(from, to, type));
                    addedRelationships.add(key);
                }
            });
        }

        return relationships;
    }

    /**
     * Maps a RelationType to a string for diagram rendering.
     *
     * @param relationType the relation type from the model
     * @return the string representation for diagrams, or null if not mappable
     * @since 5.0.0
     */
    private String mapRelationTypeToString(RelationType relationType) {
        return switch (relationType) {
            case OWNS -> "owns";
            case CONTAINS -> "contains";
            case REFERENCES -> "references";
            case DEPENDS_ON -> "uses";
            case PERSISTS -> "persists-via";
            case EMITS -> "emits";
            case IMPLEMENTS -> "implements";
            case EXTENDS -> "extends";
            case EXPOSES -> "exposes";
            case ADAPTS -> "adapts";
            case HANDLES -> "handles";
        };
    }

    /**
     * Extracts cycle path from violation message.
     * Expected format: "Circular dependency between aggregates: A -> B -> A"
     */
    private List<String> extractCyclePathFromMessage(String message) {
        List<String> path = new ArrayList<>();
        int colonIndex = message.lastIndexOf(':');
        if (colonIndex >= 0 && colonIndex < message.length() - 1) {
            String cycleInfo = message.substring(colonIndex + 1).trim();
            // Split by " -> "
            String[] parts = cycleInfo.split("\\s*->\\s*");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    path.add(trimmed);
                }
            }
        }
        return path;
    }

    /**
     * Builds type violations for diagram visualization.
     * Extracts violations that affect specific types for visual highlighting.
     *
     * <p>Supports all 11 violation types:
     * <ul>
     *   <li>DDD: MUTABLE_VALUE_OBJECT, IMPURE_DOMAIN, BOUNDARY_VIOLATION, MISSING_IDENTITY,
     *       MISSING_REPOSITORY, EVENT_NAMING</li>
     *   <li>Hexagonal: PORT_UNCOVERED, DEPENDENCY_INVERSION, LAYER_VIOLATION, PORT_NOT_INTERFACE</li>
     * </ul>
     *
     * @since 5.0.0
     */
    private List<TypeViolation> buildTypeViolations(List<Violation> violations) {
        List<TypeViolation> typeViolations = new ArrayList<>();
        Set<String> addedTypes = new HashSet<>();

        for (Violation violation : violations) {
            String constraintId = violation.constraintId().value();

            // Process each affected type
            for (String qualifiedName : violation.affectedTypes()) {
                String typeName = extractSimpleName(qualifiedName);
                if (typeName.isEmpty()) {
                    continue;
                }

                // Skip if already added for this type with same violation type
                String key = typeName + ":" + constraintId;
                if (addedTypes.contains(key)) {
                    continue;
                }

                TypeViolation typeViolation = mapConstraintToTypeViolation(constraintId, typeName);
                if (typeViolation != null) {
                    typeViolations.add(typeViolation);
                    addedTypes.add(key);
                }
            }
        }

        return typeViolations;
    }

    /**
     * Maps a constraint ID to a TypeViolation.
     *
     * <p>Supports all 11 constraint IDs for diagram visualization:
     * <ul>
     *   <li>DDD constraints: value-object-immutable, domain-purity, aggregate-boundary,
     *       entity-identity, aggregate-repository, event-naming</li>
     *   <li>Hexagonal constraints: port-coverage, dependency-inversion, layer-isolation,
     *       port-interface</li>
     * </ul>
     *
     * @param constraintId the constraint ID from the violation
     * @param typeName the simple name of the affected type
     * @return a TypeViolation or null if the constraint is not visualized
     * @since 5.0.0
     */
    private static TypeViolation mapConstraintToTypeViolation(String constraintId, String typeName) {
        return switch (constraintId) {
            // DDD violations (existing)
            case "ddd:value-object-immutable" -> TypeViolation.mutableValueObject(typeName);
            case "ddd:domain-purity" -> TypeViolation.impureDomain(typeName);
            case "ddd:aggregate-boundary" -> TypeViolation.boundaryViolation(typeName);
            // DDD violations (new)
            case "ddd:entity-identity" -> TypeViolation.missingIdentity(typeName);
            case "ddd:aggregate-repository" -> TypeViolation.missingRepository(typeName);
            case "ddd:event-naming" -> TypeViolation.eventNaming(typeName);
            // Hexagonal violations (new)
            case "hexagonal:port-coverage" -> TypeViolation.portUncovered(typeName);
            case "hexagonal:dependency-inversion" -> TypeViolation.dependencyInversion(typeName);
            case "hexagonal:layer-isolation" -> TypeViolation.layerViolation(typeName);
            case "hexagonal:port-interface" -> TypeViolation.portNotInterface(typeName);
            // aggregate-cycle is handled separately in buildRelationships
            default -> null;
        };
    }

    /**
     * Builds the module topology from the architectural model.
     *
     * <p>Returns {@code null} if the model does not contain a {@link ModuleIndex}
     * (mono-module mode).
     *
     * @param model the architectural model
     * @return the module topology, or null if mono-module
     * @since 5.0.0
     */
    private ModuleTopology buildModuleTopology(ArchitecturalModel model) {
        if (model.moduleIndex().isEmpty()) {
            return null;
        }
        ModuleIndex moduleIndex = model.moduleIndex().get();

        List<ModuleTopology.ModuleInfo> moduleInfos = moduleIndex
                .modules()
                .sorted(Comparator.comparing(ModuleDescriptor::moduleId))
                .map(descriptor -> {
                    int typeCount = (int)
                            moduleIndex.typesInModule(descriptor.moduleId()).count();
                    List<String> packages = moduleIndex
                            .typesInModule(descriptor.moduleId())
                            .map(typeId -> {
                                String qn = typeId.qualifiedName();
                                int lastDot = qn.lastIndexOf('.');
                                return lastDot > 0 ? qn.substring(0, lastDot) : "";
                            })
                            .filter(pkg -> !pkg.isEmpty())
                            .distinct()
                            .sorted()
                            .toList();
                    return new ModuleTopology.ModuleInfo(
                            descriptor.moduleId(), descriptor.role().name(), typeCount, packages);
                })
                .toList();

        String summary = String.format("%d modules detected", moduleInfos.size());
        return new ModuleTopology(moduleInfos, summary);
    }

    /**
     * Extracts simple name from qualified name.
     */
    private String extractSimpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return "";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    private IssuesSummary buildIssues(List<Violation> violations) {
        if (violations.isEmpty()) {
            return IssuesSummary.empty();
        }

        // Enrich violations
        List<IssueEntry> entries =
                violations.stream().map(issueEnricher::enrich).toList();

        // Group by theme
        Map<String, List<IssueEntry>> byTheme = entries.stream().collect(Collectors.groupingBy(this::determineTheme));

        List<IssueGroup> groups = new ArrayList<>();
        for (var entry : byTheme.entrySet()) {
            String theme = entry.getKey();
            List<IssueEntry> themeViolations = entry.getValue();
            groups.add(
                    IssueGroup.of(themeId(theme), theme, themeIcon(theme), themeDescription(theme), themeViolations));
        }

        // Sort groups by severity (most severe first), then by theme name for deterministic order
        groups.sort(Comparator.comparingInt((IssueGroup g) -> maxSeverity(g.violations()))
                .thenComparing(IssueGroup::theme));

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
        return entries.stream().mapToInt(e -> e.severity().ordinal()).min().orElse(Integer.MAX_VALUE);
    }

    private RemediationPlan buildRemediation(List<Violation> violations, IssuesSummary issues) {
        if (violations.isEmpty()) {
            return RemediationPlan.empty();
        }

        List<RemediationAction> actions = new ArrayList<>();
        double totalDays = 0;
        double hexaglueSavingsDays = 0;

        // Group issues by corrective action
        Map<String, List<IssueEntry>> byCorrection = issues.allIssues().stream()
                .collect(Collectors.groupingBy(e -> e.suggestion().action()));

        int priority = 1;
        for (var entry : byCorrection.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<IssueEntry>>>comparingInt(e -> maxSeverity(e.getValue()))
                        .thenComparing(Map.Entry::getKey))
                .toList()) {

            List<IssueEntry> relatedIssues = entry.getValue();
            IssueEntry first = relatedIssues.get(0);

            double effortPerInstance =
                    parseEffort(first.suggestion().effortOpt().orElse("1 day"));
            double effortDays = effortPerInstance * relatedIssues.size();
            totalDays += effortDays;

            // Track HexaGlue savings for automatable actions
            if (first.suggestion().isAutomatableByHexaglue()) {
                hexaglueSavingsDays += effortDays;
            }

            List<String> affectedTypes = relatedIssues.stream()
                    .map(e -> e.location().type())
                    .distinct()
                    .toList();

            List<String> issueIds = relatedIssues.stream().map(IssueEntry::id).toList();

            actions.add(RemediationAction.builder()
                    .priority(priority++)
                    .severity(first.severity())
                    .title(entry.getKey())
                    .description(first.impact())
                    .effort(effortDays, "Estimated effort")
                    .impact("Resolves " + relatedIssues.size() + " issue(s)")
                    .affectedTypes(affectedTypes)
                    .relatedIssues(issueIds)
                    .hexagluePlugin(first.suggestion().hexagluePluginOpt().orElse(null))
                    .build());
        }

        String summary;
        if (hexaglueSavingsDays > 0) {
            double effectiveDays = totalDays - hexaglueSavingsDays;
            summary = String.format(
                    "%d action%s required. Manual effort: %.1f days. With HexaGlue: %.1f days (saves %.1f days).",
                    actions.size(), actions.size() != 1 ? "s" : "", totalDays, effectiveDays, hexaglueSavingsDays);
        } else {
            summary = String.format(
                    "%d action%s required to achieve compliance. Estimated total effort: %.1f days.",
                    actions.size(), actions.size() != 1 ? "s" : "", totalDays);
        }

        TotalEffort totalEffort =
                TotalEffort.withSavings(totalDays, hexaglueSavingsDays, DEFAULT_DAILY_RATE, DEFAULT_CURRENCY);

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

    private Appendix buildAppendix(
            HealthScore healthScore, AuditResult auditResult, ArchitectureQuery architectureQuery) {
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
                            m.threshold()
                                    .map(t -> new MetricEntry.MetricThreshold(
                                            t.min().orElse(null), t.max().orElse(null)))
                                    .orElse(null),
                            m.exceedsThreshold() ? KpiStatus.CRITICAL : KpiStatus.OK);
                })
                .toList();

        // Constraints evaluated
        Map<String, Long> constraintViolations = auditResult.violations().stream()
                .collect(Collectors.groupingBy(v -> v.constraintId().value(), Collectors.counting()));

        List<ConstraintResult> constraintsEvaluated = constraintViolations.entrySet().stream()
                .map(e -> new ConstraintResult(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ConstraintResult::id))
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

    /**
     * Extracts the set of packages containing classified types from the model.
     *
     * <p>Used to filter coupling and dependency metrics to only consider
     * packages in the architectural scope (domain, ports, application services).
     *
     * @param model the architectural model
     * @return set of package names containing at least one classified type
     * @since 5.0.0
     */
    private Set<String> extractClassifiedPackages(ArchitecturalModel model) {
        return model.typeRegistry()
                .map(registry -> {
                    Set<String> packages = new HashSet<>();
                    registry.all().forEach(type -> {
                        String pkg = extractPackage(type.id().qualifiedName());
                        if (!pkg.isEmpty()) {
                            packages.add(pkg);
                        }
                    });
                    return packages;
                })
                .orElse(Set.of());
    }

    /**
     * Extracts field details from a type structure for diagram rendering.
     *
     * @param structure the type structure
     * @return list of field details
     * @since 5.0.0
     */
    private List<FieldDetail> extractFieldDetails(TypeStructure structure) {
        if (structure == null) {
            return List.of();
        }
        return structure.fields().stream().map(this::toFieldDetail).toList();
    }

    /**
     * Converts an arch model Field to a FieldDetail for diagram rendering.
     *
     * @param field the field from type structure
     * @return the field detail
     * @since 5.0.0
     */
    private FieldDetail toFieldDetail(Field field) {
        String typeMermaid = MermaidTypeConverter.convert(field.type());
        String visibility = MermaidTypeConverter.visibilitySymbol(field.modifiers());
        boolean isStatic = MermaidTypeConverter.isStatic(field.modifiers());
        return FieldDetail.of(field.name(), typeMermaid, visibility, isStatic);
    }

    /**
     * Extracts method details from a type structure for diagram rendering.
     *
     * <p>Filters out:
     * <ul>
     *   <li>Getters (isGetter())</li>
     *   <li>Setters (isSetter())</li>
     *   <li>Object methods (equals, hashCode, toString)</li>
     *   <li>Factory methods (typically static of/from methods)</li>
     *   <li>Non-public methods</li>
     * </ul>
     *
     * @param structure the type structure
     * @return list of method details for public business methods
     * @since 5.0.0
     */
    private List<MethodDetail> extractMethodDetails(TypeStructure structure) {
        if (structure == null) {
            return List.of();
        }
        return structure.methods().stream()
                .filter(this::isPublicBusinessMethod)
                .map(this::toMethodDetail)
                .toList();
    }

    /**
     * Determines if a method is a public business method suitable for diagram display.
     *
     * <p>Excludes:
     * <ul>
     *   <li>Non-public methods</li>
     *   <li>Getters and setters</li>
     *   <li>Object methods (equals, hashCode, toString)</li>
     *   <li>Factory methods (static of/from/create methods)</li>
     * </ul>
     *
     * @param method the method to check
     * @return true if this is a business method to display
     * @since 5.0.0
     */
    private boolean isPublicBusinessMethod(Method method) {
        // Must be public
        if (!method.isPublic()) {
            return false;
        }
        // Exclude getters, setters, object methods
        if (method.isGetter() || method.isSetter() || method.isObjectMethod()) {
            return false;
        }
        // Exclude factory methods
        if (method.hasRole(MethodRole.FACTORY)) {
            return false;
        }
        return true;
    }

    /**
     * Converts an arch model Method to a MethodDetail for diagram rendering.
     *
     * @param method the method from type structure
     * @return the method detail
     * @since 5.0.0
     */
    private MethodDetail toMethodDetail(Method method) {
        // Build signature: (ParamType1, ParamType2): ReturnType
        StringBuilder signature = new StringBuilder("(");
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) {
                signature.append(", ");
            }
            signature.append(
                    MermaidTypeConverter.convert(method.parameters().get(i).type()));
        }
        signature.append("): ");
        signature.append(MermaidTypeConverter.convert(method.returnType()));

        String visibility = MermaidTypeConverter.visibilitySymbol(method.modifiers());
        boolean isStatic = MermaidTypeConverter.isStatic(method.modifiers());

        return MethodDetail.of(method.name(), signature.toString(), visibility, isStatic);
    }
}
