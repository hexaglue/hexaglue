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

import io.hexaglue.core.classification.ClassificationContext;
import io.hexaglue.core.frontend.TypeRef;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.NodeId;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import io.hexaglue.spi.ir.CascadeType;
import io.hexaglue.spi.ir.DomainKind;
import io.hexaglue.spi.ir.DomainRelation;
import io.hexaglue.spi.ir.FetchType;
import io.hexaglue.spi.ir.RelationKind;
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

        DomainKind targetKind = getDomainKind(elementId, context);
        if (targetKind == null) {
            return Optional.empty();
        }

        // VALUE_OBJECT in collection → ELEMENT_COLLECTION
        if (targetKind == DomainKind.VALUE_OBJECT) {
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ELEMENT_COLLECTION,
                    elementTypeFqn,
                    DomainKind.VALUE_OBJECT,
                    null,
                    CascadeType.NONE,
                    FetchType.EAGER,
                    false));
        }

        // AGGREGATE_ROOT in collection → MANY_TO_MANY (inter-aggregate relationship)
        // This is a DDD-semantic: each aggregate root is independently managed,
        // so a collection of them represents a many-to-many association
        if (targetKind == DomainKind.AGGREGATE_ROOT) {
            String mappedBy = mappedByDetector
                    .detectMappedBy(ownerType, field, elementType, query)
                    .orElse(null);

            // Inter-aggregate relations should NOT cascade
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_MANY,
                    elementTypeFqn,
                    DomainKind.AGGREGATE_ROOT,
                    mappedBy,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false));
        }

        // ENTITY in collection → ONE_TO_MANY (child entities within aggregate)
        if (targetKind == DomainKind.ENTITY) {
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
                    DomainKind.ENTITY,
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

        DomainKind targetKind = getDomainKind(typeId, context);
        if (targetKind == null) {
            return Optional.empty();
        }

        // VALUE_OBJECT → EMBEDDED
        if (targetKind == DomainKind.VALUE_OBJECT) {
            return Optional.of(DomainRelation.embedded(field.simpleName(), typeFqn));
        }

        // AGGREGATE_ROOT → MANY_TO_ONE (reference to another aggregate)
        if (targetKind == DomainKind.AGGREGATE_ROOT) {
            // References to other aggregates should NOT cascade
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_ONE,
                    typeFqn,
                    DomainKind.AGGREGATE_ROOT,
                    null,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false));
        }

        // ENTITY → ONE_TO_ONE (child entity within aggregate)
        // In DDD, a single reference to a child entity is typically a 1:1 relationship
        // (the child is owned exclusively by this aggregate)
        if (targetKind == DomainKind.ENTITY) {
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
                    DomainKind.ENTITY,
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

        DomainKind targetKind = getDomainKind(valueId, context);
        if (targetKind == null) {
            return Optional.empty();
        }

        // Map<K, VALUE_OBJECT> → ELEMENT_COLLECTION
        if (targetKind == DomainKind.VALUE_OBJECT) {
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ELEMENT_COLLECTION,
                    valueTypeFqn,
                    DomainKind.VALUE_OBJECT,
                    null,
                    CascadeType.NONE,
                    FetchType.EAGER,
                    false));
        }

        // Map<K, ENTITY> → ONE_TO_MANY (indexed collection)
        if (targetKind == DomainKind.ENTITY) {
            CascadeType cascade =
                    cascadeInference.infer(ownerType, targetType.get(), RelationKind.ONE_TO_MANY, context);

            boolean orphanRemoval = cascade == CascadeType.ALL;

            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ONE_TO_MANY,
                    valueTypeFqn,
                    DomainKind.ENTITY,
                    null,
                    cascade,
                    FetchType.LAZY,
                    orphanRemoval));
        }

        // Map<K, AGGREGATE_ROOT> → MANY_TO_MANY (indexed inter-aggregate)
        if (targetKind == DomainKind.AGGREGATE_ROOT) {
            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_MANY,
                    valueTypeFqn,
                    DomainKind.AGGREGATE_ROOT,
                    null,
                    CascadeType.NONE,
                    FetchType.LAZY,
                    false));
        }

        return Optional.empty();
    }

    private DomainKind getDomainKind(NodeId typeId, ClassificationContext context) {
        String kind = context.getKind(typeId);
        if (kind == null) {
            return null;
        }
        try {
            return DomainKind.valueOf(kind);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
