package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.domain.DomainKind;

/**
 * Matches types annotated with @ValueObject (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitValueObjectCriteria extends AbstractExplicitAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "ValueObject";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.ddd.annotation.ValueObject";

    public ExplicitValueObjectCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, DomainKind.VALUE_OBJECT);
    }
}
