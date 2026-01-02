package io.hexaglue.core.classification.port;

import io.hexaglue.core.classification.ClassificationCriteria;

/**
 * Specialized criteria for port classification that includes port direction.
 */
public interface PortClassificationCriteria extends ClassificationCriteria<PortKind> {

    /**
     * Returns the direction of the port this criteria targets.
     */
    PortDirection targetDirection();
}
