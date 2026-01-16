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
 * Legacy Intermediate Representation (IR) for analyzed applications.
 *
 * <p><strong>This package is deprecated for removal.</strong>
 *
 * <p>Since HexaGlue v4.0.0, all plugins should use {@link io.hexaglue.arch.ArchitecturalModel}
 * instead of the legacy IR types. The IR was used as an intermediate format between
 * the analysis phase and the generation phase, but this dual-model approach has been
 * superseded by the unified ArchitecturalModel.
 *
 * <h2>Migration Guide</h2>
 * <table>
 *   <tr><th>Legacy IR Type</th><th>Replacement</th></tr>
 *   <tr><td>{@link io.hexaglue.spi.ir.IrSnapshot}</td><td>{@link io.hexaglue.arch.ArchitecturalModel}</td></tr>
 *   <tr><td>{@link io.hexaglue.spi.ir.DomainModel}</td><td>{@code model.query().aggregates()} etc.</td></tr>
 *   <tr><td>{@link io.hexaglue.spi.ir.DomainType}</td><td>{@link io.hexaglue.arch.element.ArchElement} subtypes</td></tr>
 *   <tr><td>{@link io.hexaglue.spi.ir.PortModel}</td><td>{@code model.query().ports()}</td></tr>
 *   <tr><td>{@link io.hexaglue.spi.ir.IrMetadata}</td><td>{@link io.hexaglue.arch.ProjectContext}</td></tr>
 * </table>
 *
 * <h2>Plugin Migration</h2>
 * <pre>{@code
 * // Before (v3.x)
 * IrSnapshot ir = context.ir();
 * String basePackage = ir.metadata().basePackage();
 * ir.domain().aggregateRoots().forEach(type -> ...);
 *
 * // After (v4.x)
 * ArchitecturalModel model = context.model();
 * String basePackage = model.project().basePackage();
 * model.query().aggregates().forEach(agg -> ...);
 * }</pre>
 *
 * @deprecated This entire package is scheduled for removal in v5.0.0.
 *             Use {@link io.hexaglue.arch.ArchitecturalModel} via {@code PluginContext.model()}.
 * @see io.hexaglue.arch.ArchitecturalModel
 * @see io.hexaglue.arch.ProjectContext
 * @since 1.0.0
 */
@Deprecated(forRemoval = true, since = "4.0.0")
package io.hexaglue.spi.ir;
