package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.frontend.JavaForm;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches interfaces with naming pattern *UseCase.
 *
 * <p>Naming patterns:
 * <ul>
 *   <li>Name ends with "UseCase"</li>
 *   <li>Name ends with "Service" (application service)</li>
 *   <li>Name ends with "Handler" (command/query handler)</li>
 * </ul>
 *
 * <p>Priority: 80 (strong heuristic)
 * <p>Confidence: HIGH
 * <p>Direction: DRIVING
 */
public final class NamingUseCaseCriteria implements PortClassificationCriteria {

    @Override
    public String name() {
        return "naming-use-case";
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public PortKind targetKind() {
        return PortKind.USE_CASE;
    }

    @Override
    public PortDirection targetDirection() {
        return PortDirection.DRIVING;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        // Must be an interface
        if (node.form() != JavaForm.INTERFACE) {
            return MatchResult.noMatch();
        }

        String name = node.simpleName();
        String matchedPattern = null;

        if (name.endsWith("UseCase")) {
            matchedPattern = "*UseCase";
        } else if (name.endsWith("Service") && !name.endsWith("DomainService")) {
            // Exclude DomainService which is a domain concept
            matchedPattern = "*Service";
        } else if (name.endsWith("Handler")) {
            matchedPattern = "*Handler";
        }

        if (matchedPattern == null) {
            return MatchResult.noMatch();
        }

        return MatchResult.match(
                ConfidenceLevel.HIGH,
                "Interface '%s' matches use case pattern '%s'".formatted(name, matchedPattern),
                List.of(Evidence.fromNaming(matchedPattern, name)));
    }
}
