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

import io.hexaglue.plugin.audit.domain.model.report.ApplicationServiceComponent;
import io.hexaglue.plugin.audit.domain.model.report.CommandHandlerComponent;
import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.QueryHandlerComponent;
import io.hexaglue.plugin.audit.domain.model.report.Relationship;
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds Mermaid class diagrams for the application layer.
 *
 * <p>Generates a class diagram showing application services, command handlers,
 * and query handlers with their relationships to aggregates and ports.
 *
 * <h3>Violation Styles</h3>
 * <ul>
 *   <li><strong>DependencyInversionWarning</strong> (amber #FFB300): Dependency inversion violated</li>
 *   <li><strong>LayerViolationWarning</strong> (gray #616161): Layer isolation violated</li>
 * </ul>
 *
 * @since 5.0.0
 */
public class ApplicationLayerDiagramBuilder {

    /**
     * Builds an application layer class diagram.
     *
     * @param components component details containing application layer components
     * @param relationships relationships to show connections
     * @param typeViolations type-level violations for styling
     * @return Mermaid classDiagram code (without code fence), or null if no components
     */
    public String build(
            ComponentDetails components, List<Relationship> relationships, List<TypeViolation> typeViolations) {

        // Check if there are any application layer components
        if (!components.hasApplicationLayer()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: Application Layer\n");
        sb.append("---\n");
        sb.append("classDiagram\n");

        // Track violations by type
        Set<String> dependencyInversionTypes = new HashSet<>();
        Set<String> layerViolationTypes = new HashSet<>();

        for (TypeViolation tv : typeViolations) {
            switch (tv.violationType()) {
                case DEPENDENCY_INVERSION -> dependencyInversionTypes.add(tv.typeName());
                case LAYER_VIOLATION -> layerViolationTypes.add(tv.typeName());
                default -> {
                    // Other violations not relevant for application layer
                }
            }
        }

        // Render Application Services
        for (ApplicationServiceComponent svc : components.applicationServices()) {
            sb.append("    class ").append(svc.name()).append("{\n");
            sb.append("        <<ApplicationService>>\n");
            sb.append("        +methods ").append(svc.methods()).append("\n");
            sb.append("    }\n");

            // Relationships to aggregates
            for (String aggregate : svc.orchestrates()) {
                sb.append("    ")
                        .append(svc.name())
                        .append(" --> ")
                        .append(aggregate)
                        .append(" : orchestrates\n");
            }

            // Relationships to ports
            for (String port : svc.usesPorts()) {
                sb.append("    ")
                        .append(svc.name())
                        .append(" --> ")
                        .append(port)
                        .append(" : uses\n");
            }
        }

        // Render Command Handlers
        for (CommandHandlerComponent handler : components.commandHandlers()) {
            sb.append("    class ").append(handler.name()).append("{\n");
            sb.append("        <<CommandHandler>>\n");
            handler.handledCommandOpt()
                    .ifPresent(cmd -> sb.append("        +handles ").append(cmd).append("\n"));
            sb.append("    }\n");

            // Relationship to target aggregate
            handler.targetAggregateOpt().ifPresent(agg -> sb.append("    ")
                    .append(handler.name())
                    .append(" --> ")
                    .append(agg)
                    .append(" : modifies\n"));
        }

        // Render Query Handlers
        for (QueryHandlerComponent handler : components.queryHandlers()) {
            sb.append("    class ").append(handler.name()).append("{\n");
            sb.append("        <<QueryHandler>>\n");
            handler.handledQueryOpt()
                    .ifPresent(query ->
                            sb.append("        +handles ").append(query).append("\n"));
            handler.returnTypeOpt()
                    .ifPresent(ret -> sb.append("        +returns ").append(ret).append("\n"));
            sb.append("    }\n");
        }

        // Add violation styles if needed
        boolean hasViolations = !dependencyInversionTypes.isEmpty() || !layerViolationTypes.isEmpty();

        if (hasViolations) {
            sb.append("\n");

            Set<String> styledTypes = new HashSet<>();

            // Apply CRITICAL severity styles first
            for (String type : dependencyInversionTypes) {
                if (!styledTypes.contains(type) && isApplicationLayerType(type, components)) {
                    sb.append("    class ").append(type).append(":::DependencyInversionWarning\n");
                    styledTypes.add(type);
                }
            }

            // Apply MAJOR severity styles
            for (String type : layerViolationTypes) {
                if (!styledTypes.contains(type) && isApplicationLayerType(type, components)) {
                    sb.append("    class ").append(type).append(":::LayerViolationWarning\n");
                    styledTypes.add(type);
                }
            }

            // Define styles
            if (!dependencyInversionTypes.isEmpty()) {
                sb.append(
                        "    classDef DependencyInversionWarning stroke:#FFB300,fill:#FFF8E1,color:#FF6F00,stroke-width:2px\n");
            }
            if (!layerViolationTypes.isEmpty()) {
                sb.append(
                        "    classDef LayerViolationWarning stroke:#616161,fill:#EEEEEE,color:#212121,stroke-width:2px\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Checks if a type name is part of the application layer.
     */
    private boolean isApplicationLayerType(String typeName, ComponentDetails components) {
        return components.applicationServices().stream().anyMatch(s -> s.name().equals(typeName))
                || components.commandHandlers().stream().anyMatch(h -> h.name().equals(typeName))
                || components.queryHandlers().stream().anyMatch(h -> h.name().equals(typeName));
    }

    /**
     * Builds an application layer class diagram (backward compatibility).
     *
     * @param components component details
     * @param relationships relationships
     * @return Mermaid classDiagram code (without code fence)
     */
    public String build(ComponentDetails components, List<Relationship> relationships) {
        return build(components, relationships, List.of());
    }
}
