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

import java.util.Optional;

/**
 * A field in a Java type.
 */
public non-sealed interface JavaField extends JavaMember {

    /**
     * Returns the declared type of this field.
     */
    TypeRef type();

    /**
     * Returns true if this field is final.
     */
    @Override
    default boolean isFinal() {
        return modifiers().contains(JavaModifier.FINAL);
    }

    /**
     * Returns the initial value expression as a string, if available.
     */
    Optional<String> initialValue();

    /**
     * Returns true if this looks like an identity field.
     */
    default boolean looksLikeIdentity() {
        String name = simpleName();
        return name.equals("id") || name.endsWith("Id");
    }

    /**
     * Returns true if this field's type is a common ID type.
     */
    default boolean hasCommonIdType() {
        String typeName = type().rawQualifiedName();
        return typeName.equals("java.util.UUID")
                || typeName.equals("java.lang.Long")
                || typeName.equals("java.lang.String")
                || typeName.equals("long");
    }
}
