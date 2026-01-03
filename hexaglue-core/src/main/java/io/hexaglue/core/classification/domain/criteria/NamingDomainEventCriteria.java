package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches types with naming patterns suggesting domain events.
 *
 * <p>Domain events are typically:
 * <ul>
 *   <li>Named in past tense (OrderPlaced, CustomerRegistered)</li>
 *   <li>Suffixed with "Event" (OrderPlacedEvent)</li>
 *   <li>Immutable (records or final fields)</li>
 * </ul>
 *
 * <p>This criteria matches types ending with "Event" that are also immutable.
 *
 * <p>Priority: 55 (between medium and lower heuristics)
 * <p>Confidence: MEDIUM
 */
public final class NamingDomainEventCriteria implements ClassificationCriteria<DomainKind> {

    @Override
    public String name() {
        return "naming-domain-event";
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public DomainKind targetKind() {
        return DomainKind.DOMAIN_EVENT;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        String simpleName = node.simpleName();

        // Check naming pattern
        if (!simpleName.endsWith("Event")) {
            return MatchResult.noMatch();
        }

        // Events should be immutable (records preferred)
        if (node.form() == JavaForm.RECORD) {
            return MatchResult.match(
                    ConfidenceLevel.MEDIUM,
                    "Record with name ending in 'Event' suggests domain event",
                    List.of(Evidence.fromNaming("*Event", simpleName)));
        }

        // For classes, we'd need to check immutability - but for now just accept the naming
        if (node.form() == JavaForm.CLASS) {
            return MatchResult.match(
                    ConfidenceLevel.LOW,
                    "Class with name ending in 'Event' may be domain event",
                    List.of(Evidence.fromNaming("*Event", simpleName)));
        }

        return MatchResult.noMatch();
    }
}
