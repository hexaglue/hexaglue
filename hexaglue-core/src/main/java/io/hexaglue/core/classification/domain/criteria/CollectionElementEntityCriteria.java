package io.hexaglue.core.classification.domain.criteria;

import static java.util.stream.Collectors.joining;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.Edge;
import io.hexaglue.core.graph.model.EdgeKind;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;

/**
 * Matches types that are used as collection elements in aggregate-like containers.
 *
 * <p>This criteria detects child entities by analyzing their usage:
 * <ul>
 *   <li>Type is used as an element in a collection field (via USES_AS_COLLECTION_ELEMENT edge)</li>
 *   <li>Container type has an identity field (aggregate-like)</li>
 *   <li>This type also has an identity field (making it an entity, not a value object)</li>
 * </ul>
 *
 * <p>This is a graph-based heuristic that exploits the USES_AS_COLLECTION_ELEMENT edges.
 *
 * <p>Priority: 60 (medium heuristic)
 * <p>Confidence: MEDIUM
 */
public final class CollectionElementEntityCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "collection-element-entity";
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.ENTITY;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a class or record, not an interface
        if (node.form() == JavaForm.INTERFACE || node.form() == JavaForm.ENUM) {
            return MatchResult.noMatch();
        }

        // Must have an identity field (otherwise it's a value object)
        if (!hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Find types that use this type as a collection element
        List<TypeNode> containers = query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_AS_COLLECTION_ELEMENT)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        if (containers.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Filter to keep only aggregate-like containers (types with identity)
        List<TypeNode> aggregateContainers =
                containers.stream().filter(c -> hasIdentityField(c, query)).toList();

        if (aggregateContainers.isEmpty()) {
            return MatchResult.noMatch();
        }

        String containerNames =
                aggregateContainers.stream().map(TypeNode::simpleName).collect(joining(", "));

        return MatchResult.match(
                ConfidenceLevel.MEDIUM,
                "Entity in collection of aggregate(s): " + containerNames,
                List.of(Evidence.fromRelationship(
                        "Collection element in: " + containerNames,
                        aggregateContainers.stream().map(TypeNode::id).toList())));
    }

    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(this::isIdentityField);
    }

    private boolean isIdentityField(FieldNode field) {
        String name = field.simpleName();
        return name.equals("id") || name.endsWith("Id");
    }
}
