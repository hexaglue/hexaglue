package io.hexaglue.core.frontend;

/**
 * A named Java element.
 */
public interface JavaNamed {

    /**
     * Returns the simple name (without package/enclosing type).
     */
    String simpleName();

    /**
     * Returns the fully qualified name.
     */
    String qualifiedName();

    /**
     * Returns the package name.
     */
    String packageName();
}
