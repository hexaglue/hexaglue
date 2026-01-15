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
 * Syntactic representation of a method or constructor parameter.
 *
 * @param name the parameter name
 * @param type the parameter type
 * @param annotations the annotations on this parameter
 * @param isVarArgs whether this is a varargs parameter
 * @since 4.0.0
 */
public record ParameterSyntax(String name, TypeRef type, List<AnnotationSyntax> annotations, boolean isVarArgs) {

    public ParameterSyntax {
        annotations = annotations != null ? List.copyOf(annotations) : List.of();
    }

    /**
     * Creates a simple parameter without annotations.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @return a new ParameterSyntax
     */
    public static ParameterSyntax of(String name, TypeRef type) {
        return new ParameterSyntax(name, type, List.of(), false);
    }
}
