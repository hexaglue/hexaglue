package io.hexaglue.core.frontend;

import java.util.Set;

/**
 * A member of a Java type (field, method, or constructor).
 */
public sealed interface JavaMember extends JavaNamed, JavaAnnotated, JavaSourced
        permits JavaField, JavaMethod, JavaConstructor {

    /**
     * Returns the qualified name of the declaring type.
     */
    String declaringTypeQualifiedName();

    /**
     * Returns the modifiers on this member.
     */
    Set<JavaModifier> modifiers();

    /**
     * Returns true if this member is public.
     */
    default boolean isPublic() {
        return modifiers().contains(JavaModifier.PUBLIC);
    }

    /**
     * Returns true if this member is private.
     */
    default boolean isPrivate() {
        return modifiers().contains(JavaModifier.PRIVATE);
    }

    /**
     * Returns true if this member is static.
     */
    default boolean isStatic() {
        return modifiers().contains(JavaModifier.STATIC);
    }

    /**
     * Returns true if this member is final.
     */
    default boolean isFinal() {
        return modifiers().contains(JavaModifier.FINAL);
    }
}
