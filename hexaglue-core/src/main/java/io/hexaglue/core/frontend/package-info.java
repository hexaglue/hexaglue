/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Source code abstraction layer (frontend).
 *
 * <p>This package provides a framework-agnostic abstraction over Java source
 * analysis. The current implementation uses Spoon, but the interface design
 * allows for alternative implementations.
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.frontend.JavaSemanticModel} - Entry point, provides types</li>
 *   <li>{@link io.hexaglue.core.frontend.JavaType} - Type abstraction</li>
 *   <li>{@link io.hexaglue.core.frontend.JavaField} - Field abstraction</li>
 *   <li>{@link io.hexaglue.core.frontend.JavaMethod} - Method abstraction</li>
 *   <li>{@link io.hexaglue.core.frontend.TypeRef} - Type reference (possibly generic)</li>
 * </ul>
 *
 * <h2>Spoon Implementation</h2>
 * <p>The {@code spoon} subpackage provides the Spoon-based implementation:
 * <ul>
 *   <li>{@link io.hexaglue.core.frontend.spoon.SpoonFrontend} - Entry point</li>
 *   <li>{@code adapters/*} - Adapters wrapping Spoon types</li>
 * </ul>
 *
 * @see io.hexaglue.core.frontend.JavaSemanticModel Main interface
 * @see io.hexaglue.core.frontend.spoon.SpoonFrontend Spoon implementation
 */
package io.hexaglue.core.frontend;
