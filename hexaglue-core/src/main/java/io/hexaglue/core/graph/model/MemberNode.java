package io.hexaglue.core.graph.model;

import io.hexaglue.core.frontend.JavaModifier;
import java.util.Set;

/**
 * Base class for member nodes (fields, methods, constructors).
 */
public abstract sealed class MemberNode extends Node permits FieldNode, MethodNode, ConstructorNode {

    /**
     * Returns the NodeId of the declaring type.
     */
    public abstract NodeId declaringTypeId();

    /**
     * Returns the fully qualified name of the declaring type.
     */
    public abstract String declaringTypeName();

    /**
     * Returns the modifiers of this member.
     */
    public abstract Set<JavaModifier> modifiers();

    /**
     * Returns true if this member is public.
     */
    public boolean isPublic() {
        return modifiers().contains(JavaModifier.PUBLIC);
    }

    /**
     * Returns true if this member is private.
     */
    public boolean isPrivate() {
        return modifiers().contains(JavaModifier.PRIVATE);
    }

    /**
     * Returns true if this member is protected.
     */
    public boolean isProtected() {
        return modifiers().contains(JavaModifier.PROTECTED);
    }

    /**
     * Returns true if this member is static.
     */
    public boolean isStatic() {
        return modifiers().contains(JavaModifier.STATIC);
    }

    /**
     * Returns true if this member is final.
     */
    public boolean isFinal() {
        return modifiers().contains(JavaModifier.FINAL);
    }

    @Override
    public String packageName() {
        int lastDot = declaringTypeName().lastIndexOf('.');
        return lastDot > 0 ? declaringTypeName().substring(0, lastDot) : "";
    }
}
