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

package io.hexaglue.core.analysis;

import io.hexaglue.arch.ElementKind;
import io.hexaglue.arch.model.ir.CascadeType;
import io.hexaglue.arch.model.ir.DomainRelation;
import io.hexaglue.arch.model.ir.FetchType;
import io.hexaglue.arch.model.ir.RelationKind;
import io.hexaglue.core.classification.ClassificationContext;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Analyzes domain type fields to detect relationships between types.
 *
 * <p>Uses classification context from domain classification (Pass 1) to
 * determine the semantic meaning of relationships:
 * <ul>
 *   <li>Field referencing an AGGREGATE_ROOT → MANY_TO_ONE</li>
 *   <li>Collection of ENTITY → ONE_TO_MANY</li>
 *   <li>Field of VALUE_OBJECT → EMBEDDED</li>
 *   <li>Collection of VALUE_OBJECT → ELEMENT_COLLECTION</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * RelationAnalyzer analyzer = new RelationAnalyzer();
 * List<DomainRelation> relations = analyzer.analyzeRelations(orderType, query, context);
 * }</pre>
 */
public final class RelationAnalyzer {

    private final MappedByDetector mappedByDetector;
    private final CascadeInference cascadeInference;

    public RelationAnalyzer() {
        this(new MappedByDetector(), new CascadeInference());
    }

    public RelationAnalyzer(MappedByDetector mappedByDetector, CascadeInference cascadeInference) {
        this.mappedByDetector = mappedByDetector;
        this.cascadeInference = cascadeInference;
    }

    /**
     * Analyzes all fields of a type to detect domain relationships.
     *
     * @param type the type to analyze
     * @param query the graph query interface
     * @param context the classification context with domain classifications
     * @return list of domain relations for this type
     */
    public List<DomainRelation> analyzeRelations(TypeNode type, GraphQuery query, ClassificationContext context) {
        List<DomainRelation> relations = new ArrayList<>();

        for (FieldNode field : query.fieldsOf(type)) {
            analyzeField(type, field, query, context).ifPresent(relations::add);
        }

        return relations;
    }

    private Optional<DomainRelation> analyzeField(
            TypeNode ownerType, FieldNode field, GraphQuery query, ClassificationContext context) {

        TypeRef fieldType = field.type();

        // Skip identity fields
        if (field.looksLikeIdentity()) {
            return Optional.empty();
        }

        // Case 1: Map type (Map<K, V>)
        if (fieldType.isMapLike()) {
            return analyzeMapField(ownerType, field, fieldType, query, context);
        }

        // Case 2: Collection type (List<X>, Set<X>, etc.)
        if (fieldType.isCollectionLike()) {
            return analyzeCollectionField(ownerType, field, fieldType, query, context);
        }

        // Case 3: Optional type (Optional<X>)
        if (fieldType.isOptionalLike()) {
            return analyzeOptionalField(ownerType, field, fieldType, query, context);
        }

        // Case 4: Simple reference
        return analyzeSimpleField(ownerType, field, fieldType, query, context);
    }

