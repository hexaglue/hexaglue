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

import io.hexaglue.plugin.audit.domain.model.report.PortComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds Mermaid C4Context diagrams showing system context.
 *
 * <p>Uses native Mermaid C4Context syntax to show the system
 * with its external actors and systems.
 *
 * @since 5.0.0
 */
public class C4ContextDiagramBuilder {

    /**
     * Builds a C4 System Context diagram.
     *
     * @param projectName name of the project/system
     * @param drivingPorts driving ports (to infer user interactions)
     * @param drivenPorts driven ports (to infer external systems)
     * @return Mermaid C4Context diagram code (without code fence)
     */
    public String build(String projectName, List<PortComponent> drivingPorts, List<PortComponent> drivenPorts) {
        StringBuilder sb = new StringBuilder();
        sb.append("C4Context\n");
        sb.append("    title System Context - ").append(projectName).append("\n\n");

        // User/Actor
        String actorDesc = !drivingPorts.isEmpty()
                ? "Interacts via " + drivingPorts.get(0).name()
                : "Interacts with the system";
        sb.append("    Person(user, \"User\", \"").append(actorDesc).append("\")\n");

        // Main system
        sb.append("    System(app, \"")
                .append(projectName)
                .append("\", \"Manages domain operations\")\n\n");

        // Infer external systems from driven ports
        Set<String> externalSystems = new HashSet<>();
        for (PortComponent port : drivenPorts) {
            String kind = port.kindOpt().orElse("OTHER");
            String externalSystem = inferExternalSystem(port.name(), kind);
            if (!externalSystems.contains(externalSystem)) {
                String sysId = sanitizeId(externalSystem);
                sb.append("    System_Ext(")
                        .append(sysId)
                        .append(", \"")
                        .append(externalSystem)
                        .append("\", \"Inferred from ")
                        .append(port.name())
                        .append("\")\n");
                externalSystems.add(externalSystem);
            }
        }

        sb.append("\n");

        // Relationships
        sb.append("    Rel_D(user, app, \"Uses\", \"REST/HTTP\")\n");

        for (String extSys : externalSystems) {
            String sysId = sanitizeId(extSys);
            String relType = extSys.contains("Database") ? "Persists data" : "Integrates with";
            sb.append("    Rel_D(app, ").append(sysId).append(", \"").append(relType).append("\")\n");
        }

        sb.append("\n");

        // Layout config
        sb.append("    UpdateLayoutConfig($c4ShapeInRow=\"3\", $c4BoundaryInRow=\"1\")\n\n");

        // Styles
        sb.append("    UpdateElementStyle(app, $fontColor=\"white\", $bgColor=\"#1565C0\")\n");
        for (String extSys : externalSystems) {
            String sysId = sanitizeId(extSys);
            String color = extSys.contains("Database") ? "#4CAF50" : "#9C27B0";
            sb.append("    UpdateElementStyle(")
                    .append(sysId)
                    .append(", $fontColor=\"white\", $bgColor=\"")
                    .append(color)
                    .append("\")\n");
        }

        return sb.toString().trim();
    }

    /**
     * Infers an external system name from a port name and kind.
     */
    private String inferExternalSystem(String portName, String kind) {
        if ("REPOSITORY".equals(kind)) {
            return "Database";
        }
        if ("GATEWAY".equals(kind)) {
            // Try to infer from port name
            String lower = portName.toLowerCase();
            if (lower.contains("payment")) {
                return "Payment Provider";
            }
            if (lower.contains("notification") || lower.contains("email") || lower.contains("sms")) {
                return "Notification Service";
            }
            if (lower.contains("auth")) {
                return "Auth Provider";
            }
            if (lower.contains("search") || lower.contains("elastic")) {
                return "Search Engine";
            }
            if (lower.contains("cache") || lower.contains("redis")) {
                return "Cache Service";
            }
            if (lower.contains("queue") || lower.contains("message") || lower.contains("event")) {
                return "Message Queue";
            }
            return "External API";
        }
        if ("EVENT_PUBLISHER".equals(kind)) {
            return "Message Queue";
        }
        return "External System";
    }

    /**
     * Sanitizes a string to be a valid Mermaid ID.
     */
    private String sanitizeId(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
}
