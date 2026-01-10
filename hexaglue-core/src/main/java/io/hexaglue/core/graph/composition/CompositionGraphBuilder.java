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

package io.hexaglue.core.graph.composition;

import io.hexaglue.core.frontend.JavaField;
import io.hexaglue.core.frontend.JavaSemanticModel;
import io.hexaglue.core.frontend.JavaType;
import io.hexaglue.core.frontend.TypeRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builder for constructing composition graphs from semantic models.
 *
 * <p>This builder analyzes field declarations in domain types to build a graph
 * of composition and reference relationships. The builder:
 * <ol>
 *   <li>Creates nodes for each domain type with structural characteristics</li>
 *   <li>Analyzes fields to determine relationship types</li>
 *   <li>Creates edges representing composition, reference-by-id, or direct-reference</li>
 * </ol>
 *
 * <p>The resulting graph is used for deterministic classification and anomaly detection.
 *
 * @since 3.0.0
 */
public final class CompositionGraphBuilder {

    /**
     * Collection types that indicate MANY cardinality.
     */
    private static final Set<String> COLLECTION_TYPES = Set.of(
            "java.util.Collection",
            "java.util.List",
            "java.util.Set",
            "java.util.Queue",
            "java.util.Deque",
            "java.lang.Iterable");

    /**
     * Creates a new builder.
     */
    public CompositionGraphBuilder() {}

    /**
     * Builds a composition graph from the given semantic model.
     *
     * <p>The builder processes all types in the model to extract composition
     * and reference relationships based on field analysis.
     *
     * @param model the semantic model containing domain types
     * @return the constructed composition graph
     */
    public CompositionGraph build(JavaSemanticModel model) {
        Objects.requireNonNull(model, "model required");

        Map<String, CompositionNode> nodes = new HashMap<>();
        List<CompositionEdge> edges = new ArrayList<>();

        // Step 1: Create nodes for all types
        for (JavaType type : model.types()) {
            CompositionNode node = createNode(type);
            nodes.put(node.qualifiedName(), node);
        }

        // Step 2: Create edges from field analysis
        for (JavaType type : model.types()) {
            String sourceQName = type.qualifiedName();

            for (JavaField field : type.fields()) {
                // Skip static fields
                if (field.isStatic()) {
                    continue;
                }

                analyzeField(sourceQName, field, nodes, edges);
            }
        }

        return new CompositionGraph(nodes, edges);
    }

    /**
     * Creates a composition node for a type.
     *
     * @param type the Java type
     * @return the composition node
     */
    private CompositionNode createNode(JavaType type) {
        String qualifiedName = type.qualifiedName();
        String simpleName = type.simpleName();
        boolean hasIdentity = type.hasIdField();
        boolean isRecord = type.isRecord();

        // Detect if it's an ID wrapper
        boolean isIdWrapper =
                type.simpleName().endsWith("Id") || type.simpleName().endsWith("ID");
        // Note: We can't use idWrapperDiscriminator here because JavaType is not CtType
        // This will be refined in the actual Spoon-based implementation

        if (isIdWrapper) {
            return CompositionNode.idWrapper(qualifiedName, simpleName, isRecord);
        } else if (hasIdentity) {
            return CompositionNode.withIdentity(qualifiedName, simpleName);
        } else {
            return CompositionNode.valueObject(qualifiedName, simpleName);
        }
    }

    /**
     * Analyzes a field to create appropriate edges.
     *
     * @param sourceQName the source type qualified name
     * @param field       the field to analyze
     * @param nodes       the map of nodes
     * @param edges       the list to add edges to
     */
    private void analyzeField(
            String sourceQName, JavaField field, Map<String, CompositionNode> nodes, List<CompositionEdge> edges) {

        TypeRef fieldType = field.type();
        String fieldName = field.simpleName();

        // Determine if the field is a collection
        boolean isCollection = isCollectionType(fieldType);
        Cardinality cardinality = isCollection ? Cardinality.MANY : Cardinality.ONE;

        // Get the actual element type (unwrap collection if needed)
        String targetTypeName = isCollection ? extractElementType(fieldType) : fieldType.rawQualifiedName();

        // Skip if target is not in our domain model
        if (!nodes.containsKey(targetTypeName)) {
            return;
        }

        CompositionNode targetNode = nodes.get(targetTypeName);

        // Determine relationship type
        RelationType relationType = determineRelationType(targetNode);

        // Create edge
        CompositionEdge edge = new CompositionEdge(sourceQName, targetTypeName, relationType, cardinality, fieldName);
        edges.add(edge);
    }

    /**
     * Checks if a type reference is a collection type.
     *
     * @param typeRef the type reference
     * @return true if the type is a collection
     */
    private boolean isCollectionType(TypeRef typeRef) {
        String qualifiedName = typeRef.rawQualifiedName();

        // Check array
        if (typeRef.isArray()) {
            return true;
        }

        // Check collection interfaces
        for (String collectionType : COLLECTION_TYPES) {
            if (qualifiedName.equals(collectionType) || qualifiedName.startsWith(collectionType + "<")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts the element type from a collection type.
     *
     * @param typeRef the collection type reference
     * @return the qualified name of the element type
     */
    private String extractElementType(TypeRef typeRef) {
        // Handle arrays
        if (typeRef.isArray()) {
            // Remove [] suffix
            String qname = typeRef.rawQualifiedName();
            return qname.replace("[]", "");
        }

        // Handle generics - this is a simplified version
        // In real implementation, we'd use Spoon's type argument API
        List<TypeRef> typeArguments = typeRef.arguments();
        if (!typeArguments.isEmpty()) {
            return typeArguments.get(0).rawQualifiedName();
        }

        // Fallback - couldn't determine element type
        return typeRef.rawQualifiedName();
    }

    /**
     * Determines the relationship type based on the target node characteristics.
     *
     * @param targetNode the target node
     * @return the relationship type
     */
    private RelationType determineRelationType(CompositionNode targetNode) {
        // If target is an ID wrapper, this is a reference by ID
        if (targetNode.isIdWrapper()) {
            return RelationType.REFERENCE_BY_ID;
        }

        // If target has no identity, it's likely a value object (composition)
        if (!targetNode.hasIdentity()) {
            return RelationType.COMPOSITION;
        }

        // Target has identity but is not an ID wrapper
        // This could be either composition (entity within aggregate) or direct reference (smell)
        // For now, we'll default to direct reference and let the classifier refine this
        return RelationType.DIRECT_REFERENCE;
    }

    /**
     * Creates a new builder instance.
     *
     * @return a new builder instance
     */
    public static CompositionGraphBuilder create() {
        return new CompositionGraphBuilder();
    }
}
