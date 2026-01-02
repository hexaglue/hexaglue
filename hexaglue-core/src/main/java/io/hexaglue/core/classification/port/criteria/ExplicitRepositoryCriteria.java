package io.hexaglue.core.classification.port.criteria;

import io.hexaglue.core.classification.port.PortDirection;
import io.hexaglue.core.classification.port.PortKind;

/**
 * Matches interfaces annotated with @Repository (jMolecules).
 *
 * <p>Priority: 100 (explicit annotation)
 * <p>Confidence: EXPLICIT
 * <p>Direction: DRIVEN
 */
public final class ExplicitRepositoryCriteria extends AbstractExplicitPortAnnotationCriteria {

    public static final String ANNOTATION_SIMPLE_NAME = "Repository";
    public static final String ANNOTATION_QUALIFIED_NAME = "org.jmolecules.ddd.annotation.Repository";

    public ExplicitRepositoryCriteria() {
        super(ANNOTATION_SIMPLE_NAME, ANNOTATION_QUALIFIED_NAME, PortKind.REPOSITORY, PortDirection.DRIVEN);
    }
}
