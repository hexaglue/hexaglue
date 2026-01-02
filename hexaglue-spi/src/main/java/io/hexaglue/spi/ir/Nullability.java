package io.hexaglue.spi.ir;

/**
 * Nullability of a property.
 */
public enum Nullability {

    /**
     * The property is never null.
     */
    NON_NULL,

    /**
     * The property can be null.
     */
    NULLABLE,

    /**
     * Nullability could not be determined.
     */
    UNKNOWN
}
