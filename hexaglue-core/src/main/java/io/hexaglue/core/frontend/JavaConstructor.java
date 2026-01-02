package io.hexaglue.core.frontend;

import java.util.List;

/**
 * A constructor in a Java type.
 */
public non-sealed interface JavaConstructor extends JavaMember {

    /**
     * Returns the constructor parameters.
     */
    List<JavaParameter> parameters();

    /**
     * Returns the declared thrown types.
     */
    List<TypeRef> thrownTypes();

    /**
     * Returns true if this is a no-arg constructor.
     */
    default boolean isNoArg() {
        return parameters().isEmpty();
    }
}
