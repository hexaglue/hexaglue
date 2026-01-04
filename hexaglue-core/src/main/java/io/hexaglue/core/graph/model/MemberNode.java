/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

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
