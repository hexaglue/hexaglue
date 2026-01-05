/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * HexaGlue Core Engine - Analysis and classification of Java applications.
 *
 * <p>This is the internal implementation of the HexaGlue analysis engine.
 * External consumers (plugins) should depend only on {@code hexaglue-spi}.
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.engine} - Main engine orchestration</li>
 *   <li>{@link io.hexaglue.core.frontend} - Source code abstraction (Spoon-based)</li>
 *   <li>{@link io.hexaglue.core.graph} - Application graph model</li>
 *   <li>{@link io.hexaglue.core.classification} - Single-pass classification system</li>
 *   <li>{@link io.hexaglue.core.analysis} - Post-classification relation analysis</li>
 *   <li>{@link io.hexaglue.core.ir} - IR export for plugins</li>
 * </ul>
 *
 * <h2>Processing Pipeline</h2>
 * <pre>
 * Source Code → Frontend → Graph → Classification → IR → Plugins
 * </pre>
 *
 * @see io.hexaglue.spi The stable plugin API
 */
package io.hexaglue.core;
