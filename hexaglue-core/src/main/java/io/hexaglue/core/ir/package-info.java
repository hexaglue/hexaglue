/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Intermediate Representation (IR) for plugin consumption.
 *
 * <p>This package converts the classified graph into the stable IR format
 * defined in {@code hexaglue-spi}. The IR is the contract between the
 * core engine and plugins.
 *
 * <h2>IR Structure</h2>
 * <ul>
 *   <li>{@code IrSnapshot} - Complete immutable snapshot</li>
 *   <li>{@code DomainModel} - Classified domain types</li>
 *   <li>{@code PortModel} - Classified port interfaces</li>
 *   <li>{@code IrMetadata} - Build metadata</li>
 * </ul>
 *
 * <h2>Export Process</h2>
 * <pre>
 * ApplicationGraph + ClassificationResults → IrExporter → IrSnapshot
 * </pre>
 *
 * @see io.hexaglue.core.ir.export.IrExporter Export logic
 * @see io.hexaglue.spi.ir.IrSnapshot SPI IR model
 */
package io.hexaglue.core.ir;
