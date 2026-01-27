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

package io.hexaglue.plugin.audit.adapter.diagram;

import io.hexaglue.arch.ArchitecturalModel;
import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivenPort;
import io.hexaglue.arch.model.DrivenPortType;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Entity;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.arch.model.index.PortIndex;
import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.arch.model.audit.BoundedContextInfo;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds C4 Model diagrams using Mermaid syntax.
 *
 * <p>The C4 model provides four levels of abstraction:
 * <ul>
 *   <li>Level 1: System Context - The system in context with external actors</li>
 *   <li>Level 2: Container - High-level technical building blocks (bounded contexts)</li>
 *   <li>Level 3: Component - Components within a container (aggregates, ports)</li>
 *   <li>Level 4: Code - Class-level detail (not generated here)</li>
 * </ul>
 *
 * <p>This builder uses Mermaid flowchart syntax to approximate C4 diagrams,
 * as Mermaid doesn't have native C4 support but can render similar structures.
 *
 * @since 1.0.0
 * @since 4.0.0 - Migrated from IrSnapshot to ArchitecturalModel
 * @since 4.1.0 - Uses registry() instead of deprecated convenience methods
 */
public class C4DiagramBuilder {

    private static final String SYSTEM_STYLE = "fill:#1168bd,stroke:#0b4884,color:#ffffff";
    private static final String PERSON_STYLE = "fill:#08427b,stroke:#052e56,color:#ffffff";
    private static final String CONTAINER_STYLE = "fill:#438dd5,stroke:#2e6295,color:#ffffff";
    private static final String EXTERNAL_STYLE = "fill:#999999,stroke:#666666,color:#ffffff";

    /**
     * Generates a C4 System Context diagram showing the application
     * and its bounded contexts at a high level.
     *
     * @param projectName the name of the project/system
     * @param boundedContexts the bounded context statistics
     * @return Mermaid diagram code
     */
    public String buildSystemContextDiagram(String projectName, List<BoundedContextStats> boundedContexts) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart TB\n");

        // System boundary
        diagram.append("    subgraph SYSTEM[\"<b>")
                .append(projectName)
                .append("</b><br/>Domain-Driven Application\"]\n");
        diagram.append("        direction TB\n");

        // Bounded contexts as internal components
        if (!boundedContexts.isEmpty()) {
            for (BoundedContextStats bc : boundedContexts) {
                String bcId = sanitizeId(bc.name());
                String stats = String.format("%d Aggregates, %d Entities", bc.aggregates(), bc.entities());
                diagram.append("        ")
                        .append(bcId)
                        .append("[\"<b>")
                        .append(bc.name())
                        .append("</b><br/>Bounded Context<br/><small>")
                        .append(stats)
                        .append("</small>\"]\n");
            }
        } else {
            diagram.append("        CORE[\"<b>Domain Core</b><br/>No bounded contexts detected\"]\n");
        }

        diagram.append("    end\n\n");

        // External actors (generic placeholders)
        diagram.append("    USER((\"<b>User</b><br/>Primary Actor\"))\n");
        diagram.append("    EXTERNAL[(\"<b>External Systems</b><br/>APIs, Databases\")]\n\n");

        // Relationships
        diagram.append("    USER --> SYSTEM\n");
        diagram.append("    SYSTEM --> EXTERNAL\n\n");

        // Styles
        diagram.append("    style SYSTEM ").append(SYSTEM_STYLE).append("\n");
        diagram.append("    style USER ").append(PERSON_STYLE).append("\n");
        diagram.append("    style EXTERNAL ").append(EXTERNAL_STYLE).append("\n");
        for (BoundedContextStats bc : boundedContexts) {
            diagram.append("    style ")
                    .append(sanitizeId(bc.name()))
                    .append(" ")
                    .append(CONTAINER_STYLE)
                    .append("\n");
        }

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Generates a C4 Container diagram showing bounded contexts and their ports.
     *
     * @param projectName the name of the project
     * @param model the architectural model
     * @param architectureQuery the architecture query for bounded context info
     * @return Mermaid diagram code
     */
    public String buildContainerDiagram(
            String projectName, ArchitecturalModel model, ArchitectureQuery architectureQuery) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart LR\n");

        List<BoundedContextInfo> contexts =
                architectureQuery != null ? architectureQuery.findBoundedContexts() : List.of();

        // Collect ports from model using port index
        PortIndex portIndex = model.portIndex().orElseThrow();
        List<DrivingPort> drivingPorts = portIndex.drivingPorts().toList();
        List<DrivenPort> drivenPorts = portIndex.drivenPorts().toList();

