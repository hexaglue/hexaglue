/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * IR export from classified graph to SPI model.
 *
 * <p>The {@link io.hexaglue.core.ir.export.IrExporter} transforms the classified
 * {@link io.hexaglue.core.graph.ApplicationGraph} into an
 * {@link io.hexaglue.spi.ir.IrSnapshot} for plugin consumption.
 *
 * <h2>Export Steps</h2>
 * <ol>
 *   <li>Export domain types (aggregates, entities, value objects, etc.)</li>
 *   <li>Export ports (repositories, use cases, gateways, etc.)</li>
 *   <li>Analyze relations using {@link io.hexaglue.core.analysis.RelationAnalyzer}</li>
 *   <li>Build immutable IR snapshot</li>
 * </ol>
 *
 * @see io.hexaglue.core.ir.export.IrExporter Main exporter class
 */
package io.hexaglue.core.ir.export;