    private Optional<DomainRelation> analyzeCollectionField(
            TypeNode ownerType, FieldNode field, TypeRef fieldType, GraphQuery query, ClassificationContext context) {

        // Get the element type from the collection
        TypeRef elementType = fieldType.unwrapElement();
        if (elementType == null || elementType.equals(fieldType)) {
            // Not a parameterized collection or couldn't unwrap
            return Optional.empty();
        }

        String elementTypeFqn = elementType.rawQualifiedName();
        NodeId elementId = NodeId.type(elementTypeFqn);

        // Check if the element type is a known domain type
        Optional<TypeNode> targetType = query.type(elementTypeFqn);
        if (targetType.isEmpty()) {
            // Element type is not in our graph (e.g., String, Integer)
            return Optional.empty();
        }

        // Use heuristic detection with targetType for unclassified records/enums
        ElementKind targetKind = getElementKind(elementId, context, targetType, query);
        if (targetKind == null) {
            return Optional.empty();
        }

        // VALUE_OBJECT in collection → ELEMENT_COLLECTION
        if (targetKind == ElementKind.VALUE_OBJECT) {
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ELEMENT_COLLECTION,
                    elementTypeFqn,
                    ElementKind.VALUE_OBJECT,
                    null,
                    CascadeType.NONE,
                    FetchType.EAGER,
                    false));
        }

        // AGGREGATE_ROOT in collection → MANY_TO_MANY (inter-aggregate relationship)
        // This is a DDD-semantic: each aggregate root is independently managed,
        // so a collection of them represents a many-to-many association
        if (targetKind == ElementKind.AGGREGATE_ROOT) {
            String mappedBy = mappedByDetector
                    .detectMappedBy(ownerType, field, elementType, query)
                    .orElse(null);

            // Inter-aggregate relations should NOT cascade
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_MANY,
                    elementTypeFqn,
                    ElementKind.AGGREGATE_ROOT,
                    mappedBy,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false));
        }

        // ENTITY in collection → ONE_TO_MANY (child entities within aggregate)
        if (targetKind == ElementKind.ENTITY) {
            String mappedBy = mappedByDetector
                    .detectMappedBy(ownerType, field, elementType, query)
                    .orElse(null);

            CascadeType cascade =
                    cascadeInference.infer(ownerType, targetType.get(), RelationKind.ONE_TO_MANY, context);

            // Orphan removal only for child entities within same aggregate
            boolean orphanRemoval = cascade == CascadeType.ALL;

            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ONE_TO_MANY,
                    elementTypeFqn,
                    ElementKind.ENTITY,
                    mappedBy,
                    cascade,
                    FetchType.LAZY,
                    orphanRemoval));
        }

        return Optional.empty();
    }

    private Optional<DomainRelation> analyzeOptionalField(
            TypeNode ownerType, FieldNode field, TypeRef fieldType, GraphQuery query, ClassificationContext context) {

        // Get the inner type from the Optional
        TypeRef innerType = fieldType.unwrapElement();
        if (innerType == null || innerType.equals(fieldType)) {
            // Not a parameterized Optional or couldn't unwrap
            return Optional.empty();
        }

        // Delegate to simple field analysis with the unwrapped type
        return analyzeSimpleField(ownerType, field, innerType, query, context);
    }

    private Optional<DomainRelation> analyzeSimpleField(
            TypeNode ownerType, FieldNode field, TypeRef fieldType, GraphQuery query, ClassificationContext context) {

        String typeFqn = fieldType.rawQualifiedName();
        NodeId typeId = NodeId.type(typeFqn);

        // Check if the type is a known domain type
        Optional<TypeNode> targetType = query.type(typeFqn);
        if (targetType.isEmpty()) {
            // Not a domain type in our graph
            return Optional.empty();
        }

        // Use heuristic detection with targetType for unclassified records/enums
        ElementKind targetKind = getElementKind(typeId, context, targetType, query);
        if (targetKind == null) {
            return Optional.empty();
        }

        // VALUE_OBJECT → EMBEDDED (but NOT for enums - they use @Enumerated)
        if (targetKind == ElementKind.VALUE_OBJECT) {
            // Check if the target type is an enum - enums should not be embedded
            // They will be handled with @Enumerated(EnumType.STRING) in JPA
            boolean isEnum = targetType.map(TypeNode::isEnum).orElse(false);
            if (!isEnum) {
                return Optional.of(DomainRelation.embedded(field.simpleName(), typeFqn));
            }
            // Enums are simple fields, no special relation needed
            return Optional.empty();
        }

        // AGGREGATE_ROOT → MANY_TO_ONE (reference to another aggregate)
        if (targetKind == ElementKind.AGGREGATE_ROOT) {
            // References to other aggregates should NOT cascade
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_ONE,
                    typeFqn,
                    ElementKind.AGGREGATE_ROOT,
                    null,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false));
        }

        // ENTITY → ONE_TO_ONE (child entity within aggregate)
        // In DDD, a single reference to a child entity is typically a 1:1 relationship
        // (the child is owned exclusively by this aggregate)
        if (targetKind == ElementKind.ENTITY) {
            CascadeType cascade = cascadeInference.infer(ownerType, targetType.get(), RelationKind.ONE_TO_ONE, context);

            // Detect mappedBy for bidirectional relationships
            String mappedBy = mappedByDetector
                    .detectMappedByForSingleReference(ownerType, field, targetType.get(), query)
                    .orElse(null);

            // Orphan removal for owned child entities with cascade ALL
            boolean orphanRemoval = cascade == CascadeType.ALL;

            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ONE_TO_ONE,
                    typeFqn,
                    ElementKind.ENTITY,
                    mappedBy,
                    cascade,
                    FetchType.LAZY,
                    orphanRemoval));
        }

        return Optional.empty();
    }

    private Optional<DomainRelation> analyzeMapField(
            TypeNode ownerType, FieldNode field, TypeRef fieldType, GraphQuery query, ClassificationContext context) {

        // Map<K, V> - extract value type (second type argument)
        List<TypeRef> arguments = fieldType.arguments();
        if (arguments.size() < 2) {
            // Not a parameterized Map
            return Optional.empty();
        }

        TypeRef valueType = arguments.get(1);
        String valueTypeFqn = valueType.rawQualifiedName();
        NodeId valueId = NodeId.type(valueTypeFqn);

        // Check if the value type is a known domain type
        Optional<TypeNode> targetType = query.type(valueTypeFqn);
        if (targetType.isEmpty()) {
            // Value type is not in our graph
            return Optional.empty();
        }

        // Use heuristic detection with targetType for unclassified records/enums
        ElementKind targetKind = getElementKind(valueId, context, targetType, query);
        if (targetKind == null) {
            return Optional.empty();
        }

        // Map<K, VALUE_OBJECT> → ELEMENT_COLLECTION
        if (targetKind == ElementKind.VALUE_OBJECT) {
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ELEMENT_COLLECTION,
                    valueTypeFqn,
                    ElementKind.VALUE_OBJECT,
                    null,
                    CascadeType.NONE,
                    FetchType.EAGER,
                    false));
        }

        // Map<K, ENTITY> → ONE_TO_MANY (indexed collection)
        if (targetKind == ElementKind.ENTITY) {
            CascadeType cascade =
                    cascadeInference.infer(ownerType, targetType.get(), RelationKind.ONE_TO_MANY, context);

            boolean orphanRemoval = cascade == CascadeType.ALL;

            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ONE_TO_MANY,
                    valueTypeFqn,
                    ElementKind.ENTITY,
                    null,
                    cascade,
                    FetchType.LAZY,
                    orphanRemoval));
        }

        // Map<K, AGGREGATE_ROOT> → MANY_TO_MANY (indexed inter-aggregate)
        if (targetKind == ElementKind.AGGREGATE_ROOT) {
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_MANY,
                    valueTypeFqn,
                    ElementKind.AGGREGATE_ROOT,
                    null,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false));
        }

        return Optional.empty();
    }

    /**
     * Gets the domain kind for a type, with optional heuristic detection.
     *
     * <p>If the type is not in the classification context and a TypeNode is provided,
     * applies heuristics:
     * <ul>
     *   <li>Records → VALUE_OBJECT (immutable data carriers)</li>
     *   <li>Enums → VALUE_OBJECT (fixed set of values)</li>
     *   <li>Classes without identity field → VALUE_OBJECT (data structures without own lifecycle)</li>
     * </ul>
     *
     * @param typeId the type's node ID
     * @param context the classification context
     * @param targetType optional TypeNode for heuristic detection
     * @param query the graph query for field access
     * @return the domain kind, or null if unknown
     */
    private ElementKind getElementKind(
            NodeId typeId, ClassificationContext context, Optional<TypeNode> targetType, GraphQuery query) {
        // First try classification context
        String kind = context.getKind(typeId);
        if (kind != null) {
            try {
                return ElementKind.valueOf(kind);
            } catch (IllegalArgumentException e) {
                // Fall through to heuristics
            }
        }

        // Heuristic: If type is not classified, infer from Java form
        if (targetType.isPresent()) {
            TypeNode node = targetType.get();

            // Records and enums are VALUE_OBJECT by convention
            if (node.isRecord() || node.isEnum()) {
                return ElementKind.VALUE_OBJECT;
            }

            // Classes without identity fields are likely VALUE_OBJECTs
            // This heuristic helps detect embedded collection elements like OrderLine
            if (node.isClass() && !node.isInterface() && !hasIdentityField(node, query)) {
                return ElementKind.VALUE_OBJECT;
            }
        }

        return null;
    }

    /**
     * Checks if a type has its own identity field.
     *
     * <p>A field is considered an identity of the type if:
     * <ul>
     *   <li>It is named exactly "id"</li>
     *   <li>Or it matches the pattern "{typeName}Id" (e.g., "orderId" in Order)</li>
     * </ul>
     *
     * <p>This is more precise than just checking if any field ends with "Id",
     * which would incorrectly flag foreign key references (like "productId" in OrderLine)
     * as identity fields.
     *
     * @param node the type node to check
     * @param query the graph query for field access
     * @return true if the type has its own identity field
     */
    private boolean hasIdentityField(TypeNode node, GraphQuery query) {
        String typeName = node.simpleName();
        String expectedIdFieldName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1) + "Id";

        return query.fieldsOf(node).stream().anyMatch(field -> {
            String fieldName = field.simpleName();
            // Field is "id" or matches "{typeName}Id" pattern (e.g., "orderId" for Order)
            return fieldName.equals("id") || fieldName.equals(expectedIdFieldName);
        });
    }
}