        // Driving adapters (left side)
        if (!drivingPorts.isEmpty()) {
            diagram.append("    subgraph DRIVING[\"<b>Driving Adapters</b><br/>Primary/Inbound\"]\n");
            diagram.append("        direction TB\n");
            for (DrivingPort port : drivingPorts.stream().limit(5).toList()) {
                String portId = sanitizeId(port.id().simpleName());
                diagram.append("        ")
                        .append(portId)
                        .append("_D[\"")
                        .append(port.id().simpleName())
                        .append("\"]\n");
            }
            if (drivingPorts.size() > 5) {
                diagram.append("        MORE_D[\"... +")
                        .append(drivingPorts.size() - 5)
                        .append(" more\"]\n");
            }
            diagram.append("    end\n\n");
        }

        // Domain core (center)
        diagram.append("    subgraph CORE[\"<b>").append(projectName).append("</b><br/>Domain Core\"]\n");
        diagram.append("        direction TB\n");
        if (!contexts.isEmpty()) {
            for (BoundedContextInfo bc : contexts) {
                String bcId = sanitizeId(bc.name());
                diagram.append("        ")
                        .append(bcId)
                        .append("[\"")
                        .append(capitalize(bc.name()))
                        .append("\"]\n");
            }
        } else {
            diagram.append("        DOMAIN[\"Domain Model\"]\n");
        }
        diagram.append("    end\n\n");

        // Driven adapters (right side)
        if (!drivenPorts.isEmpty()) {
            diagram.append("    subgraph DRIVEN[\"<b>Driven Adapters</b><br/>Secondary/Outbound\"]\n");
            diagram.append("        direction TB\n");
            for (DrivenPort port : drivenPorts.stream().limit(5).toList()) {
                String portId = sanitizeId(port.id().simpleName());
                diagram.append("        ")
                        .append(portId)
                        .append("_V[\"")
                        .append(port.id().simpleName())
                        .append("\"]\n");
            }
            if (drivenPorts.size() > 5) {
                diagram.append("        MORE_V[\"... +")
                        .append(drivenPorts.size() - 5)
                        .append(" more\"]\n");
            }
            diagram.append("    end\n\n");
        }

        // Connections
        diagram.append("    DRIVING --> CORE\n");
        diagram.append("    CORE --> DRIVEN\n\n");

        // Styles
        diagram.append("    style CORE ").append(SYSTEM_STYLE).append("\n");
        diagram.append("    style DRIVING ").append(CONTAINER_STYLE).append("\n");
        diagram.append("    style DRIVEN ").append(EXTERNAL_STYLE).append("\n");

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Generates an Aggregate graph showing domain aggregates and their relationships.
     *
     * @param model the architectural model
     * @return Mermaid diagram code
     */
    public String buildAggregateDiagram(ArchitecturalModel model) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart TB\n");

        // Find aggregates using domain index
        DomainIndex domainIndex = model.domainIndex().orElseThrow();
        List<AggregateRoot> aggregates = domainIndex
                .aggregateRoots()
                .sorted(Comparator.comparing(agg -> agg.id().simpleName()))
                .toList();

        if (aggregates.isEmpty()) {
            diagram.append("    NONE[\"No Aggregate Roots detected\"]\n");
            diagram.append("```\n");
            return diagram.toString();
        }

        // Build aggregate nodes with their entities
        Map<String, List<Object>> aggregateChildren = findAggregateChildren(model);

        for (AggregateRoot aggregate : aggregates) {
            String aggId = sanitizeId(aggregate.id().simpleName());
            diagram.append("    subgraph ")
                    .append(aggId)
                    .append("_AGG[\"<b>")
                    .append(aggregate.id().simpleName())
                    .append("</b><br/>Aggregate Root\"]\n");

            // Add root
            diagram.append("        ")
                    .append(aggId)
                    .append("((")
                    .append(aggregate.id().simpleName())
                    .append("))\n");

            // Add child entities
            List<Object> children =
                    aggregateChildren.getOrDefault(aggregate.id().qualifiedName(), List.of());
            for (Object child : children.stream().limit(4).toList()) {
                String childName;
                String shape;
                if (child instanceof Entity entity) {
                    childName = entity.id().simpleName();
                    shape = "[" + childName + "]";
                } else if (child instanceof ValueObject vo) {
                    childName = vo.id().simpleName();
                    shape = "(" + childName + ")";
                } else {
                    continue;
                }
                String childId = sanitizeId(childName);
                diagram.append("        ").append(childId).append(shape).append("\n");
                diagram.append("        ")
                        .append(aggId)
                        .append(" --> ")
                        .append(childId)
                        .append("\n");
            }
            if (children.size() > 4) {
                diagram.append("        MORE_")
                        .append(aggId)
                        .append("[\"... +")
                        .append(children.size() - 4)
                        .append(" more\"]\n");
            }

            diagram.append("    end\n\n");
        }

