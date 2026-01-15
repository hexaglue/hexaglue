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

package io.hexaglue.spi.ir;

import java.util.Set;

/**
 * A parameter of a port method with complete type information.
 *
 * <p>This preserves parameter names and annotations, providing richer information
 * than just qualified type names.
 *
 * @param name the parameter name (e.g., "id", "email", "order")
 * @param type the type reference with generics preserved
 * @param isIdentity true if this parameter represents the aggregate's identity
 * @param annotations annotations present on the parameter (e.g., @NotNull, @Valid)
 * @since 3.0.0
 */
public record MethodParameter(String name, TypeRef type, boolean isIdentity, Set<String> annotations) {

    /**
     * Creates a parameter without annotations.
     *
     * @param name the parameter name
     * @param type the type reference
     * @param isIdentity whether this parameter is the aggregate's identity
     * @return a new MethodParameter
     */
    public static MethodParameter of(String name, TypeRef type, boolean isIdentity) {
        return new MethodParameter(name, type, isIdentity, Set.of());
    }

    /**
     * Creates a simple parameter that is not an identity.
     *
     * @param name the parameter name
     * @param type the type reference
     * @return a new MethodParameter
     */
    public static MethodParameter simple(String name, TypeRef type) {
        return new MethodParameter(name, type, false, Set.of());
    }

    /**
     * Returns the qualified type name for backward compatibility.
     *
     * @return the fully qualified type name
     */
    public String typeName() {
        return type.qualifiedName();
    }
}
