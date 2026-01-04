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
