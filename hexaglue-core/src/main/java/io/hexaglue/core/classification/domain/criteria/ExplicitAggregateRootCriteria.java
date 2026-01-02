package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.domain.DomainKind;

/**
 * Matches types annotated with @AggregateRoot (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitAggregateRootCriteria extends AbstractExplicitAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "AggregateRoot";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.ddd.annotation.AggregateRoot";

    public ExplicitAggregateRootCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, DomainKind.AGGREGATE_ROOT);
    }
}
