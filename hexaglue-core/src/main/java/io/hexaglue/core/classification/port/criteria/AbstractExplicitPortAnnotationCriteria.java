package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.port.PortClassificationCriteria;
import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;

/**
 * Base class for criteria that match explicit jMolecules port annotations.
 *
 * <p>All explicit annotation criteria have priority 100 (highest)
 * and confidence EXPLICIT.
 */
public abstract class AbstractExplicitPortAnnotationCriteria implements PortClassificationCriteria {

    private final String annotationSimpleName;
    private final String annotationQualifiedName;
    private final PortKind targetKind;
    private final PortDirection targetDirection;

    protected AbstractExplicitPortAnnotationCriteria(
            String annotationSimpleName,
            String annotationQualifiedName,
            PortKind targetKind,
            PortDirection targetDirection) {
        this.annotationSimpleName = annotationSimpleName;
        this.annotationQualifiedName = annotationQualifiedName;
        this.targetKind = targetKind;
        this.targetDirection = targetDirection;
    }

    @Override
    public String name() {
        return "explicit-" + targetKind.name().toLowerCase().replace('_', '-');
    }

    @Override
    public int priority() {
        return 100; // Highest priority for explicit annotations
    }

    @Override
    public PortKind targetKind() {
        return targetKind;
    }

    @Override
    public PortDirection targetDirection() {
        return targetDirection;
    }

    @Override
    public MatchResult evaluate(TypeNode node, GraphQuery query) {
        for (AnnotationRef annotation : node.annotations()) {
            if (matchesAnnotation(annotation)) {
                return MatchResult.explicitAnnotation(annotationSimpleName, node.id());
            }
        }
        return MatchResult.noMatch();
    }

    private boolean matchesAnnotation(AnnotationRef annotation) {
        // Match by qualified name or simple name (for flexibility)
        return annotation.qualifiedName().equals(annotationQualifiedName)
                || annotation.simpleName().equals(annotationSimpleName);
    }

    protected String annotationSimpleName() {
        return annotationSimpleName;
    }

    protected String annotationQualifiedName() {
        return annotationQualifiedName;
    }
}