        // Styles
        diagram.append("\n");
        for (AggregateRoot aggregate : aggregates) {
            diagram.append("    style ")
                    .append(sanitizeId(aggregate.id().simpleName()))
                    .append(" fill:#ff9800,stroke:#e65100\n");
        }

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Generates a Port Matrix diagram showing driving and driven ports.
     *
     * @param model the architectural model
     * @return Mermaid diagram code
     */
    public String buildPortMatrixDiagram(ArchitecturalModel model) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart LR\n");

        PortIndex portIndex = model.portIndex().orElseThrow();
        List<DrivingPort> drivingPorts = portIndex.drivingPorts().toList();
        List<DrivenPort> drivenPorts = portIndex.drivenPorts().toList();

        // Driving ports
        diagram.append("    subgraph DRIVING[\"🔵 DRIVING PORTS<br/>(Primary/Inbound)\"]\n");
        diagram.append("        direction TB\n");
        for (DrivingPort port : drivingPorts) {
            String id = sanitizeId(port.id().simpleName()) + "_IN";
            diagram.append("        ")
                    .append(id)
                    .append("[\"🔌 ")
                    .append(port.id().simpleName())
                    .append("\"]\n");
        }
        if (drivingPorts.isEmpty()) {
            diagram.append("        NONE_IN[\"No driving ports\"]\n");
        }
        diagram.append("    end\n\n");

        // Domain core
        diagram.append("    CORE((\"🎯 Domain Core\"))\n\n");

        // Driven ports
        diagram.append("    subgraph DRIVEN[\"🟠 DRIVEN PORTS<br/>(Secondary/Outbound)\"]\n");
        diagram.append("        direction TB\n");
        for (DrivenPort port : drivenPorts) {
            String id = sanitizeId(port.id().simpleName()) + "_OUT";
            String icon = port.portType() == DrivenPortType.REPOSITORY ? "📦" : "🔌";
            diagram.append("        ")
                    .append(id)
                    .append("[\"")
                    .append(icon)
                    .append(" ")
                    .append(port.id().simpleName())
                    .append("\"]\n");
        }
        if (drivenPorts.isEmpty()) {
            diagram.append("        NONE_OUT[\"No driven ports\"]\n");
        }
        diagram.append("    end\n\n");

        // Connections
        diagram.append("    DRIVING --> CORE\n");
        diagram.append("    CORE --> DRIVEN\n\n");

        // Styles
        diagram.append("    style CORE fill:#4caf50,stroke:#2e7d32,color:#fff\n");
        diagram.append("    style DRIVING fill:#2196f3,stroke:#1565c0,color:#fff\n");
        diagram.append("    style DRIVEN fill:#ff9800,stroke:#e65100,color:#fff\n");

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Finds child entities and value objects for each aggregate.
     */
    private Map<String, List<Object>> findAggregateChildren(ArchitecturalModel model) {
        Map<String, List<Object>> result = new HashMap<>();

        DomainIndex domainIndex = model.domainIndex().orElseThrow();
        List<AggregateRoot> aggregates = domainIndex.aggregateRoots().toList();
        List<Entity> entities = domainIndex.entities().toList();
        List<ValueObject> valueObjects = domainIndex.valueObjects().toList();

        // Simple heuristic: entities/VOs in same package as aggregate are children
        for (AggregateRoot aggregate : aggregates) {
            String aggPackage = extractPackage(aggregate.id().qualifiedName());
            List<Object> children = Stream.concat(
                            entities.stream()
                                    .filter(e -> extractPackage(e.id().qualifiedName())
                                                    .equals(aggPackage)
                                            || extractPackage(e.id().qualifiedName())
                                                    .startsWith(aggPackage + ".")),
                            valueObjects.stream()
                                    .filter(vo -> extractPackage(vo.id().qualifiedName())
                                                    .equals(aggPackage)
                                            || extractPackage(vo.id().qualifiedName())
                                                    .startsWith(aggPackage + ".")))
                    .collect(Collectors.toList());
            result.put(aggregate.id().qualifiedName(), children);
        }

        return result;
    }

    /**
     * Extracts the package name from a qualified name.
     */
    private String extractPackage(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Sanitizes a string to be a valid Mermaid node ID.
     */
    private String sanitizeId(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Capitalizes the first letter.
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
