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

package io.hexaglue.syntax;

import java.util.List;
import java.util.Objects;

/**
 * Reference to a type in source code.
 *
 * <p>Represents both simple types (String, int) and parameterized types (List&lt;String&gt;).</p>
 *
 * @param qualifiedName the fully qualified name of the type
 * @param simpleName the simple name (without package)
 * @param typeArguments the generic type arguments (empty for non-generic types)
 * @param isPrimitive whether this is a primitive type
 * @param isArray whether this is an array type
 * @param arrayDimensions the number of array dimensions (0 if not an array)
 * @since 4.0.0
 */
public record TypeRef(
        String qualifiedName,
        String simpleName,
        List<TypeRef> typeArguments,
        boolean isPrimitive,
        boolean isArray,
        int arrayDimensions) {

    public TypeRef {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        typeArguments = typeArguments != null ? List.copyOf(typeArguments) : List.of();
    }

    /**
     * Creates a simple type reference.
     *
     * @param qualifiedName the fully qualified name
     * @return a new TypeRef
     */
    public static TypeRef of(String qualifiedName) {
        String simpleName = qualifiedName.contains(".")
                ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
                : qualifiedName;
        return new TypeRef(qualifiedName, simpleName, List.of(), false, false, 0);
    }

    /**
     * Creates a primitive type reference.
     *
     * @param name the primitive name (int, boolean, etc.)
     * @return a new TypeRef
     */
    public static TypeRef primitive(String name) {
        return new TypeRef(name, name, List.of(), true, false, 0);
    }

    /**
     * Creates a parameterized type reference.
     *
     * @param qualifiedName the fully qualified name
     * @param typeArgs the type arguments
     * @return a new TypeRef
     */
    public static TypeRef parameterized(String qualifiedName, List<TypeRef> typeArgs) {
        String simpleName = qualifiedName.contains(".")
                ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
                : qualifiedName;
        return new TypeRef(qualifiedName, simpleName, typeArgs, false, false, 0);
    }

    /**
     * Returns whether this type is parameterized (has type arguments).
     *
     * @return true if parameterized
     */
    public boolean isParameterized() {
        return !typeArguments.isEmpty();
    }

    /**
     * Returns a representation of this type as it would appear in source code.
     *
     * @return the source representation
     */
    public String toSourceString() {
        StringBuilder sb = new StringBuilder(simpleName);
        if (!typeArguments.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(typeArguments.get(i).toSourceString());
            }
            sb.append(">");
        }
        for (int i = 0; i < arrayDimensions; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }
}
