package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.graph.model.*;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matches types that are used as the dominant type in Repository signatures
 * and have an identity field.
 *
 * <p>This is a strong heuristic for AGGREGATE_ROOT:
 * <ul>
 *   <li>Type is used in a *Repository interface signature (return or parameter)</li>
 *   <li>Type has a field named "id" or ending with "Id"</li>
 * </ul>
 *
 * <p>Priority: 80 (strong heuristic)
 * <p>Confidence: HIGH
 */
public final class RepositoryDominantCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "repository-dominant";
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.AGGREGATE_ROOT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Step 1: Find repositories that use this type in their signature
        Set<TypeNode> repositories = findRepositoriesUsing(node, query);
        if (repositories.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Step 2: Check if this type has an identity field
        Optional<FieldNode> identityField = findIdentityField(node, query);
        if (identityField.isEmpty()) {
            return MatchResult.noMatch();
        }

        // Match!
        String repoNames = repositories.stream().map(TypeNode::simpleName).collect(Collectors.joining(", "));

        List<Evidence> evidences = new ArrayList<>();
        evidences.add(Evidence.fromRelationship(
                "Used in repository signature: " + repoNames,
                repositories.stream().map(TypeNode::id).toList()));
        evidences.add(Evidence.fromStructure(
                "Has identity field '" + identityField.get().simpleName() + "'",
                List.of(identityField.get().id())));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Dominant type in repository [%s], has identity field '%s'"
                        .formatted(repoNames, identityField.get().simpleName()),
                evidences);
    }

    private Set<TypeNode> findRepositoriesUsing(TypeNode node, GraphQuery query) {
        // Find interfaces that use this type in their signature (via USES_IN_SIGNATURE edge)
        return query.graph().edgesTo(node.id()).stream()
                .filter(e -> e.kind() == EdgeKind.USES_IN_SIGNATURE)
                .map(Edge::from)
                .map(query::type)
                .flatMap(Optional::stream)
                .filter(iface -> isRepository(iface, node))
                .collect(Collectors.toSet());
    }

    private boolean isRepository(TypeNode type, TypeNode dominantType) {
        String name = type.simpleName();
        // Match *Repository, *Repositories, or types with @Repository annotation
        if (name.endsWith("Repository") || name.endsWith("Repositories")) {
            return true;
        }
        if (hasRepositoryAnnotation(type)) {
            return true;
        }
        // Check if interface is the plural of the dominant type (e.g., Orders for Order)
        return isPluralOf(name, dominantType.simpleName());
    }

    private boolean isPluralOf(String potentialPlural, String singular) {
        // Simple pluralization rules
        // Orders -> Order, Customers -> Customer, etc.
        if (potentialPlural.equals(singular + "s")) {
            return true;
        }
        // Entries -> Entry (y -> ies)
        if (potentialPlural.endsWith("ies") && singular.endsWith("y")) {
            String singularBase = singular.substring(0, singular.length() - 1);
            return potentialPlural.equals(singularBase + "ies");
        }
        return false;
    }

    private boolean hasRepositoryAnnotation(TypeNode type) {
        return type.annotations().stream()
                .anyMatch(a -> a.simpleName().equals("Repository")
                        || a.qualifiedName().equals("org.jmolecules.ddd.annotation.Repository"));
    }

    private Optional<FieldNode> findIdentityField(TypeNode node, GraphQuery query) {
        return query.fieldsOf(node).stream().filter(this::isIdentityField).findFirst();
    }

    private boolean isIdentityField(FieldNode field) {
        String name = field.simpleName();
        // Match "id" exactly or fields ending with "Id" (like orderId, customerId)
        return name.equals("id") || name.endsWith("Id");
    }
}
