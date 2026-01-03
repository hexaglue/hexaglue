package io.hexaglue.core.classification.domain.criteria;

import io.hexaglue.core.classification.domain.DomainKind;

/**
 * Matches types annotated with @DomainEvent (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 */
public final class ExplicitDomainEventCriteria extends AbstractExplicitAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "DomainEvent";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.event.annotation.DomainEvent";

    public ExplicitDomainEventCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, DomainKind.DOMAIN_EVENT);
    }
}
