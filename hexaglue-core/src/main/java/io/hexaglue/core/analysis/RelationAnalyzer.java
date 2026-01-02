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

        // Case 1: Collection type (List<X>, Set<X>, etc.)
        if (fieldType.isCollectionLike()) {
            return analyzeCollectionField(ownerType, field, fieldType, query, context);
        }

        // Case 2: Simple reference
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

        // ENTITY or AGGREGATE_ROOT in collection → ONE_TO_MANY
        if (targetKind == DomainKind.ENTITY || targetKind == DomainKind.AGGREGATE_ROOT) {
            String mappedBy = mappedByDetector
                    .detectMappedBy(ownerType, field, elementType, query)
                    .orElse(null);

            CascadeType cascade =
                    cascadeInference.infer(ownerType, targetType.get(), RelationKind.ONE_TO_MANY, context);

            // Orphan removal only for child entities within same aggregate
            boolean orphanRemoval = targetKind == DomainKind.ENTITY && cascade == CascadeType.ALL;

            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.ONE_TO_MANY,
                    elementTypeFqn,
                    targetKind,
                    mappedBy,
                    cascade,
                    FetchType.LAZY,
                    orphanRemoval));
        }

        return Optional.empty();
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

        // ENTITY → MANY_TO_ONE or ONE_TO_ONE (child entity reference)
        if (targetKind == DomainKind.ENTITY) {
            CascadeType cascade =
                    cascadeInference.infer(ownerType, targetType.get(), RelationKind.MANY_TO_ONE, context);

            return Optional.of(new DomainRelation(
                    field.simpleName(),
                    RelationKind.MANY_TO_ONE,
                    typeFqn,
                    DomainKind.ENTITY,
                    null,
                    cascade,
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
