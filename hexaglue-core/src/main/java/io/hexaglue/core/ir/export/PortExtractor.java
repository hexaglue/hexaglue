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

package io.hexaglue.core.ir.export;

import io.hexaglue.core.classification.ClassificationResult;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.ApplicationGraph;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.PortMethod;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts port-specific information from interface nodes.
 *
 * <p>This class handles extraction of:
 * <ul>
 *   <li>Managed types (domain types used in port signatures)</li>
 *   <li>Primary managed type (first aggregate/entity type)</li>
 *   <li>Port methods with their signatures</li>
 * </ul>
 */
final class PortExtractor {

    private final TypeConverter typeConverter;

    PortExtractor() {
        this.typeConverter = new TypeConverter();
    }

    /**
     * Extracts the types managed by a port interface.
     *
     * <p>Managed types are domain types that appear in the port's method signatures
     * as return types or parameters.
     *
     * @param graph the application graph
     * @param portNode the port type node
     * @return list of fully qualified names of managed types
     */
    List<String> extractManagedTypes(ApplicationGraph graph, TypeNode portNode) {
        Set<String> managedTypes = new LinkedHashSet<>();

        List<MethodNode> methods = graph.methodsOf(portNode);
        for (MethodNode method : methods) {
            // Return type
            addManagedType(method.returnType(), graph, managedTypes);

            // Parameters
            for (ParameterInfo param : method.parameters()) {
                addManagedType(param.type(), graph, managedTypes);
            }
        }

        return new ArrayList<>(managedTypes);
    }

    private void addManagedType(TypeRef typeRef, ApplicationGraph graph, Set<String> managedTypes) {
        // Unwrap collections and optionals
        TypeRef elementType = typeRef.unwrapElement();
        String typeName = elementType.rawQualifiedName();

        // Only include application types (not JDK types)
        if (graph.typeNode(typeName).isPresent()) {
            managedTypes.add(typeName);
        }
    }

    /**
     * Extracts method information from a port interface.
     *
     * @param graph the application graph
     * @param portNode the port type node
     * @return list of port methods
     */
    List<PortMethod> extractPortMethods(ApplicationGraph graph, TypeNode portNode) {
        return graph.methodsOf(portNode).stream().map(this::toPortMethod).toList();
    }

    private PortMethod toPortMethod(MethodNode method) {
        List<String> paramTypes = method.parameters().stream()
                .map(p -> p.type().rawQualifiedName())
                .toList();

        return new PortMethod(method.simpleName(), method.returnType().rawQualifiedName(), paramTypes);
    }

    /**
     * Extracts the primary managed type for a port.
     *
     * <p>The primary managed type is the first AGGREGATE_ROOT or ENTITY type
     * found in the managed types list. Value objects and identifiers are skipped.
     *
     * @param managedTypes the list of all managed type qualified names
     * @param domainClassifications map of domain type classifications by node ID
     * @return the qualified name of the primary managed type, or null if none
     */
    String extractPrimaryManagedType(
            List<String> managedTypes, Map<NodeId, ClassificationResult> domainClassifications) {
        for (String typeName : managedTypes) {
            NodeId nodeId = NodeId.type(typeName);
            ClassificationResult classification = domainClassifications.get(nodeId);

            if (classification == null) {
                continue;
            }

            DomainKind kind = typeConverter.toDomainKind(classification.kind());

            // Skip value objects and identifiers - only consider aggregates and entities
            if (kind == DomainKind.AGGREGATE_ROOT || kind == DomainKind.ENTITY) {
                return typeName;
            }
        }

        return null;
    }
}
