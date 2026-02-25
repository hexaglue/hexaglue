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

import io.hexaglue.plugin.audit.domain.model.report.AdapterComponent;
import io.hexaglue.plugin.audit.domain.model.report.AggregateComponent;
import io.hexaglue.plugin.audit.domain.model.report.ApplicationServiceComponent;
import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.PortComponent;
import io.hexaglue.plugin.audit.domain.model.report.Relationship;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds Mermaid C4Component diagrams showing system components.
 *
 * <p>Uses native Mermaid C4Component syntax to show ports, aggregates,
 * adapters and their relationships, with cycles highlighted.
 *
 * @since 5.0.0
 */
public class C4ComponentDiagramBuilder {

    /**
     * Builds a C4 Component diagram.
     *
     * @param projectName name of the project
     * @param components component details
     * @param relationships relationships including cycles
     * @return Mermaid C4Component diagram code (without code fence)
     */
    public String build(String projectName, ComponentDetails components, List<Relationship> relationships) {
        StringBuilder sb = new StringBuilder();
        sb.append("C4Component\n");
        sb.append("    title Component Diagram - ").append(projectName).append("\n\n");

        // Track rendered component IDs to avoid duplicates and orphan references
        Set<String> renderedIds = new HashSet<>();

        // Driving Side
        if (!components.drivingPorts().isEmpty()
                || !components.applicationServices().isEmpty()) {
            sb.append("    Container_Boundary(driving, \"Driving Side\") {\n");
            for (PortComponent port : components.drivingPorts()) {
                String portId = sanitizeId(port.name());
                if (!renderedIds.add(portId)) continue;
                sb.append("        Component(")
                        .append(portId)
                        .append(", \"")
                        .append(port.name())
                        .append("\", \"Driving Port\", \"")
                        .append(describePort(port))
                        .append("\")\n");
            }
            for (ApplicationServiceComponent service : components.applicationServices()) {
                String serviceId = sanitizeId(service.name());
                if (!renderedIds.add(serviceId)) continue;
                sb.append("        Component(")
                        .append(serviceId)
                        .append(", \"")
                        .append(service.name())
                        .append("\", \"Application Service\", \"")
                        .append(service.methods())
                        .append(" methods\")\n");
            }
            sb.append("    }\n\n");
        }

        // Domain Core
        if (!components.aggregates().isEmpty()) {
            sb.append("    Container_Boundary(domain, \"Domain Core\") {\n");
            for (AggregateComponent agg : components.aggregates()) {
                String aggId = sanitizeId(agg.name());
                if (!renderedIds.add(aggId)) continue;
                sb.append("        Component(")
                        .append(aggId)
                        .append(", \"")
                        .append(agg.name())
                        .append("\", \"Aggregate Root\", \"")
                        .append(agg.fields())
                        .append(" fields\")\n");
            }
            sb.append("    }\n\n");
        }

        // Driven Side
        if (!components.drivenPorts().isEmpty()) {
            sb.append("    Container_Boundary(driven, \"Driven Side\") {\n");
            for (PortComponent port : components.drivenPorts()) {
                String portId = sanitizeId(port.name());
                if (!renderedIds.add(portId)) continue;
                String kind = port.kindOpt().orElse("Port");
                String componentType = "REPOSITORY".equals(kind) ? "ComponentDb" : "Component";
                String styleHint = "GATEWAY".equals(kind) ? "_Ext" : "";
                sb.append("        ")
                        .append(componentType)
                        .append(styleHint)
                        .append("(")
                        .append(portId)
                        .append(", \"")
                        .append(port.name())
                        .append("\", \"")
                        .append(kind)
                        .append("\", \"")
                        .append(port.methods())
                        .append(" methods\")\n");
            }
            sb.append("    }\n\n");

            // Infrastructure (adapters)
            List<AdapterComponent> drivenAdapters = components.adapters().stream()
                    .filter(a -> a.type() == AdapterComponent.AdapterType.DRIVEN)
                    .toList();
            if (!drivenAdapters.isEmpty()) {
                sb.append("    Container_Boundary(infra, \"Infrastructure Layer\") {\n");
                for (AdapterComponent adapter : drivenAdapters) {
                    String adapterId = sanitizeId(adapter.name());
                    if (!renderedIds.add(adapterId)) continue;
                    sb.append("        Component(")
                            .append(adapterId)
                            .append(", \"")
                            .append(adapter.name())
                            .append("\", \"Adapter\", \"Implements ")
                            .append(adapter.implementsPort())
                            .append("\")\n");
                }
                sb.append("    }\n\n");
            }
        }

        // Relationships
        Set<String> renderedRels = new HashSet<>();

        // Driving port -> Aggregate orchestration
        for (PortComponent port : components.drivingPorts()) {
            String fromId = sanitizeId(port.name());
            if (!renderedIds.contains(fromId)) continue;
            for (String agg : port.orchestrates()) {
                String toId = sanitizeId(agg);
                if (!renderedIds.contains(toId)) continue;
                String relKey = port.name() + "->" + agg;
                if (!renderedRels.contains(relKey)) {
                    sb.append("    Rel_D(")
                            .append(fromId)
                            .append(", ")
                            .append(toId)
                            .append(", \"orchestrates\")\n");
                    renderedRels.add(relKey);
                }
            }
        }

        // Aggregate -> Port usage
        for (AggregateComponent agg : components.aggregates()) {
            String fromId = sanitizeId(agg.name());
            if (!renderedIds.contains(fromId)) continue;
            for (String portName : agg.usesPorts()) {
                String toId = sanitizeId(portName);
                if (!renderedIds.contains(toId)) continue;
                String relKey = agg.name() + "->" + portName;
                if (!renderedRels.contains(relKey)) {
                    sb.append("    Rel_D(")
                            .append(fromId)
                            .append(", ")
                            .append(toId)
                            .append(", \"uses\")\n");
                    renderedRels.add(relKey);
                }
            }
        }

        // Port -> Adapter implementation
        for (AdapterComponent adapter : components.adapters()) {
            String fromId = sanitizeId(adapter.implementsPort());
            String toId = sanitizeId(adapter.name());
            if (!renderedIds.contains(fromId) || !renderedIds.contains(toId)) continue;
            String relKey = adapter.implementsPort() + "->" + adapter.name();
            if (!renderedRels.contains(relKey)) {
                sb.append("    Rel_D(").append(fromId).append(", ").append(toId).append(", \"implemented by\")\n");
                renderedRels.add(relKey);
            }
        }

        // Inter-aggregate cycles (special highlighting)
        for (Relationship rel : relationships) {
            if (rel.isCycle() && "references".equals(rel.type())) {
                String fromId = sanitizeId(rel.from());
                String toId = sanitizeId(rel.to());
                if (!renderedIds.contains(fromId) || !renderedIds.contains(toId)) continue;
                String relKey = rel.from() + "<->" + rel.to();
                String reverseKey = rel.to() + "<->" + rel.from();
                if (!renderedRels.contains(relKey) && !renderedRels.contains(reverseKey)) {
                    sb.append("    BiRel(")
                            .append(fromId)
                            .append(", ")
                            .append(toId)
                            .append(", \"cycle!\")\n");
                    renderedRels.add(relKey);
                }
            }
        }

        sb.append("\n");

        // Layout config
        sb.append("    UpdateLayoutConfig($c4ShapeInRow=\"3\", $c4BoundaryInRow=\"1\")\n\n");

        // Styles
        Set<String> styledIds = new HashSet<>();
        for (PortComponent port : components.drivingPorts()) {
            String id = sanitizeId(port.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            sb.append("    UpdateElementStyle(").append(id).append(", $fontColor=\"white\", $bgColor=\"#2196F3\")\n");
        }
        for (ApplicationServiceComponent service : components.applicationServices()) {
            String id = sanitizeId(service.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            sb.append("    UpdateElementStyle(").append(id).append(", $fontColor=\"white\", $bgColor=\"#2196F3\")\n");
        }
        for (AggregateComponent agg : components.aggregates()) {
            String id = sanitizeId(agg.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            sb.append("    UpdateElementStyle(").append(id).append(", $fontColor=\"white\", $bgColor=\"#FF9800\")\n");
        }
        List<AdapterComponent> drivenAdaptersForStyle = components.adapters().stream()
                .filter(a -> a.type() == AdapterComponent.AdapterType.DRIVEN)
                .toList();
        for (AdapterComponent adapter : drivenAdaptersForStyle) {
            String id = sanitizeId(adapter.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            sb.append("    UpdateElementStyle(").append(id).append(", $fontColor=\"white\", $bgColor=\"#4CAF50\")\n");
        }

        // Highlight cycles in red
        for (Relationship rel : relationships) {
            if (rel.isCycle()) {
                String fromId = sanitizeId(rel.from());
                String toId = sanitizeId(rel.to());
                if (!renderedIds.contains(fromId) || !renderedIds.contains(toId)) continue;
                sb.append("    UpdateRelStyle(")
                        .append(fromId)
                        .append(", ")
                        .append(toId)
                        .append(", $lineColor=\"red\", $textColor=\"red\")\n");
            }
        }

        return sb.toString().trim();
    }

    private String describePort(PortComponent port) {
        if (!port.orchestrates().isEmpty()) {
            return "Orchestrates " + String.join(", ", port.orchestrates());
        }
        return port.methods() + " methods";
    }

    private String sanitizeId(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
}
