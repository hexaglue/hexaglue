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

import java.util.stream.Stream;

/**
 * Abstraction over the Java source code being analyzed.
 *
 * <p>This interface provides access to all types in the analysis scope,
 * independent of the underlying implementation (Spoon, JavaParser, etc.).
 *
 * <p>Implementations must ensure:
 * <ul>
 *   <li>Types are returned in deterministic order (by qualified name)</li>
 *   <li>Only types within the configured base package are included</li>
 *   <li>Type resolution is complete (classpath properly configured)</li>
 * </ul>
 */
public interface JavaSemanticModel {

    /**
     * Returns all types in the analysis scope.
     *
     * <p>This includes classes, records, interfaces, enums, and annotation types.
     * Types are filtered by the configured base package.
     *
     * @return stream of all types, ordered by qualified name
     */
    Stream<JavaType> types();
}
