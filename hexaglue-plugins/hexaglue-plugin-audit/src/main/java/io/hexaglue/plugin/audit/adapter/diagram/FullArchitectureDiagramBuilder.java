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
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a complete C4Component diagram showing the full hexagonal architecture.
 *
 * <p>This builder generates a comprehensive view of the architecture with three layers:
 * <ul>
 *   <li><strong>Driving Side</strong>: Application services, driving ports</li>
 *   <li><strong>Domain Core</strong>: Aggregate roots (limited to 15 for readability)</li>
 *   <li><strong>Driven Side</strong>: Driven ports and their adapter implementations</li>
 * </ul>
 *
 * <p>The diagram shows relationships between components:
 * <ul>
 *   <li>{@code orchestrates} - from services to aggregates</li>
 *   <li>{@code uses} - from aggregates to ports</li>
 *   <li>{@code implemented by} - from ports to adapters</li>
 *   <li>{@code cycle!} - bidirectional for cycle violations (shown as BiRel)</li>
 * </ul>
 *
 * <p>Violations are highlighted with specific styles:
 * <ul>
 *   <li>Cycle participants are shown with red background (#FF5978)</li>
 * </ul>
 *
 * @since 5.0.0
 */
public class FullArchitectureDiagramBuilder {

    /** Maximum number of components per layer to maintain readability. */
    private static final int MAX_COMPONENTS_PER_LAYER = 15;

    /** Color for cycle violation highlighting. */
    private static final String CYCLE_COLOR = "#FF5978";

    /** Color for driving side components. */
    private static final String DRIVING_COLOR = "#2196F3";

    /** Color for domain core components. */
    private static final String DOMAIN_COLOR = "#FF9800";

    /** Color for driven side components. */
    private static final String DRIVEN_COLOR = "#4CAF50";

    /**
     * Builds a full architecture C4Component diagram.
     *
     * @param projectName name of the project for the diagram title
     * @param components component details containing all architectural elements
     * @param relationships relationships between components, including cycles
     * @param typeViolations type-level violations for styling (cycles, etc.)
     * @return Optional containing Mermaid C4Component diagram code (without code fence), or empty if no meaningful content
     * @since 5.0.0
     */
    public Optional<String> build(
            String projectName,
            ComponentDetails components,
            List<Relationship> relationships,
            List<TypeViolation> typeViolations) {

        // Check if there's meaningful content to display
        if (!hasContent(components)) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("C4Component\n");
        sb.append("    title Full Architecture - ").append(projectName).append("\n\n");

        // Collect cycle participants for styling
        Set<String> cycleParticipants = collectCycleParticipants(relationships, typeViolations);

        // Track rendered relationships to avoid duplicates
        Set<String> renderedRels = new HashSet<>();

        // Track rendered component IDs to avoid duplicates and orphan references
        Set<String> renderedIds = new HashSet<>();

        // Driving Side (Application Services + Driving Ports)
        appendDrivingSide(sb, components, renderedIds);

        // Domain Core (Aggregates)
        appendDomainCore(sb, components, renderedIds);

        // Driven Side (Driven Ports + Adapters)
        appendDrivenSide(sb, components, renderedIds);

        // Relationships
        appendRelationships(sb, components, relationships, renderedRels, renderedIds);

        // Layout configuration
        sb.append("\n    UpdateLayoutConfig($c4ShapeInRow=\"3\", $c4BoundaryInRow=\"1\")\n\n");

        // Styles
        appendStyles(sb, components, cycleParticipants, renderedIds);

        return Optional.of(sb.toString().trim());
    }

    /**
     * Checks if there's meaningful content to display.
     *
     * <p>Returns true if there's at least one driven port or application service
     * (the minimum to show a hexagonal architecture).
     */
    private boolean hasContent(ComponentDetails components) {
        return !components.drivenPorts().isEmpty()
                || !components.applicationServices().isEmpty()
                || !components.drivingPorts().isEmpty();
    }

    /**
     * Collects names of types participating in cycles.
     */
    private Set<String> collectCycleParticipants(List<Relationship> relationships, List<TypeViolation> typeViolations) {
        Set<String> participants = new HashSet<>();

        // From relationships
        for (Relationship rel : relationships) {
            if (rel.isCycle()) {
                participants.add(rel.from());
                participants.add(rel.to());
            }
        }

        // From type violations
        for (TypeViolation tv : typeViolations) {
            if (tv.violationType() == TypeViolation.ViolationType.CYCLE) {
                participants.add(tv.typeName());
            }
        }

        return participants;
    }

    /**
     * Appends the driving side boundary (application services and driving ports).
     */
    private void appendDrivingSide(StringBuilder sb, ComponentDetails components, Set<String> renderedIds) {
        List<ApplicationServiceComponent> services = components.applicationServices();
        List<PortComponent> drivingPorts = components.drivingPorts();

        if (services.isEmpty() && drivingPorts.isEmpty()) {
            return;
        }

        sb.append("    Container_Boundary(driving, \"Driving Side\") {\n");

        // Application services
        int serviceCount = 0;
        for (ApplicationServiceComponent service : services) {
            if (serviceCount >= MAX_COMPONENTS_PER_LAYER) break;
            String serviceId = sanitizeId(service.name());
            if (!renderedIds.add(serviceId)) continue;
            sb.append("        Component(")
                    .append(serviceId)
                    .append(", \"")
                    .append(service.name())
                    .append("\", \"Application Service\", \"")
                    .append(service.methods())
                    .append(" methods\")\n");
            serviceCount++;
        }

        // Driving ports
        int portCount = 0;
        for (PortComponent port : drivingPorts) {
            if (serviceCount + portCount >= MAX_COMPONENTS_PER_LAYER) break;
            String portId = sanitizeId(port.name());
            if (!renderedIds.add(portId)) continue;
            sb.append("        Component(")
                    .append(portId)
                    .append(", \"")
                    .append(port.name())
                    .append("\", \"Driving Port\", \"")
                    .append(describePort(port))
                    .append("\")\n");
            portCount++;
        }

        sb.append("    }\n\n");
    }

    /**
     * Appends the domain core boundary (aggregates).
     */
    private void appendDomainCore(StringBuilder sb, ComponentDetails components, Set<String> renderedIds) {
        List<AggregateComponent> aggregates = components.aggregates();

        if (aggregates.isEmpty()) {
            return;
        }

        sb.append("    Container_Boundary(domain, \"Domain Core\") {\n");

        int count = 0;
        for (AggregateComponent agg : aggregates) {
            if (count >= MAX_COMPONENTS_PER_LAYER) break;
            String aggId = sanitizeId(agg.name());
            if (!renderedIds.add(aggId)) continue;
            sb.append("        Component(")
                    .append(aggId)
                    .append(", \"")
                    .append(agg.name())
                    .append("\", \"Aggregate Root\", \"")
                    .append(agg.fields())
                    .append(" fields\")\n");
            count++;
        }

        sb.append("    }\n\n");
    }

    /**
     * Appends the driven side boundary (driven ports and adapters).
     */
    private void appendDrivenSide(StringBuilder sb, ComponentDetails components, Set<String> renderedIds) {
        List<PortComponent> drivenPorts = components.drivenPorts();
        List<AdapterComponent> drivenAdapters = components.adapters().stream()
                .filter(a -> a.type() == AdapterComponent.AdapterType.DRIVEN)
                .toList();

        if (drivenPorts.isEmpty()) {
            return;
        }

        sb.append("    Container_Boundary(driven, \"Driven Side\") {\n");

        // Driven ports
        int portCount = 0;
        for (PortComponent port : drivenPorts) {
            if (portCount >= MAX_COMPONENTS_PER_LAYER) break;
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
            portCount++;
        }

        sb.append("    }\n\n");

        // Infrastructure layer (adapters)
        if (!drivenAdapters.isEmpty()) {
            sb.append("    Container_Boundary(infra, \"Infrastructure Layer\") {\n");
            int adapterCount = 0;
            for (AdapterComponent adapter : drivenAdapters) {
                if (adapterCount >= MAX_COMPONENTS_PER_LAYER) break;
                String adapterId = sanitizeId(adapter.name());
                if (!renderedIds.add(adapterId)) continue;
                sb.append("        Component(")
                        .append(adapterId)
                        .append(", \"")
                        .append(adapter.name())
                        .append("\", \"Adapter\", \"Implements ")
                        .append(adapter.implementsPort())
                        .append("\")\n");
                adapterCount++;
            }
            sb.append("    }\n\n");
        }
    }

    /**
     * Appends relationships between components.
     */
    private void appendRelationships(
            StringBuilder sb,
            ComponentDetails components,
            List<Relationship> relationships,
            Set<String> renderedRels,
            Set<String> renderedIds) {

        // Application service -> Aggregate orchestration
        for (ApplicationServiceComponent service : components.applicationServices()) {
            String fromId = sanitizeId(service.name());
            if (!renderedIds.contains(fromId)) continue;
            for (String agg : service.orchestrates()) {
                String toId = sanitizeId(agg);
                if (!renderedIds.contains(toId)) continue;
                String relKey = service.name() + "->" + agg;
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
    }

    /**
     * Appends style definitions for components.
     */
    private void appendStyles(
            StringBuilder sb, ComponentDetails components, Set<String> cycleParticipants, Set<String> renderedIds) {
        Set<String> styledIds = new HashSet<>();

        // Driving side styles
        for (ApplicationServiceComponent service : components.applicationServices()) {
            String id = sanitizeId(service.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            String style = cycleParticipants.contains(service.name()) ? CYCLE_COLOR : DRIVING_COLOR;
            sb.append("    UpdateElementStyle(")
                    .append(id)
                    .append(", $fontColor=\"white\", $bgColor=\"")
                    .append(style)
                    .append("\")\n");
        }
        for (PortComponent port : components.drivingPorts()) {
            String id = sanitizeId(port.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            String style = cycleParticipants.contains(port.name()) ? CYCLE_COLOR : DRIVING_COLOR;
            sb.append("    UpdateElementStyle(")
                    .append(id)
                    .append(", $fontColor=\"white\", $bgColor=\"")
                    .append(style)
                    .append("\")\n");
        }

        // Domain core styles
        for (AggregateComponent agg : components.aggregates()) {
            String id = sanitizeId(agg.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            String style = cycleParticipants.contains(agg.name()) ? CYCLE_COLOR : DOMAIN_COLOR;
            sb.append("    UpdateElementStyle(")
                    .append(id)
                    .append(", $fontColor=\"white\", $bgColor=\"")
                    .append(style)
                    .append("\")\n");
        }

        // Driven side styles
        for (PortComponent port : components.drivenPorts()) {
            String id = sanitizeId(port.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            String style = cycleParticipants.contains(port.name()) ? CYCLE_COLOR : DRIVEN_COLOR;
            sb.append("    UpdateElementStyle(")
                    .append(id)
                    .append(", $fontColor=\"white\", $bgColor=\"")
                    .append(style)
                    .append("\")\n");
        }

        // Adapter styles (driven only, to avoid overriding application service styles)
        List<AdapterComponent> drivenAdapters = components.adapters().stream()
                .filter(a -> a.type() == AdapterComponent.AdapterType.DRIVEN)
                .toList();
        for (AdapterComponent adapter : drivenAdapters) {
            String id = sanitizeId(adapter.name());
            if (!renderedIds.contains(id) || !styledIds.add(id)) continue;
            sb.append("    UpdateElementStyle(")
                    .append(id)
                    .append(", $fontColor=\"white\", $bgColor=\"")
                    .append(DRIVEN_COLOR)
                    .append("\")\n");
        }
    }

    /**
     * Describes a port for display in the diagram.
     */
    private String describePort(PortComponent port) {
        if (!port.orchestrates().isEmpty()) {
            return "Orchestrates " + String.join(", ", port.orchestrates());
        }
        return port.methods() + " methods";
    }

    /**
     * Sanitizes a name for use as a Mermaid ID.
     */
    private String sanitizeId(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
}
