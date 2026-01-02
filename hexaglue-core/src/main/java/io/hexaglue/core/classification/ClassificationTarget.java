package io.hexaglue.core.classification;

/**
 * Target of a classification.
 *
 * <p>Indicates whether a type is being classified as a domain concept
 * or as a port (interface boundary).
 */
public enum ClassificationTarget {

    /**
     * Domain classification (AGGREGATE_ROOT, ENTITY, VALUE_OBJECT, etc.).
     */
    DOMAIN,

    /**
     * Port classification (REPOSITORY, GATEWAY, USE_CASE, etc.).
     */
    PORT
}
