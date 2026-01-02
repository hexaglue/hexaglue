package io.hexaglue.core.frontend;

/**
 * The syntactic form of a Java type declaration.
 */
public enum JavaForm {

    /**
     * A regular class declaration.
     */
    CLASS,

    /**
     * A record declaration (Java 16+).
     */
    RECORD,

    /**
     * An interface declaration.
     */
    INTERFACE,

    /**
     * An enum declaration.
     */
    ENUM,

    /**
     * An annotation type declaration.
     */
    ANNOTATION
}
