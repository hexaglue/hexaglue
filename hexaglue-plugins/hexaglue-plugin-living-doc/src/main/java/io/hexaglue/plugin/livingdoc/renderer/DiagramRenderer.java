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

package io.hexaglue.plugin.livingdoc.renderer;

import io.hexaglue.plugin.livingdoc.mermaid.MermaidBuilder;
import io.hexaglue.plugin.livingdoc.model.DomainTypeDoc;
import io.hexaglue.plugin.livingdoc.model.PortDoc;
import io.hexaglue.plugin.livingdoc.model.PropertyDoc;
import io.hexaglue.plugin.livingdoc.model.RelationDoc;
import io.hexaglue.plugin.livingdoc.util.TypeDisplayUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders Mermaid diagrams for architecture documentation.
 *
 * @since 4.0.0
 * @since 5.0.0 - Added configurable maxPropertiesInDiagram
 */
public final class DiagramRenderer {

    private final int maxPropertiesInDiagram;

    /**
     * Creates a renderer with default settings (max 5 properties per class).
     */
    public DiagramRenderer() {
        this(5);
    }

    /**
     * Creates a renderer with configurable property limit.
     *
     * @param maxPropertiesInDiagram maximum number of properties to show per class in diagrams
     */
    public DiagramRenderer(int maxPropertiesInDiagram) {
        this.maxPropertiesInDiagram = maxPropertiesInDiagram;
    }

    public String renderDomainClassDiagram(List<DomainTypeDoc> types) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("classDiagram\n");

        // Generate classes
        for (DomainTypeDoc type : types) {
            generateClassDefinition(sb, type);
        }

        // Generate relationships
        Map<String, DomainTypeDoc> typeMap = new HashMap<>();
        for (DomainTypeDoc type : types) {
            typeMap.put(type.debug().qualifiedName(), type);
        }

        for (DomainTypeDoc type : types) {
            generateRelationships(sb, type, typeMap);
        }

