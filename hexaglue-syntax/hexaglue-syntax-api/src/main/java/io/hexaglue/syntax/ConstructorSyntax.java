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
import java.util.Optional;
import java.util.Set;

/**
 * Syntactic representation of a constructor.
 *
 * @since 4.0.0
 */
public interface ConstructorSyntax {

    /**
     * Returns the constructor parameters.
     *
     * @return an immutable list of parameters
     */
    List<ParameterSyntax> parameters();

    /**
     * Returns the types thrown by this constructor.
     *
     * @return an immutable list of thrown types
     */
    List<TypeRef> thrownTypes();

    /**
     * Returns the annotations on this constructor.
     *
     * @return an immutable list of annotations
     */
    List<AnnotationSyntax> annotations();

    /**
     * Returns the constructor modifiers.
     *
     * @return an immutable set of modifiers
     */
    Set<Modifier> modifiers();

    /**
     * Returns the source location of this constructor.
     *
     * @return the source location
     */
    SourceLocation sourceLocation();

    /**
     * Returns the constructor body, if available.
     *
     * @return an Optional containing the body
     */
    Optional<MethodBodySyntax> body();

    /**
     * Returns the constructor signature for identification.
     *
     * <p>Format: "(Type1,Type2)" for parameters.</p>
     *
     * @return the signature string
     */
    default String signature() {
        StringBuilder sb = new StringBuilder("(");
        List<ParameterSyntax> params = parameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(params.get(i).type().simpleName());
        }
        sb.append(")");
        return sb.toString();
    }
}
