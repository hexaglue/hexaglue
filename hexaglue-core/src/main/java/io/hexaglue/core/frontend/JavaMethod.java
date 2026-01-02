package io.hexaglue.core.frontend;

import java.util.List;

/**
 * A method in a Java type (excluding constructors).
 */
public non-sealed interface JavaMethod extends JavaMember {

    /**
     * Returns the return type.
     */
    TypeRef returnType();

    /**
     * Returns the method parameters.
     */
    List<JavaParameter> parameters();

    /**
     * Returns the declared thrown types.
     */
    List<TypeRef> thrownTypes();

    /**
     * Returns true if this is a default method (in an interface).
     */
    boolean isDefault();

    /**
     * Returns true if this method takes no parameters.
     */
    default boolean hasNoParameters() {
        return parameters().isEmpty();
    }

    /**
     * Returns true if this method takes a single parameter of the given type.
     */
    default boolean hasSingleParameter(String typeQualifiedName) {
        return parameters().size() == 1
                && parameters().get(0).type().rawQualifiedName().equals(typeQualifiedName);
    }

    /**
     * Returns true if this looks like a getter method.
     */
    default boolean looksLikeGetter() {
        String name = simpleName();
        return (name.startsWith("get") || name.startsWith("is"))
                && hasNoParameters()
                && !"void".equals(returnType().rawQualifiedName());
    }

    /**
     * Returns true if this looks like a CRUD method (save, find, delete).
     */
    default boolean looksLikeCrudMethod() {
        String name = simpleName().toLowerCase();
        return name.startsWith("save")
                || name.startsWith("find")
                || name.startsWith("delete")
                || name.startsWith("remove")
                || name.startsWith("get")
                || name.startsWith("exists")
                || name.startsWith("count");
    }
}
