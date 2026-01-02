package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.ConfidenceLevel;
import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;
import java.util.List;

/**
 * Matches interfaces annotated with @PrimaryPort (jMolecules) or implementing PrimaryPort.
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 * <p>Direction: DRIVING
 */
public final class ExplicitPrimaryPortCriteria implements PortClassificationCriteria {

    public static final String INTERFACE_QUALIFIED_NAME = "org.jmolecules.architecture.hexagonal.PrimaryPort";
    public static final String ANNOTATION_SIMPLE_NAME = "PrimaryPort";

    @Override
    public String name() {
        return "explicit-primary-port";
    }

    @Override
    public int priority() {
        return 100;
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
        // Check if implements PrimaryPort interface
        boolean implementsPrimaryPort = node.interfaces().stream()
                .anyMatch(iface -> iface.rawQualifiedName().equals(INTERFACE_QUALIFIED_NAME)
                        || iface.simpleName().equals("PrimaryPort"));

        if (implementsPrimaryPort) {
            return MatchResult.match(
                    ConfidenceLevel.EXPLICIT,
                    "Implements PrimaryPort interface",
                    List.of(Evidence.fromStructure("Implements " + INTERFACE_QUALIFIED_NAME, List.of(node.id()))));
        }

        // Check for @PrimaryPort annotation
        for (AnnotationRef annotation : node.annotations()) {
            if (annotation.simpleName().equals(ANNOTATION_SIMPLE_NAME)) {
                return MatchResult.explicitAnnotation(ANNOTATION_SIMPLE_NAME, node.id());
            }
        }

        return MatchResult.noMatch();
    }
}
