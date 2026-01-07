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

/**
 * HexaGlue Service Provider Interface.
 *
 * <p>This package contains the contracts for HexaGlue plugins:
 * <ul>
 *   <li>{@code io.hexaglue.spi.plugin} - Plugin interface and context</li>
 *   <li>{@code io.hexaglue.spi.ir} - Intermediate Representation types</li>
 * </ul>
 *
 * <h2>Stability Guarantee</h2>
 * <p>This module follows semantic versioning. Plugins should depend ONLY on this module,
 * never on hexaglue-core internals.
 *
 * <h2>Deprecation Policy</h2>
 * <ul>
 *   <li>Deprecated APIs are maintained for at least one major version cycle</li>
 *   <li>Example: An API deprecated in 2.x will be removed no earlier than 3.0.0</li>
 *   <li>Breaking changes are documented in CHANGELOG.md</li>
 *   <li>Migration paths are always provided in {@code @deprecated} Javadoc tags</li>
 *   <li>New APIs are marked with {@code @since M.m.0} tags</li>
 * </ul>
 *
 * <h2>API Conventions</h2>
 * <ul>
 *   <li>All data structures in {@code io.hexaglue.spi.ir} are immutable</li>
 *   <li>Optional values use {@link java.util.Optional} rather than null</li>
 *   <li>Collections are returned as immutable {@link java.util.List}</li>
 *   <li>New interface methods use {@code default} implementations for backward compatibility</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.hexaglue.spi;
