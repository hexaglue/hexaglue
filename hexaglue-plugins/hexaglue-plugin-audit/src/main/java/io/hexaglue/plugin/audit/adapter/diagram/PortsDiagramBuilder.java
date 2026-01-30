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

import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.PortComponent;
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds Mermaid class diagrams for the ports layer.
 *
 * <p>Generates a class diagram showing driving ports (inbound) and driven ports
 * (outbound) with their adapter implementations.
 *
 * <h3>Violation Styles</h3>
 * <ul>
 *   <li><strong>PortUncoveredWarning</strong> (teal #00897B): Port without adapter</li>
 *   <li><strong>PortNotInterfaceWarning</strong> (brown #8D6E63): Port not an interface</li>
 * </ul>
 *
 * @since 5.0.0
 */
public class PortsDiagramBuilder {

    /**
     * Builds a ports layer class diagram.
     *
     * @param components component details containing ports
     * @param typeViolations type-level violations for styling
     * @return Optional containing Mermaid classDiagram code (without code fence), or empty if no ports
     */
    public Optional<String> build(ComponentDetails components, List<TypeViolation> typeViolations) {

        // Check if there are any ports
        if (!components.hasPortsLayer()) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: Ports Layer\n");
        sb.append("---\n");
        sb.append("classDiagram\n");

        // Track violations by type
        Set<String> portUncoveredTypes = new HashSet<>();
        Set<String> portNotInterfaceTypes = new HashSet<>();

        for (TypeViolation tv : typeViolations) {
            switch (tv.violationType()) {
                case PORT_UNCOVERED -> portUncoveredTypes.add(tv.typeName());
                case PORT_NOT_INTERFACE -> portNotInterfaceTypes.add(tv.typeName());
                default -> {
                    // Other violations not relevant for ports layer
                }
            }
        }

        // Track rendered adapters to avoid duplicates
        Set<String> renderedAdapters = new HashSet<>();

        // Render Driving Ports (Inbound)
        if (!components.drivingPorts().isEmpty()) {
            sb.append("    %% Driving Ports (Inbound)\n");
            for (PortComponent port : components.drivingPorts()) {
                sb.append("    class ").append(port.name()).append("{\n");
                sb.append("        <<DrivingPort>>\n");
                sb.append("        +methods ").append(port.methods()).append("\n");
                sb.append("    }\n");

                // Show adapter implementation if present
                if (port.hasAdapter()) {
                    String adapter = port.adapter();
                    if (!renderedAdapters.contains(adapter)) {
                        sb.append("    class ").append(adapter).append("{\n");
                        sb.append("        <<Adapter>>\n");
                        sb.append("    }\n");
                        renderedAdapters.add(adapter);
                    }
                    sb.append("    ")
                            .append(adapter)
                            .append(" ..|> ")
                            .append(port.name())
                            .append(" : implements\n");
                }

                // Show orchestrated aggregates
                for (String aggregate : port.orchestrates()) {
                    sb.append("    ")
                            .append(port.name())
                            .append(" --> ")
                            .append(aggregate)
                            .append(" : orchestrates\n");
                }
            }
        }

        // Render Driven Ports (Outbound)
        if (!components.drivenPorts().isEmpty()) {
            sb.append("\n    %% Driven Ports (Outbound)\n");
            for (PortComponent port : components.drivenPorts()) {
                sb.append("    class ").append(port.name()).append("{\n");
                sb.append("        <<DrivenPort>>\n");
                port.kindOpt()
                        .ifPresent(
                                kind -> sb.append("        +type ").append(kind).append("\n"));
                sb.append("        +methods ").append(port.methods()).append("\n");
                sb.append("    }\n");

                // Show adapter implementation if present
                if (port.hasAdapter()) {
                    String adapter = port.adapter();
                    if (!renderedAdapters.contains(adapter)) {
                        sb.append("    class ").append(adapter).append("{\n");
                        sb.append("        <<Adapter>>\n");
                        sb.append("    }\n");
                        renderedAdapters.add(adapter);
                    }
                    sb.append("    ")
                            .append(adapter)
                            .append(" ..|> ")
                            .append(port.name())
                            .append(" : implements\n");
                }
            }
        }

        // Add violation styles if needed
        boolean hasViolations = !portUncoveredTypes.isEmpty() || !portNotInterfaceTypes.isEmpty();

        if (hasViolations) {
            sb.append("\n");

            Set<String> styledTypes = new HashSet<>();

            // Apply CRITICAL severity styles first (PORT_NOT_INTERFACE)
            for (String type : portNotInterfaceTypes) {
                if (!styledTypes.contains(type) && isPortType(type, components)) {
                    sb.append("    class ").append(type).append(":::PortNotInterfaceWarning\n");
                    styledTypes.add(type);
                }
            }

            // Apply MAJOR severity styles (PORT_UNCOVERED)
            for (String type : portUncoveredTypes) {
                if (!styledTypes.contains(type) && isPortType(type, components)) {
                    sb.append("    class ").append(type).append(":::PortUncoveredWarning\n");
                    styledTypes.add(type);
                }
            }

            // Define styles
            if (!portNotInterfaceTypes.isEmpty()) {
                sb.append(
                        "    classDef PortNotInterfaceWarning stroke:#8D6E63,fill:#EFEBE9,color:#4E342E,stroke-width:2px\n");
            }
            if (!portUncoveredTypes.isEmpty()) {
                sb.append(
                        "    classDef PortUncoveredWarning stroke:#00897B,fill:#E0F2F1,color:#004D40,stroke-width:2px\n");
            }
        }

        return Optional.of(sb.toString().trim());
    }

    /**
     * Checks if a type name is a port.
     */
    private boolean isPortType(String typeName, ComponentDetails components) {
        return components.drivingPorts().stream().anyMatch(p -> p.name().equals(typeName))
                || components.drivenPorts().stream().anyMatch(p -> p.name().equals(typeName));
    }

    /**
     * Builds a ports layer class diagram without violation styling.
     *
     * @param components component details
     * @return Optional containing Mermaid classDiagram code (without code fence)
     */
    public Optional<String> build(ComponentDetails components) {
        return build(components, List.of());
    }
}
