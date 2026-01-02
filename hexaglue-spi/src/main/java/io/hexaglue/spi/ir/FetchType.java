package io.hexaglue.spi.ir;

/**
 * JPA fetch strategy for relationship mapping.
 */
public enum FetchType {

    /**
     * Lazy fetching - data is loaded only when accessed.
     * Recommended for collections and optional associations.
     */
    LAZY,

    /**
     * Eager fetching - data is loaded immediately with the parent entity.
     * Use sparingly to avoid N+1 queries.
     */
    EAGER
}
