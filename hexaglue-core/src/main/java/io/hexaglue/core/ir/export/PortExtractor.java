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
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.MethodNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.ParameterInfo;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.spi.ir.Cardinality;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.Identity;
import io.hexaglue.spi.ir.MethodParameter;
import io.hexaglue.spi.ir.PortMethod;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts port-specific information from interface nodes.
 *
 * <p>This class handles extraction of:
 * <ul>
 *   <li>Managed types (domain types used in port signatures)</li>
 *   <li>Primary managed type (first aggregate/entity type)</li>
 *   <li>Port methods with their signatures and classification</li>
 * </ul>
 *
 * @since 3.0.0 Uses {@link PortMethodClassifier} for method classification
 */
final class PortExtractor {

    private final TypeConverter typeConverter;
    private final PortMethodClassifier methodClassifier;

    PortExtractor() {
        this.typeConverter = new TypeConverter();
        this.methodClassifier = new PortMethodClassifier();
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
        return extractPortMethods(graph, portNode, Optional.empty());
    }

    /**
     * Extracts method information from a port interface with aggregate identity context.
     *
     * @param graph the application graph
     * @param portNode the port type node
     * @param aggregateIdentity the identity of the primary managed aggregate, if available
     * @return list of port methods with classification
     * @since 3.0.0
     */
    List<PortMethod> extractPortMethods(
            ApplicationGraph graph, TypeNode portNode, Optional<Identity> aggregateIdentity) {
        return graph.methodsOf(portNode).stream()
                .map(method -> toPortMethod(method, aggregateIdentity))
                .toList();
    }

    private PortMethod toPortMethod(MethodNode method, Optional<Identity> aggregateIdentity) {
        // Classify the method using the classifier
        PortMethodClassifier.ClassificationResult classification =
                methodClassifier.classify(method.simpleName(), method.parameters(), aggregateIdentity);

        // Convert return type to SPI TypeRef
        io.hexaglue.spi.ir.TypeRef returnType = convertTypeRef(method.returnType());

        // Convert parameters to SPI MethodParameter with identity detection
        List<MethodParameter> parameters = method.parameters().stream()
                .map(p -> convertParameter(p, aggregateIdentity))
                .toList();

        // Extract method annotations
        Set<String> annotations =
                method.annotations().stream().map(AnnotationRef::qualifiedName).collect(Collectors.toSet());

        return new PortMethod(
                method.simpleName(),
                returnType,
                parameters,
                classification.kind(),
                classification.targetProperties(),
                classification.modifiers(),
                classification.limitSize(),
                classification.orderByProperty(),
                annotations);
    }

    /**
     * Converts a Core TypeRef to an SPI TypeRef.
     */
    private io.hexaglue.spi.ir.TypeRef convertTypeRef(TypeRef coreType) {
        if (coreType.arguments().isEmpty()) {
            // Simple type
            return io.hexaglue.spi.ir.TypeRef.of(coreType.rawQualifiedName());
        }

        // Parameterized type - recursively convert type arguments
        List<io.hexaglue.spi.ir.TypeRef> spiArguments =
                coreType.arguments().stream().map(this::convertTypeRef).toList();

        Cardinality cardinality = inferCardinality(coreType);

        return io.hexaglue.spi.ir.TypeRef.parameterized(coreType.rawQualifiedName(), cardinality, spiArguments);
    }

    private Cardinality inferCardinality(TypeRef coreType) {
        if (coreType.isOptionalLike()) {
            return Cardinality.OPTIONAL;
        }
        if (coreType.isCollectionLike()) {
            // Check if it's a Stream
            if (coreType.rawQualifiedName().equals("java.util.stream.Stream")) {
                return Cardinality.STREAM;
            }
            return Cardinality.COLLECTION;
        }
        return Cardinality.SINGLE;
    }

    /**
     * Converts a Core ParameterInfo to an SPI MethodParameter.
     */
    private MethodParameter convertParameter(ParameterInfo coreParam, Optional<Identity> aggregateIdentity) {
        io.hexaglue.spi.ir.TypeRef spiType = convertTypeRef(coreParam.type());

        // Detect if this parameter is the aggregate's identity
        boolean isIdentity = aggregateIdentity
                .map(id -> isIdentityType(coreParam.type(), id))
                .orElse(false);

        // Extract parameter annotations
        Set<String> annotations = coreParam.annotations().stream()
                .map(AnnotationRef::qualifiedName)
                .collect(Collectors.toSet());

        return new MethodParameter(coreParam.name(), spiType, isIdentity, annotations);
    }

    private boolean isIdentityType(TypeRef paramType, Identity identity) {
        String paramTypeName = paramType.rawQualifiedName();
        return paramTypeName.equals(identity.type().qualifiedName())
                || paramTypeName.equals(identity.unwrappedType().qualifiedName());
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
