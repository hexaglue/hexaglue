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

/**
 * Matches records with a single component and name ending with "Id",
 * suggesting they are identifiers (ID wrapper types).
 *
 * <p>Pattern:
 * <ul>
 *   <li>Type is a record</li>
 *   <li>Name ends with "Id" (e.g., OrderId, CustomerId)</li>
 *   <li>Has exactly one component (the wrapped value)</li>
 * </ul>
 *
 * <p>Priority: 80 (strong heuristic)
 * <p>Confidence: HIGH
 */
public final class RecordSingleIdCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "record-single-id";
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.IDENTIFIER;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be a record
        if (node.form() != JavaForm.RECORD) {
            return MatchResult.noMatch();
        }

        // Name must end with "Id"
        if (!node.simpleName().endsWith("Id")) {
            return MatchResult.noMatch();
        }

        // Must have exactly one field (record component)
        List<FieldNode> fields = query.fieldsOf(node);
        if (fields.size() != 1) {
            return MatchResult.noMatch();
        }

        FieldNode component = fields.get(0);
        String wrappedType = component.type().simpleName();

        List<Evidence> evidences = List.of(
                Evidence.fromNaming("*Id", node.simpleName()),
                Evidence.fromStructure(
                        "Record with single component '%s' of type %s".formatted(component.simpleName(), wrappedType),
                        List.of(component.id())));

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Record '%s' with single component wrapping %s".formatted(node.simpleName(), wrappedType),
                evidences);
    }
}
