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
 * Analyzed method body content.
 *
 * <p>Provides access to source code, invocations, and field accesses
 * for detailed analysis.</p>
 *
 * @since 4.0.0
 */
public interface MethodBodySyntax {

    /**
     * Returns the source code of the method body.
     *
     * @return the source code
     */
    String sourceCode();

    /**
     * Returns the method invocations in this body.
     *
     * @return an immutable list of invocations
     */
    List<MethodInvocationSyntax> invocations();

    /**
     * Returns the field accesses in this body.
     *
     * @return an immutable list of field accesses
     */
    List<FieldAccessSyntax> fieldAccesses();

    /**
     * Returns the cyclomatic complexity of this method.
     *
     * @return the cyclomatic complexity
     */
    int cyclomaticComplexity();

    /**
     * Returns the number of lines in this method body.
     *
     * @return the line count
     */
    int lineCount();

    /**
     * Represents a method invocation in the body.
     *
     * @param targetType the type on which the method is invoked (may be null for local calls)
     * @param methodName the invoked method name
     * @param argumentCount the number of arguments
     */
    record MethodInvocationSyntax(TypeRef targetType, String methodName, int argumentCount) {}

    /**
     * Represents a field access in the body.
     *
     * @param targetType the type containing the field
     * @param fieldName the accessed field name
     * @param isRead whether this is a read access (vs write)
     */
    record FieldAccessSyntax(TypeRef targetType, String fieldName, boolean isRead) {}
}
