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

import io.hexaglue.plugin.audit.adapter.report.model.ComponentInventory.BoundedContextStats;
import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.audit.BoundedContextInfo;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainType;
import io.hexaglue.spi.ir.IrSnapshot;
import io.hexaglue.spi.ir.Port;
import io.hexaglue.spi.ir.PortDirection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        diagram.append("    subgraph SYSTEM[\"<b>").append(projectName).append("</b><br/>Domain-Driven Application\"]\n");
        diagram.append("        direction TB\n");

        // Bounded contexts as internal components
        if (!boundedContexts.isEmpty()) {
            for (BoundedContextStats bc : boundedContexts) {
                String bcId = sanitizeId(bc.name());
                String stats = String.format("%d Aggregates, %d Entities", bc.aggregates(), bc.entities());
                diagram.append("        ").append(bcId).append("[\"<b>").append(bc.name())
                        .append("</b><br/>Bounded Context<br/><small>").append(stats).append("</small>\"]\n");
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
            diagram.append("    style ").append(sanitizeId(bc.name())).append(" ").append(CONTAINER_STYLE).append("\n");
        }

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Generates a C4 Container diagram showing bounded contexts and their ports.
     *
     * @param projectName the name of the project
     * @param ir the IR snapshot
     * @param architectureQuery the architecture query for bounded context info
     * @return Mermaid diagram code
     */
    public String buildContainerDiagram(String projectName, IrSnapshot ir, ArchitectureQuery architectureQuery) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart LR\n");

        List<BoundedContextInfo> contexts = architectureQuery != null
                ? architectureQuery.findBoundedContexts()
                : List.of();

        // Group ports by direction
        Map<PortDirection, List<Port>> portsByDirection = ir.ports().ports().stream()
                .collect(Collectors.groupingBy(Port::direction));

        List<Port> drivingPorts = portsByDirection.getOrDefault(PortDirection.DRIVING, List.of());
        List<Port> drivenPorts = portsByDirection.getOrDefault(PortDirection.DRIVEN, List.of());

        // Driving adapters (left side)
        if (!drivingPorts.isEmpty()) {
            diagram.append("    subgraph DRIVING[\"<b>Driving Adapters</b><br/>Primary/Inbound\"]\n");
            diagram.append("        direction TB\n");
            for (Port port : drivingPorts.stream().limit(5).toList()) {
                String portId = sanitizeId(port.simpleName());
                diagram.append("        ").append(portId).append("_D[\"").append(port.simpleName()).append("\"]\n");
            }
            if (drivingPorts.size() > 5) {
                diagram.append("        MORE_D[\"... +").append(drivingPorts.size() - 5).append(" more\"]\n");
            }
            diagram.append("    end\n\n");
        }

        // Domain core (center)
        diagram.append("    subgraph CORE[\"<b>").append(projectName).append("</b><br/>Domain Core\"]\n");
        diagram.append("        direction TB\n");
        if (!contexts.isEmpty()) {
            for (BoundedContextInfo bc : contexts) {
                String bcId = sanitizeId(bc.name());
                diagram.append("        ").append(bcId).append("[\"").append(capitalize(bc.name())).append("\"]\n");
            }
        } else {
            diagram.append("        DOMAIN[\"Domain Model\"]\n");
        }
        diagram.append("    end\n\n");

        // Driven adapters (right side)
        if (!drivenPorts.isEmpty()) {
            diagram.append("    subgraph DRIVEN[\"<b>Driven Adapters</b><br/>Secondary/Outbound\"]\n");
            diagram.append("        direction TB\n");
            for (Port port : drivenPorts.stream().limit(5).toList()) {
                String portId = sanitizeId(port.simpleName());
                diagram.append("        ").append(portId).append("_V[\"").append(port.simpleName()).append("\"]\n");
            }
            if (drivenPorts.size() > 5) {
                diagram.append("        MORE_V[\"... +").append(drivenPorts.size() - 5).append(" more\"]\n");
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
     * @param ir the IR snapshot
     * @return Mermaid diagram code
     */
    public String buildAggregateDiagram(IrSnapshot ir) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart TB\n");

        // Find aggregates
        List<DomainType> aggregates = ir.domain().types().stream()
                .filter(t -> t.kind() == DomainKind.AGGREGATE_ROOT)
                .sorted(Comparator.comparing(DomainType::simpleName))
                .toList();

        if (aggregates.isEmpty()) {
            diagram.append("    NONE[\"No Aggregate Roots detected\"]\n");
            diagram.append("```\n");
            return diagram.toString();
        }

        // Build aggregate nodes with their entities
        Map<String, List<DomainType>> aggregateChildren = findAggregateChildren(ir);

        for (DomainType aggregate : aggregates) {
            String aggId = sanitizeId(aggregate.simpleName());
            diagram.append("    subgraph ").append(aggId).append("_AGG[\"<b>").append(aggregate.simpleName())
                    .append("</b><br/>Aggregate Root\"]\n");

            // Add root
            diagram.append("        ").append(aggId).append("((").append(aggregate.simpleName()).append("))\n");

            // Add child entities
            List<DomainType> children = aggregateChildren.getOrDefault(aggregate.qualifiedName(), List.of());
            for (DomainType child : children.stream().limit(4).toList()) {
                String childId = sanitizeId(child.simpleName());
                String shape = child.kind() == DomainKind.ENTITY ? "[" + child.simpleName() + "]"
                        : "(" + child.simpleName() + ")";
                diagram.append("        ").append(childId).append(shape).append("\n");
                diagram.append("        ").append(aggId).append(" --> ").append(childId).append("\n");
            }
            if (children.size() > 4) {
                diagram.append("        MORE_").append(aggId).append("[\"... +").append(children.size() - 4)
                        .append(" more\"]\n");
            }

            diagram.append("    end\n\n");
        }

        // Add cross-aggregate relationships from domain relations
        for (DomainType aggregate : aggregates) {
            for (var relation : aggregate.relations()) {
                String targetFqn = relation.targetTypeFqn();
                // Check if target is another aggregate
                for (DomainType otherAgg : aggregates) {
                    if (otherAgg.qualifiedName().equals(targetFqn)) {
                        String fromId = sanitizeId(aggregate.simpleName());
                        String toId = sanitizeId(otherAgg.simpleName());
                        diagram.append("    ").append(fromId).append(" -.-> ").append(toId).append("\n");
                    }
                }
            }
        }

        // Styles
        diagram.append("\n");
        for (DomainType aggregate : aggregates) {
            diagram.append("    style ").append(sanitizeId(aggregate.simpleName())).append(" fill:#ff9800,stroke:#e65100\n");
        }

        diagram.append("```\n");
        return diagram.toString();
    }

    /**
     * Generates a Port Matrix diagram showing driving and driven ports.
     *
     * @param ir the IR snapshot
     * @return Mermaid diagram code
     */
    public String buildPortMatrixDiagram(IrSnapshot ir) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("```mermaid\n");
        diagram.append("flowchart LR\n");

        Map<PortDirection, List<Port>> portsByDirection = ir.ports().ports().stream()
                .collect(Collectors.groupingBy(Port::direction));

        List<Port> drivingPorts = portsByDirection.getOrDefault(PortDirection.DRIVING, List.of());
        List<Port> drivenPorts = portsByDirection.getOrDefault(PortDirection.DRIVEN, List.of());

        // Driving ports
        diagram.append("    subgraph DRIVING[\"🔵 DRIVING PORTS<br/>(Primary/Inbound)\"]\n");
        diagram.append("        direction TB\n");
        for (Port port : drivingPorts) {
            String id = sanitizeId(port.simpleName()) + "_IN";
            String icon = port.isRepository() ? "📦" : "🔌";
            diagram.append("        ").append(id).append("[\"").append(icon).append(" ").append(port.simpleName())
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
        for (Port port : drivenPorts) {
            String id = sanitizeId(port.simpleName()) + "_OUT";
            String icon = port.isRepository() ? "📦" : "🔌";
            diagram.append("        ").append(id).append("[\"").append(icon).append(" ").append(port.simpleName())
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
    private Map<String, List<DomainType>> findAggregateChildren(IrSnapshot ir) {
        Map<String, List<DomainType>> result = new HashMap<>();

        List<DomainType> aggregates = ir.domain().types().stream()
                .filter(t -> t.kind() == DomainKind.AGGREGATE_ROOT)
                .toList();

        List<DomainType> entities = ir.domain().types().stream()
                .filter(t -> t.kind() == DomainKind.ENTITY || t.kind() == DomainKind.VALUE_OBJECT)
                .toList();

        // Simple heuristic: entities in same package as aggregate are children
        for (DomainType aggregate : aggregates) {
            String aggPackage = aggregate.packageName();
            List<DomainType> children = entities.stream()
                    .filter(e -> e.packageName().equals(aggPackage) || e.packageName().startsWith(aggPackage + "."))
                    .collect(Collectors.toList());
            result.put(aggregate.qualifiedName(), children);
        }

        return result;
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
