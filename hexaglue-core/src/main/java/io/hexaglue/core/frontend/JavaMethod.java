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