        sb.append("```\n\n");
        return sb.toString();
    }

    public String renderAggregateDiagram(DomainTypeDoc aggregate, Map<String, DomainTypeDoc> allTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("classDiagram\n");

        // The aggregate root
        generateClassDefinition(sb, aggregate);

        // Related types from properties
        for (PropertyDoc prop : aggregate.properties()) {
            // For collections, use the element type from typeArguments
            String propTypeName;
            if (prop.cardinality().equals("COLLECTION") && !prop.typeArguments().isEmpty()) {
                propTypeName = prop.typeArguments().get(0);
            } else {
                propTypeName = extractBaseType(prop.type());
            }
            DomainTypeDoc relatedType = allTypes.get(propTypeName);

            if (relatedType != null) {
                generateClassDefinition(sb, relatedType);

                String sourceClass = MermaidBuilder.sanitizeId(aggregate.name());
                String targetClass = MermaidBuilder.sanitizeId(relatedType.name());

                if (prop.cardinality().equals("COLLECTION")) {
                    sb.append("    ")
                            .append(sourceClass)
                            .append(" \"1\" *-- \"*\" ")
                            .append(targetClass)
                            .append("\n");
                } else {
                    sb.append("    ")
                            .append(sourceClass)
                            .append(" *-- ")
                            .append(targetClass)
                            .append("\n");
                }
            }
        }

        sb.append("```\n\n");
        return sb.toString();
    }

    public String renderPortsFlowDiagram(
            List<PortDoc> drivingPorts, List<PortDoc> drivenPorts, List<DomainTypeDoc> aggregates) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("flowchart LR\n");

        // External actors
        sb.append("    subgraph External[\"External\"]\n");
        sb.append("        User([User])\n");
        sb.append("        API([API Client])\n");
        sb.append("    end\n\n");

        // Driving ports - always show subgraph to reflect hexagonal architecture target
        sb.append("    subgraph Driving[\"Driving Ports\"]\n");
        if (drivingPorts.isEmpty()) {
            sb.append("        NoDriving[\"(none)\"]\n");
        } else {
            for (PortDoc port : drivingPorts) {
                sb.append("        ")
                        .append(MermaidBuilder.sanitizeId(port.name()))
                        .append("[")
                        .append(port.name())
                        .append("]\n");
            }
        }
        sb.append("    end\n\n");

        // Domain
        sb.append("    subgraph Domain[\"Domain\"]\n");
        if (aggregates.isEmpty()) {
            sb.append("        DomainLogic[Domain Logic]\n");
        } else {
            for (DomainTypeDoc agg : aggregates) {
                sb.append("        ")
                        .append(MermaidBuilder.sanitizeId(agg.name()))
                        .append("{{")
                        .append(agg.name())
                        .append("}}\n");
            }
        }
        sb.append("    end\n\n");

        // Driven ports - always show subgraph to reflect hexagonal architecture target
        sb.append("    subgraph Driven[\"Driven Ports\"]\n");
        if (drivenPorts.isEmpty()) {
            sb.append("        NoDriven[\"(none)\"]\n");
        } else {
            for (PortDoc port : drivenPorts) {
                sb.append("        ")
                        .append(MermaidBuilder.sanitizeId(port.name()))
                        .append("[")
                        .append(port.name())
                        .append("]\n");
            }
        }
        sb.append("    end\n\n");

        // Infrastructure
        sb.append("    subgraph Infra[\"Infrastructure\"]\n");
        sb.append("        DB[(Database)]\n");
        sb.append("        ExtAPI[External APIs]\n");
        sb.append("    end\n\n");

        // Connections
        sb.append("    User --> Driving\n");
        sb.append("    API --> Driving\n");
        sb.append("    Driving --> Domain\n");
        sb.append("    Domain --> Driven\n");
        sb.append("    Driven --> DB\n");
        sb.append("    Driven --> ExtAPI\n");

        sb.append("```\n\n");
        return sb.toString();
    }

    public String renderDependenciesDiagram(
            List<PortDoc> drivingPorts, List<PortDoc> drivenPorts, List<DomainTypeDoc> aggregates) {
        StringBuilder sb = new StringBuilder();
        sb.append("```mermaid\n");
        sb.append("flowchart TB\n");

        // Driving Adapters (external)
        sb.append("    subgraph DrivingAdapters[\"Driving Adapters\"]\n");
        sb.append("        Controller[REST Controller]\n");
        sb.append("        CLI[CLI]\n");
        sb.append("    end\n\n");

        // Driving ports - always show subgraph to reflect hexagonal architecture target
        sb.append("    subgraph DrivingPorts[\"Driving Ports\"]\n");
        if (drivingPorts.isEmpty()) {
            sb.append("        NoDrivingPorts[[\"(none)\"]]\n");
        } else {
            for (PortDoc port : drivingPorts) {
                sb.append("        ")
                        .append(MermaidBuilder.sanitizeId(port.name()))
                        .append("[[")
                        .append(port.name())
                        .append("]]\n");
            }
        }
        sb.append("    end\n\n");

        // Domain
        sb.append("    subgraph Domain[\"Domain\"]\n");
        if (aggregates.isEmpty()) {
            sb.append("        DomainLogic[Domain Logic]\n");
        } else {
            for (DomainTypeDoc agg : aggregates) {
                sb.append("        ")
                        .append(MermaidBuilder.sanitizeId(agg.name()))
                        .append("{{")
                        .append(agg.name())
                        .append("}}\n");
            }
        }
        sb.append("    end\n\n");

        // Driven ports - always show subgraph to reflect hexagonal architecture target
        sb.append("    subgraph DrivenPorts[\"Driven Ports\"]\n");
        if (drivenPorts.isEmpty()) {
            sb.append("        NoDrivenPorts[[\"(none)\"]]\n");
        } else {
            for (PortDoc port : drivenPorts) {
                sb.append("        ")
                        .append(MermaidBuilder.sanitizeId(port.name()))
                        .append("[[")
                        .append(port.name())
                        .append("]]\n");
            }
        }
        sb.append("    end\n\n");

        // Driven Adapters (external)
        sb.append("    subgraph DrivenAdapters[\"Driven Adapters\"]\n");
        sb.append("        JpaRepo[JPA Repository]\n");
        sb.append("        ExtClient[External Client]\n");
        sb.append("    end\n\n");

        // Dependencies - all arrows point toward the domain
        sb.append("    DrivingAdapters --> DrivingPorts\n");
        sb.append("    DrivingPorts --> Domain\n");
        sb.append("    DrivenAdapters --> DrivenPorts\n");
        sb.append("    DrivenPorts --> Domain\n");

        sb.append("```\n\n");
        return sb.toString();
    }

    private void generateClassDefinition(StringBuilder sb, DomainTypeDoc type) {
        String className = MermaidBuilder.sanitizeId(type.name());

        // Class declaration with stereotype
        sb.append("    class ").append(className).append(" {\n");

        // Stereotype annotation
        String stereotype = TypeDisplayUtil.getStereotype(type.kind());
        if (stereotype != null) {
            sb.append("        <<").append(stereotype).append(">>\n");
        }

        // Identity field
        if (type.identity() != null) {
            sb.append("        +")
                    .append(type.identity().type())
                    .append(" ")
                    .append(type.identity().fieldName())
                    .append("\n");
        }

        // Properties (limit to avoid cluttering)
        List<PropertyDoc> props = type.properties();
        int maxProps = Math.min(props.size(), maxPropertiesInDiagram);
        for (int i = 0; i < maxProps; i++) {
            PropertyDoc prop = props.get(i);
            if (!prop.isIdentity()) {
                String propType = formatPropertyTypeForDiagram(prop);
                sb.append("        +")
                        .append(propType)
                        .append(" ")
                        .append(prop.name())
                        .append("\n");
            }
        }
        if (props.size() > maxProps) {
            sb.append("        ...\n");
        }

        sb.append("    }\n");
    }

    private void generateRelationships(StringBuilder sb, DomainTypeDoc type, Map<String, DomainTypeDoc> typeMap) {
        String sourceClass = MermaidBuilder.sanitizeId(type.name());

        // From explicit relations
        for (RelationDoc rel : type.relations()) {
            String targetClass = MermaidBuilder.sanitizeId(rel.targetType());

            String arrow = getRelationArrow(rel.kind());
            sb.append("    ")
                    .append(sourceClass)
                    .append(" ")
                    .append(arrow)
                    .append(" ")
                    .append(targetClass)
                    .append("\n");
        }

        // From properties referencing other domain types
        for (PropertyDoc prop : type.properties()) {
            if (!prop.isIdentity() && prop.relationInfo() == null) {
                // For collections, use the element type from typeArguments
                String propTypeName;
                if (prop.cardinality().equals("COLLECTION")
                        && !prop.typeArguments().isEmpty()) {
                    propTypeName = prop.typeArguments().get(0);
                } else {
                    propTypeName = extractBaseType(prop.type());
                }
                DomainTypeDoc targetType = typeMap.get(propTypeName);

                if (targetType != null) {
                    String targetClass = MermaidBuilder.sanitizeId(targetType.name());
                    if (!sourceClass.equals(targetClass)) {
                        if (prop.cardinality().equals("COLLECTION")) {
                            sb.append("    ")
                                    .append(sourceClass)
                                    .append(" \"1\" --o \"*\" ")
                                    .append(targetClass)
                                    .append(" : ")
                                    .append(prop.name())
                                    .append("\n");
                        } else {
                            sb.append("    ")
                                    .append(sourceClass)
                                    .append(" --> ")
                                    .append(targetClass)
                                    .append(" : ")
                                    .append(prop.name())
                                    .append("\n");
                        }
                    }
                }
            }
        }
    }

    private String formatPropertyTypeForDiagram(PropertyDoc prop) {
        String simpleName = TypeDisplayUtil.simplifyType(prop.type());

        if (prop.cardinality().equals("COLLECTION")) {
            if (prop.isParameterized() && !prop.typeArguments().isEmpty()) {
                String elementType =
                        TypeDisplayUtil.simplifyType(prop.typeArguments().get(0));
                return "List~" + elementType + "~";
            }
        }
        return simpleName;
    }

    private String extractBaseType(String qualifiedType) {
        // For parameterized types like List<com.example.Order>, extract com.example.Order
        if (qualifiedType.contains("<")) {
            int start = qualifiedType.indexOf('<') + 1;
            int end = qualifiedType.lastIndexOf('>');
            if (end > start) {
                return qualifiedType.substring(start, end);
            }
        }
        return qualifiedType;
    }

    private String getRelationArrow(String kind) {
        return switch (kind) {
            case "ONE_TO_ONE" -> "\"1\" -- \"1\"";
            case "ONE_TO_MANY" -> "\"1\" --o \"*\"";
            case "MANY_TO_ONE" -> "\"*\" o-- \"1\"";
            case "MANY_TO_MANY" -> "\"*\" --o \"*\"";
            case "EMBEDDED" -> "*--";
            case "ELEMENT_COLLECTION" -> "\"1\" *-- \"*\"";
            default -> "-->";
        };
    }
}
