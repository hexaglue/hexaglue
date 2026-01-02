package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.domain.DomainKind;

/**
 * Matches types annotated with @Entity (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitEntityCriteria extends AbstractExplicitAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "Entity";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.ddd.annotation.Entity";

    public ExplicitEntityCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, DomainKind.ENTITY);
    }
}
