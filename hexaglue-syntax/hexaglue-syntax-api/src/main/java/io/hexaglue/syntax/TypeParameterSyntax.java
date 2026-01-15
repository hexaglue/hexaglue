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

/**
 * Syntactic representation of a type parameter (generic).
 *
 * @param name the type parameter name (e.g., "T", "E")
 * @param bounds the upper bounds (empty if unbounded)
 * @since 4.0.0
 */
public record TypeParameterSyntax(String name, List<TypeRef> bounds) {

    public TypeParameterSyntax {
        bounds = bounds != null ? List.copyOf(bounds) : List.of();
    }

    /**
     * Creates an unbounded type parameter.
     *
     * @param name the type parameter name
     * @return a new TypeParameterSyntax
     */
    public static TypeParameterSyntax unbounded(String name) {
        return new TypeParameterSyntax(name, List.of());
    }

    /**
     * Returns whether this type parameter has bounds.
     *
     * @return true if bounded
     */
    public boolean isBounded() {
        return !bounds.isEmpty();
    }
}
