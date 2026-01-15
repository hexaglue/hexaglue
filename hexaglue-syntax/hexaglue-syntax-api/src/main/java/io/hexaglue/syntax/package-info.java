/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Parser abstraction API for HexaGlue.
 *
 * <p>This package defines the abstract interfaces for AST parsing, allowing
 * different parser implementations (Spoon, JDT, JavaParser) to be used
 * interchangeably.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@code SyntaxProvider} - Main entry point for parsing</li>
 *   <li>{@code TypeSyntax} - Representation of a type</li>
 *   <li>{@code MethodSyntax} - Representation of a method</li>
 *   <li>{@code FieldSyntax} - Representation of a field</li>
 *   <li>{@code AnnotationSyntax} - Representation of an annotation</li>
 * </ul>
 *
 * <h2>Design Principle</h2>
 * <p>This module has <strong>ZERO external dependencies</strong>. It only uses
 * {@code java.*} packages. This ensures the API remains purely abstract and
 * doesn't leak parser implementation details.</p>
 *
 * @since 4.0.0
 */
package io.hexaglue.syntax;
