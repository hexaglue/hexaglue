/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Main engine orchestration.
 *
 * <p>The {@link io.hexaglue.core.engine.DefaultHexaGlueEngine} orchestrates
 * the complete analysis pipeline from source code to plugin execution.
 *
 * <h2>Pipeline Steps</h2>
 * <ol>
 *   <li><b>Frontend</b> - Parse source code via Spoon</li>
 *   <li><b>Graph Build</b> - Construct ApplicationGraph</li>
 *   <li><b>Classification</b> - Classify types and ports</li>
 *   <li><b>IR Export</b> - Convert to stable IR format</li>
 *   <li><b>Plugin Execution</b> - Run all registered plugins</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * HexaGlueEngine engine = new DefaultHexaGlueEngine();
 * engine.analyze(sourceRoot, outputDir);
 * }</pre>
 *
 * @see io.hexaglue.core.engine.DefaultHexaGlueEngine Main engine implementation
 * @see io.hexaglue.spi.engine.HexaGlueEngine SPI engine interface
 */
package io.hexaglue.core.engine;
