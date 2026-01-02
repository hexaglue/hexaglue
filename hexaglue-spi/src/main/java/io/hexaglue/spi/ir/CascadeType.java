package io.hexaglue.spi.ir;

/**
 * JPA cascade operation types for relationship mapping.
 */
public enum CascadeType {

    /**
     * No cascade operations.
     */
    NONE,

    /**
     * Cascade persist operations.
     */
    PERSIST,

    /**
     * Cascade merge operations.
     */
    MERGE,

    /**
     * Cascade remove operations.
     */
    REMOVE,

    /**
     * Cascade refresh operations.
     */
    REFRESH,

    /**
     * Cascade detach operations.
     */
    DETACH,

    /**
     * All cascade operations (PERSIST, MERGE, REMOVE, REFRESH, DETACH).
     */
    ALL
}
