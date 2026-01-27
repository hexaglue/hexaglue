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

package io.hexaglue.arch.model.audit;

import java.util.List;
import java.util.Set;

/**
 * Declaration information for a method in a code unit.
 *
 * @param name           the method name
 * @param returnType     the return type (qualified name)
 * @param parameterTypes the parameter types (qualified names, in order)
 * @param modifiers      the method modifiers (e.g., "public", "abstract")
 * @param annotations    the annotation qualified names
 * @param complexity     the cyclomatic complexity of this method
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record MethodDeclaration(
        String name,
        String returnType,
        List<String> parameterTypes,
        Set<String> modifiers,
        Set<String> annotations,
        int complexity) {

    /**
     * Compact constructor with defensive copies.
     */
    public MethodDeclaration {
        parameterTypes = parameterTypes != null ? List.copyOf(parameterTypes) : List.of();
        modifiers = modifiers != null ? Set.copyOf(modifiers) : Set.of();
        annotations = annotations != null ? Set.copyOf(annotations) : Set.of();
    }

    /**
     * Returns true if this method is complex.
     *
     * @return true if complexity > 10
     */
    public boolean isComplex() {
        return complexity > 10;
    }
}
