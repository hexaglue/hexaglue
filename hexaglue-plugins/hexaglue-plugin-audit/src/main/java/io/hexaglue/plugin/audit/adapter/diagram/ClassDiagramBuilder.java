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

import io.hexaglue.plugin.audit.domain.model.report.AggregateComponent;
import io.hexaglue.plugin.audit.domain.model.report.ComponentDetails;
import io.hexaglue.plugin.audit.domain.model.report.FieldDetail;
import io.hexaglue.plugin.audit.domain.model.report.IdentifierComponent;
import io.hexaglue.plugin.audit.domain.model.report.MethodDetail;
import io.hexaglue.plugin.audit.domain.model.report.Relationship;
import io.hexaglue.plugin.audit.domain.model.report.TypeViolation;
import io.hexaglue.plugin.audit.domain.model.report.ValueObjectComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds Mermaid class diagrams for domain model visualization.
 *
 * <p>Generates a class diagram showing aggregates with their value objects,
 * identifiers, and inter-aggregate references. Violations are highlighted
 * with specific styles based on severity:
 *
 * <h3>BLOCKER severity:</h3>
 * <ul>
 *   <li><strong>Alert</strong> (red #FF5978): Cycle violations between aggregates</li>
 * </ul>
 *
 * <h3>CRITICAL severity:</h3>
 * <ul>
 *   <li><strong>MutableWarning</strong> (orange #FF9800): Mutable value objects</li>
 *   <li><strong>ImpurityWarning</strong> (purple #9C27B0): Domain purity violations</li>
 *   <li><strong>MissingIdentityWarning</strong> (yellow #FBC02D): Entity missing identity</li>
 *   <li><strong>DependencyInversionWarning</strong> (amber #FFB300): Dependency inversion violated</li>
 *   <li><strong>PortNotInterfaceWarning</strong> (brown #8D6E63): Port not an interface</li>
 * </ul>
 *
 * <h3>MAJOR severity:</h3>
 * <ul>
 *   <li><strong>BoundaryWarning</strong> (red #E53935): Aggregate boundary violations</li>
 *   <li><strong>MissingRepositoryInfo</strong> (blue #1976D2): Aggregate missing repository</li>
 *   <li><strong>PortUncoveredWarning</strong> (teal #00897B): Port without adapter</li>
 *   <li><strong>LayerViolationWarning</strong> (gray #616161): Layer isolation violated</li>
 * </ul>
 *
 * <h3>MINOR severity:</h3>
 * <ul>
 *   <li><strong>EventNamingWarning</strong> (cyan #00ACC1): Event not named in past tense</li>
 * </ul>
 *
 * @since 5.0.0
 */
public class ClassDiagramBuilder {

    /**
     * Builds a domain model class diagram.
     *
     * @param components component details
     * @param relationships relationships including cycles
     * @param typeViolations type-level violations for styling
     * @return Mermaid classDiagram code (without code fence)
     */
    public String build(
            ComponentDetails components, List<Relationship> relationships, List<TypeViolation> typeViolations) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("config:\n");
        sb.append("    layout: elk\n");
        sb.append("title: Domain Model\n");
        sb.append("---\n");
        sb.append("classDiagram\n");

        if (components.aggregates().isEmpty()) {
            sb.append("    class NoAggregates\n");
            sb.append("    NoAggregates : No Aggregates Detected\n");
            return sb.toString().trim();
        }

        // Track which styles are needed by severity level for priority handling
        // BLOCKER severity
        Set<String> cycleParticipants = new HashSet<>();
        // CRITICAL severity
        Set<String> mutableVOs = new HashSet<>();
        Set<String> impureTypes = new HashSet<>();
        Set<String> missingIdentityTypes = new HashSet<>();
        Set<String> dependencyInversionTypes = new HashSet<>();
        Set<String> portNotInterfaceTypes = new HashSet<>();
        // MAJOR severity
        Set<String> boundaryViolations = new HashSet<>();
        Set<String> missingRepositoryTypes = new HashSet<>();
        Set<String> portUncoveredTypes = new HashSet<>();
        Set<String> layerViolationTypes = new HashSet<>();
        // MINOR severity
        Set<String> eventNamingTypes = new HashSet<>();

        // Categorize violations by type
        for (TypeViolation tv : typeViolations) {
            switch (tv.violationType()) {
                // BLOCKER
                case CYCLE -> cycleParticipants.add(tv.typeName());
                // CRITICAL
                case MUTABLE_VALUE_OBJECT -> mutableVOs.add(tv.typeName());
                case IMPURE_DOMAIN -> impureTypes.add(tv.typeName());
                case MISSING_IDENTITY -> missingIdentityTypes.add(tv.typeName());
                case DEPENDENCY_INVERSION -> dependencyInversionTypes.add(tv.typeName());
                case PORT_NOT_INTERFACE -> portNotInterfaceTypes.add(tv.typeName());
                // MAJOR
                case BOUNDARY_VIOLATION -> boundaryViolations.add(tv.typeName());
                case MISSING_REPOSITORY -> missingRepositoryTypes.add(tv.typeName());
                case PORT_UNCOVERED -> portUncoveredTypes.add(tv.typeName());
                case LAYER_VIOLATION -> layerViolationTypes.add(tv.typeName());
                // MINOR
                case EVENT_NAMING -> eventNamingTypes.add(tv.typeName());
            }
        }

        // Track which types have been rendered
        Set<String> renderedTypes = new HashSet<>();

        // Render all aggregates first
        for (AggregateComponent agg : components.aggregates()) {
            // Aggregate Root class with stereotype and details
            sb.append("    class ").append(agg.name()).append("{\n");
            sb.append("        <<AggregateRoot>>\n");
            if (!appendFieldDetails(sb, agg.fieldDetails()) && agg.fields() > 0) {
                // Fallback to summary when no details available
                sb.append("        +fields ").append(agg.fields()).append("\n");
            }
            appendMethodDetails(sb, agg.methodDetails());
            sb.append("    }\n");
            renderedTypes.add(agg.name());
        }

        // Collect all types that have composition relationships (from any aggregate/entity)
        Set<String> compositionTargets = relationships.stream()
                .filter(r -> "owns".equals(r.type()) || "contains".equals(r.type()))
                .map(Relationship::to)
                .collect(Collectors.toSet());

        // Render Value Objects that are composition targets
        for (ValueObjectComponent vo : components.valueObjects()) {
            if (!renderedTypes.contains(vo.name()) && compositionTargets.contains(vo.name())) {
                sb.append("    class ").append(vo.name()).append("{\n");
                sb.append("        <<ValueObject>>\n");
                appendFieldDetails(sb, vo.fieldDetails());
                sb.append("    }\n");
                renderedTypes.add(vo.name());
            }
        }

        // Render Identifiers that are composition targets
        for (IdentifierComponent id : components.identifiers()) {
            if (!renderedTypes.contains(id.name()) && compositionTargets.contains(id.name())) {
                sb.append("    class ").append(id.name()).append("{\n");
                sb.append("        <<Identifier>>\n");
                id.wrappedTypeOpt().ifPresent(wt -> sb.append("        +wraps ")
                        .append(simpleTypeName(wt))
                        .append("\n"));
                sb.append("    }\n");
                renderedTypes.add(id.name());
            }
        }

        // Render remaining Value Objects (for completeness, even if no explicit relationship)
        for (ValueObjectComponent vo : components.valueObjects()) {
            if (!renderedTypes.contains(vo.name())) {
                sb.append("    class ").append(vo.name()).append("{\n");
                sb.append("        <<ValueObject>>\n");
                appendFieldDetails(sb, vo.fieldDetails());
                sb.append("    }\n");
                renderedTypes.add(vo.name());
            }
        }

        // Render remaining Identifiers (for completeness)
        for (IdentifierComponent id : components.identifiers()) {
            if (!renderedTypes.contains(id.name())) {
                sb.append("    class ").append(id.name()).append("{\n");
                sb.append("        <<Identifier>>\n");
                id.wrappedTypeOpt().ifPresent(wt -> sb.append("        +wraps ")
                        .append(simpleTypeName(wt))
                        .append("\n"));
                sb.append("    }\n");
                renderedTypes.add(id.name());
            }
        }

        // Render composition relationships from the actual relationship graph (cross-package aware)
        Set<String> renderedCompositions = new HashSet<>();
        for (Relationship rel : relationships) {
            if ("owns".equals(rel.type()) || "contains".equals(rel.type())) {
                String key = rel.from() + "->" + rel.to();
                if (!renderedCompositions.contains(key)) {
                    sb.append("    ")
                            .append(rel.from())
                            .append(" *-- ")
                            .append(rel.to())
                            .append("\n");
                    renderedCompositions.add(key);
                }
            }
        }

        // Render all other relationship types
        Set<String> renderedRelationships = new HashSet<>();

        for (Relationship rel : relationships) {
            String key = rel.from() + "->" + rel.to();
            String reverseKey = rel.to() + "->" + rel.from();

            // Skip compositions (already rendered above) and already rendered relationships
            if ("owns".equals(rel.type()) || "contains".equals(rel.type())) {
                continue;
            }
            if (renderedRelationships.contains(key)) {
                continue;
            }

            String arrow = getArrowForRelationType(rel.type());
            String label = getLabelForRelationType(rel.type());

            // Handle cycles specially
            if (rel.isCycle()) {
                // Skip reverse if already rendered
                if (renderedRelationships.contains(reverseKey)) {
                    continue;
                }
                sb.append("    ")
                        .append(rel.from())
                        .append(" <..> ")
                        .append(rel.to())
                        .append(" : CYCLE!!\n");
                renderedRelationships.add(key);
                renderedRelationships.add(reverseKey);
                cycleParticipants.add(rel.from());
                cycleParticipants.add(rel.to());
            } else {
                sb.append("    ")
                        .append(rel.from())
                        .append(" ")
                        .append(arrow)
                        .append(" ")
                        .append(rel.to());
                if (label != null) {
                    sb.append(" : ").append(label);
                }
                sb.append("\n");
                renderedRelationships.add(key);
            }
        }

        // Add styles section if there are any violations
        boolean hasAnyViolations = !cycleParticipants.isEmpty()
                || !mutableVOs.isEmpty()
                || !impureTypes.isEmpty()
                || !missingIdentityTypes.isEmpty()
                || !dependencyInversionTypes.isEmpty()
                || !portNotInterfaceTypes.isEmpty()
                || !boundaryViolations.isEmpty()
                || !missingRepositoryTypes.isEmpty()
                || !portUncoveredTypes.isEmpty()
                || !layerViolationTypes.isEmpty()
                || !eventNamingTypes.isEmpty();

        if (hasAnyViolations) {
            sb.append("\n");

            // Track which types have been styled (for priority handling)
            Set<String> styledTypes = new HashSet<>();

            // Apply class styles by severity priority (BLOCKER > CRITICAL > MAJOR > MINOR)

            // BLOCKER: Cycle (highest priority)
            for (String participant : cycleParticipants) {
                sb.append("    class ").append(participant).append(":::Alert\n");
                styledTypes.add(participant);
            }

            // CRITICAL: Mutable VO, Impure Domain, Missing Identity, Dependency Inversion, Port Not Interface
            for (String vo : mutableVOs) {
                if (!styledTypes.contains(vo)) {
                    sb.append("    class ").append(vo).append(":::MutableWarning\n");
                    styledTypes.add(vo);
                }
            }
            for (String impure : impureTypes) {
                if (!styledTypes.contains(impure)) {
                    sb.append("    class ").append(impure).append(":::ImpurityWarning\n");
                    styledTypes.add(impure);
                }
            }
            for (String missingId : missingIdentityTypes) {
                if (!styledTypes.contains(missingId)) {
                    sb.append("    class ").append(missingId).append(":::MissingIdentityWarning\n");
                    styledTypes.add(missingId);
                }
            }
            for (String depInv : dependencyInversionTypes) {
                if (!styledTypes.contains(depInv)) {
                    sb.append("    class ").append(depInv).append(":::DependencyInversionWarning\n");
                    styledTypes.add(depInv);
                }
            }
            for (String portNotIface : portNotInterfaceTypes) {
                if (!styledTypes.contains(portNotIface)) {
                    sb.append("    class ").append(portNotIface).append(":::PortNotInterfaceWarning\n");
                    styledTypes.add(portNotIface);
                }
            }

            // MAJOR: Boundary, Missing Repository, Port Uncovered, Layer Violation
            for (String boundary : boundaryViolations) {
                if (!styledTypes.contains(boundary)) {
                    sb.append("    class ").append(boundary).append(":::BoundaryWarning\n");
                    styledTypes.add(boundary);
                }
            }
            for (String missingRepo : missingRepositoryTypes) {
                if (!styledTypes.contains(missingRepo)) {
                    sb.append("    class ").append(missingRepo).append(":::MissingRepositoryInfo\n");
                    styledTypes.add(missingRepo);
                }
            }
            for (String portUncov : portUncoveredTypes) {
                if (!styledTypes.contains(portUncov)) {
                    sb.append("    class ").append(portUncov).append(":::PortUncoveredWarning\n");
                    styledTypes.add(portUncov);
                }
            }
            for (String layerViol : layerViolationTypes) {
                if (!styledTypes.contains(layerViol)) {
                    sb.append("    class ").append(layerViol).append(":::LayerViolationWarning\n");
                    styledTypes.add(layerViol);
                }
            }

            // MINOR: Event Naming (lowest priority)
            for (String eventNaming : eventNamingTypes) {
                if (!styledTypes.contains(eventNaming)) {
                    sb.append("    class ").append(eventNaming).append(":::EventNamingWarning\n");
                    styledTypes.add(eventNaming);
                }
            }

            // Define all used styles
            // BLOCKER
            if (!cycleParticipants.isEmpty()) {
                sb.append("    classDef Alert stroke:#FF5978,fill:#FFDFE5,color:#8E2236,stroke-width:2px\n");
            }
            // CRITICAL
            if (!mutableVOs.isEmpty()) {
                sb.append("    classDef MutableWarning stroke:#FF9800,fill:#FFF3E0,color:#E65100,stroke-width:2px\n");
            }
            if (!impureTypes.isEmpty()) {
                sb.append("    classDef ImpurityWarning stroke:#9C27B0,fill:#F3E5F5,color:#6A1B9A,stroke-width:2px\n");
            }
            if (!missingIdentityTypes.isEmpty()) {
                sb.append(
                        "    classDef MissingIdentityWarning stroke:#FBC02D,fill:#FFFDE7,color:#F57F17,stroke-width:2px\n");
            }
            if (!dependencyInversionTypes.isEmpty()) {
                sb.append(
                        "    classDef DependencyInversionWarning stroke:#FFB300,fill:#FFF8E1,color:#FF6F00,stroke-width:2px\n");
            }
            if (!portNotInterfaceTypes.isEmpty()) {
                sb.append(
                        "    classDef PortNotInterfaceWarning stroke:#8D6E63,fill:#EFEBE9,color:#4E342E,stroke-width:2px\n");
            }
            // MAJOR
            if (!boundaryViolations.isEmpty()) {
                sb.append("    classDef BoundaryWarning stroke:#E53935,fill:#FFEBEE,color:#B71C1C,stroke-width:2px\n");
            }
            if (!missingRepositoryTypes.isEmpty()) {
                sb.append(
                        "    classDef MissingRepositoryInfo stroke:#1976D2,fill:#E3F2FD,color:#0D47A1,stroke-width:2px\n");
            }
            if (!portUncoveredTypes.isEmpty()) {
                sb.append(
                        "    classDef PortUncoveredWarning stroke:#00897B,fill:#E0F2F1,color:#004D40,stroke-width:2px\n");
            }
            if (!layerViolationTypes.isEmpty()) {
                sb.append(
                        "    classDef LayerViolationWarning stroke:#616161,fill:#EEEEEE,color:#212121,stroke-width:2px\n");
            }
            // MINOR
            if (!eventNamingTypes.isEmpty()) {
                sb.append(
                        "    classDef EventNamingWarning stroke:#00ACC1,fill:#E0F7FA,color:#006064,stroke-width:2px\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Builds a domain model class diagram (backward compatibility).
     *
     * @param components component details
     * @param relationships relationships including cycles
     * @return Mermaid classDiagram code (without code fence)
     */
    public String build(ComponentDetails components, List<Relationship> relationships) {
        return build(components, relationships, List.of());
    }

    /**
     * Returns the Mermaid arrow style for a relationship type.
     *
     * @param type the relationship type string
     * @return the Mermaid arrow syntax
     * @since 5.0.0
     */
    private String getArrowForRelationType(String type) {
        return switch (type) {
            case "extends" -> "--|>";
            case "implements", "adapts" -> "..|>";
            case "references", "uses", "persists-via", "emits", "exposes", "handles" -> "..>";
            default -> "-->";
        };
    }

    /**
     * Returns the label to display on a relationship arrow, or null if no label needed.
     *
     * @param type the relationship type string
     * @return the label string, or null
     * @since 5.0.0
     */
    private String getLabelForRelationType(String type) {
        return switch (type) {
            case "persists-via" -> "persists";
            case "emits" -> "emits";
            case "adapts" -> "adapts";
            case "exposes" -> "exposes";
            case "handles" -> "handles";
            // No label for common relationships
            case "references", "uses", "extends", "implements" -> null;
            default -> null;
        };
    }

    /**
     * Extracts simple type name from fully qualified name.
     */
    private String simpleTypeName(String qualifiedName) {
        if (qualifiedName == null) {
            return "unknown";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Appends field details to the diagram output.
     *
     * <p>Each field is rendered in Mermaid format on its own line.
     *
     * @param sb the string builder to append to
     * @param fieldDetails the list of field details
     * @return true if field details were appended, false otherwise
     * @since 5.0.0
     */
    private boolean appendFieldDetails(StringBuilder sb, List<FieldDetail> fieldDetails) {
        if (fieldDetails == null || fieldDetails.isEmpty()) {
            return false;
        }
        for (FieldDetail field : fieldDetails) {
            sb.append("        ").append(field.toMermaid()).append("\n");
        }
        return true;
    }

    /**
     * Appends method details to the diagram output.
     *
     * <p>Each method is rendered in Mermaid format on its own line.
     *
     * @param sb the string builder to append to
     * @param methodDetails the list of method details
     * @since 5.0.0
     */
    private void appendMethodDetails(StringBuilder sb, List<MethodDetail> methodDetails) {
        if (methodDetails == null || methodDetails.isEmpty()) {
            return;
        }
        for (MethodDetail method : methodDetails) {
            sb.append("        ").append(method.toMermaid()).append("\n");
        }
    }
}
