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

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Abstract parser interface for AST analysis.
 *
 * <p>This is the main abstraction that allows different parser implementations
 * (Spoon, JDT, JavaParser) to be used interchangeably.</p>
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>SpoonSyntaxProvider - Uses Spoon for AST parsing</li>
 *   <li>JdtSyntaxProvider - Uses Eclipse JDT for AST parsing</li>
 *   <li>JavaParserSyntaxProvider - Uses JavaParser for AST parsing</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SyntaxProvider syntax = new SpoonSyntaxProvider(config);
 *
 * // List all types in scope
 * syntax.types().forEach(type -> {
 *     System.out.println(type.qualifiedName());
 * });
 *
 * // Get a specific type
 * Optional<TypeSyntax> order = syntax.type("com.example.Order");
 * }</pre>
 *
 * @since 4.0.0
 */
public interface SyntaxProvider {

    /**
     * Returns a stream of all types in the analysis scope.
     *
     * <p>Types outside the base package or generated types are excluded.</p>
     *
     * @return a stream of types
     */
    Stream<TypeSyntax> types();

    /**
     * Returns a type by its qualified name, if present.
     *
     * @param qualifiedName the fully qualified name
     * @return an Optional containing the type
     */
    Optional<TypeSyntax> type(String qualifiedName);

    /**
     * Returns metadata about this analysis.
     *
     * @return the analysis metadata
     */
    SyntaxMetadata metadata();

    /**
     * Returns the capabilities of this parser.
     *
     * <p>Plugins can check capabilities before attempting to use
     * parser-specific features.</p>
     *
     * @return the parser capabilities
     */
    SyntaxCapabilities capabilities();
}
