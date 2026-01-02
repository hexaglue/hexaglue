package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.FieldNode;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;
import java.util.Optional;

/**
 * Matches types that have an identity field, suggesting they are entities.
 *
 * <p>Identity field patterns:
 * <ul>
 *   <li>Field named exactly "id"</li>
 *   <li>Field with name ending in "Id" (e.g., orderId, customerId)</li>
 * </ul>
 *
 * <p>Priority: 60 (medium heuristic - needs more context for higher confidence)
 * <p>Confidence: MEDIUM
 */
public final class HasIdentityCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "has-identity";
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
        // Only apply to classes (not interfaces, records, enums)
        if (node.form() != JavaForm.CLASS) {
            return MatchResult.noMatch();
        }

        // Find identity field
        Optional<FieldNode> identityField = findIdentityField(node, query);
        if (identityField.isEmpty()) {
            return MatchResult.noMatch();
        }

        FieldNode field = identityField.get();
        Evidence evidence = Evidence.fromStructure(
                "Has identity field '%s' of type %s"
                        .formatted(field.simpleName(), field.type().simpleName()),
                List.of(field.id()));

        return MatchResult.match(
                ConfidenceLevel.MEDIUM, "Has identity field '%s'".formatted(field.simpleName()), evidence);
    }

    private Optional<FieldNode> findIdentityField(TypeNode node, GraphQuery query) {
        List<FieldNode> fields = query.fieldsOf(node);

        // First, look for a field named exactly "id"
        Optional<FieldNode> idField =
                fields.stream().filter(f -> f.simpleName().equals("id")).findFirst();

        if (idField.isPresent()) {
            return idField;
        }

        // Then look for fields ending with "Id"
        return fields.stream().filter(f -> f.simpleName().endsWith("Id")).findFirst();
    }
}
