package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.ClassificationCriteria;
import io.hexaglue.core.classification.MatchResult;
import io.hexaglue.core.classification.domain.DomainKind;
import io.hexaglue.core.graph.model.AnnotationRef;
import io.hexaglue.core.graph.model.TypeNode;
import io.hexaglue.core.graph.query.GraphQuery;

/**
 * Base class for criteria that match explicit jMolecules annotations.
 *
 * <p>All explicit annotation criteria have priority 100 (highest)
 * and confidence EXPLICIT.
 */
public abstract class AbstractExplicitAnnotationCriteria implements ClassificationCriteria<DomainKind> {

    private final String annotationSimpleName;
    private final String annotationQualifiedName;
    private final DomainKind targetKind;

    protected AbstractExplicitAnnotationCriteria(
            String annotationSimpleName, String annotationQualifiedName, DomainKind targetKind) {
        this.annotationSimpleName = annotationSimpleName;
        this.annotationQualifiedName = annotationQualifiedName;
        this.targetKind = targetKind;
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
    public DomainKind targetKind() {
        return targetKind;
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
