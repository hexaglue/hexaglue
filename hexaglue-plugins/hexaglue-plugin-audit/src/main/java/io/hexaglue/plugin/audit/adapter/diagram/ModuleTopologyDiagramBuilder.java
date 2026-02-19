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

import io.hexaglue.plugin.audit.domain.model.report.ModuleTopology;
import java.util.Optional;

/**
 * Generates a Mermaid diagram showing module topology for multi-module projects.
 *
 * <p>The diagram uses {@code graph TB} (top-to-bottom) layout with colored nodes
 * based on architectural role:
 * <ul>
 *   <li>DOMAIN = green</li>
 *   <li>INFRASTRUCTURE = blue</li>
 *   <li>APPLICATION = orange</li>
 *   <li>API = purple</li>
 *   <li>ASSEMBLY = grey</li>
 *   <li>SHARED = brown</li>
 * </ul>
 *
 * @since 5.0.0
 */
public class ModuleTopologyDiagramBuilder {

    /**
     * Builds a Mermaid graph diagram from module topology.
     *
     * @param topology the module topology
     * @return the Mermaid diagram, or empty if topology is null or has no modules
     */
    public Optional<String> build(ModuleTopology topology) {
        if (topology == null || topology.modules().isEmpty()) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("graph TB\n");

        for (ModuleTopology.ModuleInfo module : topology.modules()) {
            String nodeId = sanitizeId(module.moduleId());
            sb.append("    ")
                    .append(nodeId)
                    .append("[\"")
                    .append(module.moduleId())
                    .append("<br>")
                    .append(module.role())
                    .append(" (")
                    .append(module.typeCount())
                    .append(" types)\"]\n");
        }

        sb.append("\n");

        // Style nodes by role
        for (ModuleTopology.ModuleInfo module : topology.modules()) {
            String nodeId = sanitizeId(module.moduleId());
            String color = colorForRole(module.role());
            sb.append("    style ")
                    .append(nodeId)
                    .append(" fill:")
                    .append(color)
                    .append(",color:#fff\n");
        }

        return Optional.of(sb.toString());
    }

    private String colorForRole(String role) {
        return switch (role) {
            case "DOMAIN" -> "#2d6a4f";
            case "INFRASTRUCTURE" -> "#1d3557";
            case "APPLICATION" -> "#e76f51";
            case "API" -> "#7b2cbf";
            case "ASSEMBLY" -> "#6c757d";
            case "SHARED" -> "#795548";
            default -> "#495057";
        };
    }

    private String sanitizeId(String moduleId) {
        return moduleId.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
