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
 * Extension of HexaGlue SPI with support for the v4 {@link io.hexaglue.arch.ArchitecturalModel}.
 *
 * <p>This package provides:
 * <ul>
 *   <li>{@link io.hexaglue.spi.arch.ArchModelPluginContext} - Extended plugin context
 *       with access to the unified architectural model</li>
 *   <li>{@link io.hexaglue.spi.arch.PluginContexts} - Utility methods for working
 *       with plugin contexts and detecting v4 support</li>
 * </ul>
 *
 * <h2>Migration Guide</h2>
 *
 * <p>To migrate a plugin from the legacy {@code IrSnapshot} API to the v4
 * {@code ArchitecturalModel} API:</p>
 *
 * <ol>
 *   <li>Add dependency on {@code hexaglue-spi-arch} instead of just {@code hexaglue-spi}</li>
 *   <li>Check for v4 support using {@link io.hexaglue.spi.arch.PluginContexts#hasArchModel}</li>
 *   <li>Use the new API when available, fall back to legacy otherwise</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public void execute(PluginContext context) {
 *     PluginContexts.getModel(context).ifPresentOrElse(
 *         model -> {
 *             // Use v4 API
 *             model.domainEntities().forEach(entity -> ...);
 *         },
 *         () -> {
 *             // Fall back to legacy API
 *             context.ir().domain().types().forEach(type -> ...);
 *         }
 *     );
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
package io.hexaglue.spi.arch;
