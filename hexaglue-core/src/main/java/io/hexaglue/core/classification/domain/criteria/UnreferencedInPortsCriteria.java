package io.hexaglue.core.classification.domain.criteria;

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
 * Matches types that have an identity field but are NOT referenced in any port signatures.
 *
 * <p>This criteria detects internal entities - entities that exist within an aggregate
 * but are not directly exposed through the application's ports. These are typically:
 * <ul>
 *   <li>Child entities within an aggregate (e.g., OrderLine within Order)</li>
 *   <li>Entities that are only accessed through their parent aggregate</li>
 * </ul>
 *
 * <p>This is a graph-based heuristic that looks for the ABSENCE of USES_IN_SIGNATURE edges.
 *
 * <p>Priority: 50 (lower than aggregate-related criteria, as this is absence-based)
 * <p>Confidence: LOW (absence of evidence is weaker than presence)
 */
public final class UnreferencedInPortsCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "unreferenced-in-ports";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.ENTITY;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a class or record, not an interface or enum
        if (node.form() == JavaForm.INTERFACE || node.form() == JavaForm.ENUM) {
            return MatchResult.noMatch();
        }

        // Must have an identity field (otherwise it's not entity-like)
        if (!hasIdentityField(node, query)) {
            return MatchResult.noMatch();
        }

        // Check if this type is referenced in any port signature
        boolean isReferencedInPorts = isUsedInAnyPortSignature(node, query);

        if (isReferencedInPorts) {
            // Type is exposed through ports - not an internal entity
            return MatchResult.noMatch();
        }

        // Type has identity but is NOT referenced in any port signature
        // This suggests it's an internal entity (e.g., OrderLine, not directly accessible)
        return MatchResult.match(
                ConfidenceLevel.LOW,
                "Entity with identity not exposed through ports (internal entity)",
                List.of(Evidence.fromStructure(
                        "Has identity field but no USES_IN_SIGNATURE edges from ports", List.of(node.id()))));
    }

    private boolean hasIdentityField(TypeNode type, GraphQuery query) {
        return query.fieldsOf(type).stream().anyMatch(this::isIdentityField);
    }

    private boolean isIdentityField(FieldNode field) {
        String name = field.simpleName();
        return name.equals("id") || name.endsWith("Id");
    }

    private boolean isUsedInAnyPortSignature(TypeNode node, GraphQuery query) {
        // Look for incoming USES_IN_SIGNATURE edges
        // These edges come FROM interfaces (ports) TO this type
        return query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                // The source should be an interface (port)
                .anyMatch(sourceType -> sourceType.form() == JavaForm.INTERFACE);
    }
}
