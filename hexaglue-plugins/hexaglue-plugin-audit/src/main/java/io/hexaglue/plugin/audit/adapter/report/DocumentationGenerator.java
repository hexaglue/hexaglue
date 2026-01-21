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

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.DomainEvent;
import io.hexaglue.arch.model.DomainService;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.audit.adapter.diagram.C4DiagramBuilder;
import io.hexaglue.plugin.audit.adapter.report.model.AuditReport;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Generates separate architectural documentation files from an audit report.
 *
 * <p>This generator produces multiple focused Markdown files for different aspects
 * of the architecture, providing better organization and navigation than a single
 * monolithic report. Files generated include:
 * <ul>
 *   <li>{@code ARCHITECTURE-OVERVIEW.md} - C4 Context view and system summary</li>
 *   <li>{@code DOMAIN-MODEL.md} - Aggregates, Entities, Value Objects</li>
 *   <li>{@code PORTS-AND-ADAPTERS.md} - Hexagonal architecture port matrix</li>
 *   <li>{@code BOUNDED-CONTEXTS.md} - Bounded context statistics</li>
 *   <li>{@code METRICS.md} - Coupling, cohesion, and other metrics</li>
 *   <li>{@code diagrams/} - Standalone Mermaid diagram files</li>
 * </ul>
 *
 * @since 3.0.0
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 */
public final class DocumentationGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final C4DiagramBuilder c4Builder;

    public DocumentationGenerator() {
        this.c4Builder = new C4DiagramBuilder();
    }

    /**
     * Generates all documentation files in the specified output directory.
     *
     * @param report    the audit report containing all analysis data
     * @param outputDir the directory where documentation files will be written
     * @throws IOException if file writing fails
     */
    public void generateAll(AuditReport report, Path outputDir) throws IOException {
        Objects.requireNonNull(report, "report required");
        Objects.requireNonNull(outputDir, "outputDir required");

        Files.createDirectories(outputDir);

        generateArchitectureOverview(report, outputDir);
        generateDomainModel(report, outputDir);
        generatePortsAndAdapters(report, outputDir);
        generateBoundedContexts(report, outputDir);
        generateMetrics(report, outputDir);
        generateDiagramFiles(report, outputDir);
    }

    /**
     * Generates ARCHITECTURE-OVERVIEW.md with C4 Context view and system summary.
     */
    public void generateArchitectureOverview(AuditReport report, Path outputDir) throws IOException {
        StringBuilder md = new StringBuilder();

        String projectName = report.metadata().projectName();
        String timestamp =
                TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault()));

        md.append("# Architecture Overview: ").append(projectName).append("\n\n");
        md.append("*Generated: ").append(timestamp).append("*\n\n");
        md.append("---\n\n");

        // Executive Summary
        md.append("## Executive Summary\n\n");
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append("| **Health Score** | ")
                .append(report.healthScore().overall())
                .append("/100 (")
                .append(report.healthScore().grade())
                .append(") |\n");
        md.append("| **DDD Compliance** | ")
                .append(report.dddCompliancePercent())
                .append("% |\n");
        md.append("| **Hexagonal Compliance** | ")
                .append(report.hexCompliancePercent())
                .append("% |\n");
        md.append("| **Architecture Style** | ").append(report.detectedStyle()).append(" |\n\n");

        // C4 System Context Diagram
        md.append("## System Context (C4 Level 1)\n\n");
        md.append(c4Builder.buildSystemContextDiagram(
                projectName, report.inventory().boundedContexts()));
        md.append("\n");

        // C4 Container Diagram
        if (report.model() != null) {
            md.append("## Container View (C4 Level 2)\n\n");
            md.append(c4Builder.buildContainerDiagram(projectName, report.model(), report.architectureQuery()));
            md.append("\n");
        }

        // Component Inventory Summary
        md.append("## Component Inventory\n\n");
        md.append("| Category | Count |\n");
        md.append("|----------|:-----:|\n");
        md.append("| Bounded Contexts | ")
                .append(report.inventory().boundedContexts().size())
                .append(" |\n");
        md.append("| Aggregate Roots | ")
                .append(report.inventory().aggregateRoots())
                .append(" |\n");
        md.append("| Entities | ").append(report.inventory().entities()).append(" |\n");
        md.append("| Value Objects | ")
                .append(report.inventory().valueObjects())
                .append(" |\n");
        md.append("| Domain Services | ")
                .append(report.inventory().domainServices())
                .append(" |\n");
        md.append("| Domain Events | ")
                .append(report.inventory().domainEvents())
                .append(" |\n");
        md.append("| Driving Ports | ")
                .append(report.inventory().drivingPorts())
                .append(" |\n");
        md.append("| Driven Ports | ").append(report.inventory().drivenPorts()).append(" |\n\n");

        // Related Documents
        md.append("## Related Documentation\n\n");
        md.append("- [Domain Model](DOMAIN-MODEL.md) - Detailed aggregate and entity documentation\n");
        md.append("- [Ports & Adapters](PORTS-AND-ADAPTERS.md) - Hexagonal architecture port matrix\n");
        md.append("- [Bounded Contexts](BOUNDED-CONTEXTS.md) - Context mapping and statistics\n");
        md.append("- [Metrics](METRICS.md) - Code quality and architecture metrics\n\n");

        md.append("---\n*Generated by HexaGlue Audit Plugin v")
                .append(report.metadata().hexaglueVersion())
                .append("*\n");

        writeFile(outputDir.resolve("ARCHITECTURE-OVERVIEW.md"), md.toString());
    }

    /**
     * Generates DOMAIN-MODEL.md with aggregate, entity, and value object documentation.
     */
    public void generateDomainModel(AuditReport report, Path outputDir) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# Domain Model: ").append(report.metadata().projectName()).append("\n\n");
        md.append("*Generated: ")
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("*\n\n");
        md.append("---\n\n");

        // Aggregate Graph
        if (report.model() != null) {
            md.append("## Aggregate Relationship Graph\n\n");
            md.append(c4Builder.buildAggregateDiagram(report.model()));
            md.append("\n");
        }

        // Aggregate Details
        if (!report.aggregateDetails().isEmpty()) {
            md.append("## Aggregate Roots\n\n");
            md.append("| Aggregate | Entities | Value Objects | Repository | Cohesion | Status |\n");
            md.append("|-----------|:--------:|:-------------:|:----------:|:--------:|:------:|\n");

            for (var agg : report.aggregateDetails()) {
                String repoStatus = agg.hasRepository() ? "Yes" : "No";
                String cohesionStr = agg.cohesion() >= 0 ? String.format("%.2f", agg.cohesion()) : "-";
                String status =
                        switch (agg.status()) {
                            case OK -> "OK";
                            case WARNING -> "Warning";
                            case PROBLEM -> "Problem";
                        };

                md.append("| `")
                        .append(agg.rootName())
                        .append("` | ")
                        .append(agg.entityCount())
                        .append(" | ")
                        .append(agg.valueObjectCount())
                        .append(" | ")
                        .append(repoStatus)
                        .append(" | ")
                        .append(cohesionStr)
                        .append(" | ")
                        .append(status)
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Domain Types by Kind (from IrSnapshot)
        if (report.model() != null) {
            appendDomainTypesByKind(md, report.model());
        }

        md.append("---\n*Generated by HexaGlue Audit Plugin v")
                .append(report.metadata().hexaglueVersion())
                .append("*\n");

        writeFile(outputDir.resolve("DOMAIN-MODEL.md"), md.toString());
    }

    private void appendDomainTypesByKind(StringBuilder md, ArchitecturalModel model) {
        DomainIndex domainIndex = model.domainIndex().orElseThrow();
        // Entities
        List<Entity> entities = domainIndex.entities()
                .sorted(Comparator.comparing(e -> e.id().simpleName()))
                .toList();
        if (!entities.isEmpty()) {
            md.append("## Entities\n\n");
            md.append("| Entity | Package |\n");
            md.append("|--------|--------|\n");
            for (Entity entity : entities) {
                md.append("| `")
                        .append(entity.id().simpleName())
                        .append("` | ")
                        .append(extractPackage(entity.id().qualifiedName()))
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Value Objects
        List<ValueObject> valueObjects = domainIndex.valueObjects()
                .sorted(Comparator.comparing(vo -> vo.id().simpleName()))
                .toList();
        if (!valueObjects.isEmpty()) {
            md.append("## Value Objects\n\n");
            md.append("| Value Object | Package |\n");
            md.append("|--------------|--------|\n");
            for (ValueObject vo : valueObjects) {
                md.append("| `")
                        .append(vo.id().simpleName())
                        .append("` | ")
                        .append(extractPackage(vo.id().qualifiedName()))
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Domain Events
        List<DomainEvent> events = domainIndex.domainEvents()
                .sorted(Comparator.comparing(ev -> ev.id().simpleName()))
                .toList();
        if (!events.isEmpty()) {
            md.append("## Domain Events\n\n");
            md.append("| Event | Package |\n");
            md.append("|-------|--------|\n");
            for (DomainEvent event : events) {
                md.append("| `")
                        .append(event.id().simpleName())
                        .append("` | ")
                        .append(extractPackage(event.id().qualifiedName()))
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Domain Services
        List<DomainService> services = domainIndex.domainServices()
                .sorted(Comparator.comparing(s -> s.id().simpleName()))
                .toList();
        if (!services.isEmpty()) {
            md.append("## Domain Services\n\n");
            md.append("| Service | Package |\n");
            md.append("|---------|--------|\n");
            for (DomainService service : services) {
                md.append("| `")
                        .append(service.id().simpleName())
                        .append("` | ")
                        .append(extractPackage(service.id().qualifiedName()))
                        .append(" |\n");
            }
            md.append("\n");
        }
    }

    /**
     * Extracts the package name from a qualified name.
     */
    private String extractPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Generates PORTS-AND-ADAPTERS.md with hexagonal architecture port matrix.
     */
    public void generatePortsAndAdapters(AuditReport report, Path outputDir) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# Ports & Adapters: ")
                .append(report.metadata().projectName())
                .append("\n\n");
        md.append("*Generated: ")
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("*\n\n");
        md.append("---\n\n");

        // Port Matrix Diagram
        if (report.model() != null) {
            md.append("## Port Matrix Diagram\n\n");
            md.append(c4Builder.buildPortMatrixDiagram(report.model()));
            md.append("\n");
        }

        // Hexagonal Compliance
        md.append("## Hexagonal Compliance: ")
                .append(report.hexCompliancePercent())
                .append("%\n\n");

        // Driving Ports Section
        md.append("## Driving Ports (Primary/Inbound)\n\n");
        md.append("Driving ports are interfaces that allow external actors to interact with the domain.\n\n");

        List<DrivingPort> drivingPorts = report.model() != null
                ? report.model()
                        .portIndex()
                        .orElseThrow()
                        .drivingPorts()
                        .sorted(Comparator.comparing(p -> p.id().simpleName()))
                        .toList()
                : List.of();

        if (!drivingPorts.isEmpty()) {
            md.append("| Port | Operations | Package |\n");
            md.append("|------|:----------:|--------|\n");
            for (DrivingPort port : drivingPorts) {
                md.append("| `")
                        .append(port.id().simpleName())
                        .append("` | ")
                        .append(port.structure().methods().size())
                        .append(" | ")
                        .append(extractPackage(port.id().qualifiedName()))
                        .append(" |\n");
            }
        } else {
            md.append("*No driving ports detected.*\n");
        }
        md.append("\n");

        // Driven Ports Section
        md.append("## Driven Ports (Secondary/Outbound)\n\n");
        md.append("Driven ports are interfaces that the domain uses to interact with external systems.\n\n");

        List<DrivenPort> drivenPorts = report.model() != null
                ? report.model()
                        .portIndex()
                        .orElseThrow()
                        .drivenPorts()
                        .sorted(Comparator.comparing(p -> p.id().simpleName()))
                        .toList()
                : List.of();

        if (!drivenPorts.isEmpty()) {
            md.append("| Port | Port Type | Managed Type | Operations | Package |\n");
            md.append("|------|-----------|--------------|:----------:|--------|\n");
            for (DrivenPort port : drivenPorts) {
                String managedType = port.managedAggregate()
                        .map(ref -> "`" + simplifyType(ref.qualifiedName()) + "`")
                        .orElse("-");
                md.append("| `")
                        .append(port.id().simpleName())
                        .append("` | ")
                        .append(port.portType())
                        .append(" | ")
                        .append(managedType)
                        .append(" | ")
                        .append(port.structure().methods().size())
                        .append(" | ")
                        .append(extractPackage(port.id().qualifiedName()))
                        .append(" |\n");
            }
        } else {
            md.append("*No driven ports detected.*\n");
        }
        md.append("\n");

        // Summary Table
        md.append("## Summary\n\n");
        md.append("| Category | Count |\n");
        md.append("|----------|:-----:|\n");
        md.append("| Driving Ports | ").append(drivingPorts.size()).append(" |\n");
        md.append("| Driven Ports | ").append(drivenPorts.size()).append(" |\n");
        md.append("| **Total Ports** | **")
                .append(drivingPorts.size() + drivenPorts.size())
                .append("** |\n\n");

        md.append("---\n*Generated by HexaGlue Audit Plugin v")
                .append(report.metadata().hexaglueVersion())
                .append("*\n");

        writeFile(outputDir.resolve("PORTS-AND-ADAPTERS.md"), md.toString());
    }

    /**
     * Generates BOUNDED-CONTEXTS.md with bounded context mapping and statistics.
     */
    public void generateBoundedContexts(AuditReport report, Path outputDir) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# Bounded Contexts: ")
                .append(report.metadata().projectName())
                .append("\n\n");
        md.append("*Generated: ")
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("*\n\n");
        md.append("---\n\n");

        ComponentInventory inventory = report.inventory();
        List<BoundedContextStats> contexts = inventory.boundedContexts();

        if (contexts.isEmpty()) {
            md.append("## No Bounded Contexts Detected\n\n");
            md.append("The analysis did not detect any bounded contexts. This may indicate:\n");
            md.append("- A monolithic domain structure\n");
            md.append("- Package structure that doesn't follow bounded context patterns\n");
            md.append("- A small codebase that doesn't require context separation\n\n");
        } else {
            md.append("## Overview\n\n");
            md.append("| Metric | Value |\n");
            md.append("|--------|:-----:|\n");
            md.append("| **Total Bounded Contexts** | ").append(contexts.size()).append(" |\n");
            int totalAgg =
                    contexts.stream().mapToInt(BoundedContextStats::aggregates).sum();
            int totalEnt =
                    contexts.stream().mapToInt(BoundedContextStats::entities).sum();
            int totalVo = contexts.stream()
                    .mapToInt(BoundedContextStats::valueObjects)
                    .sum();
            int totalLoc = contexts.stream()
                    .mapToInt(BoundedContextStats::estimatedLoc)
                    .sum();
            md.append("| Total Aggregates | ").append(totalAgg).append(" |\n");
            md.append("| Total Entities | ").append(totalEnt).append(" |\n");
            md.append("| Total Value Objects | ").append(totalVo).append(" |\n");
            md.append("| Total LOC (estimated) | ")
                    .append(String.format("%,d", totalLoc))
                    .append(" |\n\n");

            // Context Breakdown Table
            md.append("## Context Breakdown\n\n");
            md.append("| Bounded Context | Aggregates | Entities | VOs | Ports | Est. LOC |\n");
            md.append("|-----------------|:----------:|:--------:|:---:|:-----:|---------:|\n");

            for (BoundedContextStats bc : contexts) {
                md.append("| **")
                        .append(bc.name())
                        .append("** | ")
                        .append(bc.aggregates())
                        .append(" | ")
                        .append(bc.entities())
                        .append(" | ")
                        .append(bc.valueObjects())
                        .append(" | ")
                        .append(bc.ports())
                        .append(" | ")
                        .append(String.format("%,d", bc.estimatedLoc()))
                        .append(" |\n");
            }
            md.append("\n");

            // Individual Context Details
            md.append("## Context Details\n\n");
            for (BoundedContextStats bc : contexts) {
                md.append("### ").append(bc.name()).append("\n\n");
                md.append("| Component Type | Count |\n");
                md.append("|---------------|:-----:|\n");
                md.append("| Aggregates | ").append(bc.aggregates()).append(" |\n");
                md.append("| Entities | ").append(bc.entities()).append(" |\n");
                md.append("| Value Objects | ").append(bc.valueObjects()).append(" |\n");
                md.append("| Ports | ").append(bc.ports()).append(" |\n");
                md.append("| Est. LOC | ")
                        .append(String.format("%,d", bc.estimatedLoc()))
                        .append(" |\n\n");
            }
        }

        md.append("---\n*Generated by HexaGlue Audit Plugin v")
                .append(report.metadata().hexaglueVersion())
                .append("*\n");

        writeFile(outputDir.resolve("BOUNDED-CONTEXTS.md"), md.toString());
    }

    /**
     * Generates METRICS.md with coupling, cohesion, and architecture metrics.
     */
    public void generateMetrics(AuditReport report, Path outputDir) throws IOException {
        StringBuilder md = new StringBuilder();

        md.append("# Architecture Metrics: ")
                .append(report.metadata().projectName())
                .append("\n\n");
        md.append("*Generated: ")
                .append(TIMESTAMP_FORMATTER.format(report.metadata().timestamp().atZone(ZoneId.systemDefault())))
                .append("*\n\n");
        md.append("---\n\n");

        // Health Score Breakdown
        md.append("## Health Score: ")
                .append(report.healthScore().overall())
                .append("/100 (")
                .append(report.healthScore().grade())
                .append(")\n\n");

        md.append("| Dimension | Weight | Score | Contribution |\n");
        md.append("|-----------|:------:|:-----:|:------------:|\n");

        double dddContrib = report.healthScore().dddCompliance() * 0.25;
        double hexContrib = report.healthScore().hexCompliance() * 0.25;
        double depContrib = report.healthScore().dependencyQuality() * 0.20;
        double cplContrib = report.healthScore().coupling() * 0.15;
        double cohContrib = report.healthScore().cohesion() * 0.15;

        md.append("| DDD Compliance | 25% | ")
                .append(report.healthScore().dddCompliance())
                .append("/100 | ")
                .append(String.format("%.2f", dddContrib))
                .append(" |\n");
        md.append("| Hexagonal Compliance | 25% | ")
                .append(report.healthScore().hexCompliance())
                .append("/100 | ")
                .append(String.format("%.2f", hexContrib))
                .append(" |\n");
        md.append("| Dependency Quality | 20% | ")
                .append(report.healthScore().dependencyQuality())
                .append("/100 | ")
                .append(String.format("%.2f", depContrib))
                .append(" |\n");
        md.append("| Coupling | 15% | ")
                .append(report.healthScore().coupling())
                .append("/100 | ")
                .append(String.format("%.2f", cplContrib))
                .append(" |\n");
        md.append("| Cohesion | 15% | ")
                .append(report.healthScore().cohesion())
                .append("/100 | ")
                .append(String.format("%.2f", cohContrib))
                .append(" |\n\n");

        // Package Coupling Metrics
        var couplingMetrics = report.architectureAnalysis().couplingMetrics();
        if (!couplingMetrics.isEmpty()) {
            md.append("## Package Coupling Metrics\n\n");
            md.append("| Package | Ca | Ce | I | A | D | Zone |\n");
            md.append("|---------|:--:|:--:|:-:|:-:|:-:|------|\n");

            for (var m : couplingMetrics) {
                String zone = "";
                if (m.isInZoneOfPain()) zone = "Pain";
                if (m.isInZoneOfUselessness()) zone = "Useless";
                md.append("| `")
                        .append(m.packageName())
                        .append("` | ")
                        .append(m.afferentCoupling())
                        .append(" | ")
                        .append(m.efferentCoupling())
                        .append(" | ")
                        .append(String.format("%.2f", m.instability()))
                        .append(" | ")
                        .append(String.format("%.2f", m.abstractness()))
                        .append(" | ")
                        .append(String.format("%.2f", m.distance()))
                        .append(" | ")
                        .append(zone)
                        .append(" |\n");
            }
            md.append(
                    "\n**Legend:** Ca=Afferent Coupling, Ce=Efferent Coupling, I=Instability, A=Abstractness, D=Distance\n\n");
        }

        // Technical Debt
        if (!report.technicalDebt().isZero()) {
            md.append("## Technical Debt\n\n");
            md.append("| Metric | Value |\n");
            md.append("|--------|------:|\n");
            md.append("| **Total Effort** | ")
                    .append(String.format("%.1f", report.technicalDebt().totalDays()))
                    .append(" person-days |\n");
            md.append("| **Estimated Cost** | ")
                    .append(String.format("%.2f EUR", report.technicalDebt().totalCost()))
                    .append(" |\n");
            md.append("| **Monthly Interest** | ")
                    .append(String.format("%.2f EUR", report.technicalDebt().monthlyInterest()))
                    .append(" |\n\n");
        }

        // All collected metrics
        if (!report.metrics().isEmpty()) {
            md.append("## All Metrics\n\n");
            md.append("| Metric | Value | Unit | Threshold | Status |\n");
            md.append("|--------|------:|------|-----------|:------:|\n");
            for (var m : report.metrics()) {
                String thresholdStr =
                        m.threshold() != null ? m.thresholdType() + " " + String.format("%.2f", m.threshold()) : "-";
                md.append("| ")
                        .append(m.name())
                        .append(" | ")
                        .append(String.format("%.2f", m.value()))
                        .append(" | ")
                        .append(m.unit())
                        .append(" | ")
                        .append(thresholdStr)
                        .append(" | ")
                        .append(m.status())
                        .append(" |\n");
            }
            md.append("\n");
        }

        md.append("---\n*Generated by HexaGlue Audit Plugin v")
                .append(report.metadata().hexaglueVersion())
                .append("*\n");

        writeFile(outputDir.resolve("METRICS.md"), md.toString());
    }

    /**
     * Generates standalone Mermaid diagram files in the diagrams/ subdirectory.
     */
    public void generateDiagramFiles(AuditReport report, Path outputDir) throws IOException {
        Path diagramsDir = outputDir.resolve("diagrams");
        Files.createDirectories(diagramsDir);

        String projectName = report.metadata().projectName();

        // System Context Diagram
        String systemContext = c4Builder.buildSystemContextDiagram(
                projectName, report.inventory().boundedContexts());
        writeFile(diagramsDir.resolve("c4-system-context.md"), systemContext);

        // Container Diagram
        if (report.model() != null) {
            String container = c4Builder.buildContainerDiagram(projectName, report.model(), report.architectureQuery());
            writeFile(diagramsDir.resolve("c4-container.md"), container);

            // Aggregate Diagram
            String aggregate = c4Builder.buildAggregateDiagram(report.model());
            writeFile(diagramsDir.resolve("aggregate-graph.md"), aggregate);

            // Port Matrix Diagram
            String portMatrix = c4Builder.buildPortMatrixDiagram(report.model());
            writeFile(diagramsDir.resolve("port-matrix.md"), portMatrix);
        }
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String simplifyType(String qualifiedType) {
        if (qualifiedType == null || qualifiedType.isEmpty()) {
            return "?";
        }
        int lastDot = qualifiedType.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedType.substring(lastDot + 1) : qualifiedType;
    }
}
