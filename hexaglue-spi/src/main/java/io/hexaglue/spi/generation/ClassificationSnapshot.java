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

package io.hexaglue.spi.generation;

import io.hexaglue.spi.ir.IrSnapshot;

/**
 * Alias for IrSnapshot to clarify usage in generation context.
 *
 * <p><strong>This interface is deprecated since HexaGlue 4.0.</strong>
 * Use {@link io.hexaglue.arch.ArchitecturalModel} via {@code PluginContext.model()} instead.
 *
 * <h2>Migration Guide</h2>
 *
 * <p>Before (v3):
 * <pre>{@code
 * public void generate(GeneratorContext context) {
 *     ClassificationSnapshot snapshot = context.snapshot();
 *     snapshot.domain().aggregateRoots().forEach(this::generateEntity);
 * }
 * }</pre>
 *
 * <p>After (v4):
 * <pre>{@code
 * public class MyPlugin implements GeneratorPlugin {
 *     private ArchitecturalModel model;
 *
 *     @Override
 *     public void execute(PluginContext context) {
 *         this.model = context.model();
 *         super.execute(context);
 *     }
 *
 *     @Override
 *     public void generate(GeneratorContext context) {
 *         model.aggregates().forEach(this::generateEntity);
 *     }
 * }
 * }</pre>
 *
 * @since 3.0.0
 * @deprecated Since 4.0.0. Use {@link io.hexaglue.arch.ArchitecturalModel} via
 *             {@code PluginContext.model()} instead. Scheduled for removal in v5.0.0.
 * @see io.hexaglue.arch.ArchitecturalModel
 */
@Deprecated(forRemoval = true, since = "4.0.0")
public interface ClassificationSnapshot {

    /**
     * Returns the underlying IrSnapshot.
     *
     * <p>This method provides access to the complete analyzed model including
     * domain types, ports, and metadata.
     *
     * @return the IR snapshot
     */
    IrSnapshot snapshot();

    /**
     * Wraps an IrSnapshot as a ClassificationSnapshot.
     *
     * @param snapshot the IR snapshot to wrap
     * @return a ClassificationSnapshot view
     */
    static ClassificationSnapshot of(IrSnapshot snapshot) {
        return () -> snapshot;
    }

    /**
     * Convenience method to access the domain model.
     *
     * @return the domain model from the snapshot
     */
    default io.hexaglue.spi.ir.DomainModel domain() {
        return snapshot().domain();
    }

    /**
     * Convenience method to access the port model.
     *
     * @return the port model from the snapshot
     */
    default io.hexaglue.spi.ir.PortModel ports() {
        return snapshot().ports();
    }

    /**
     * Convenience method to access metadata.
     *
     * @return the metadata from the snapshot
     */
    default io.hexaglue.spi.ir.IrMetadata metadata() {
        return snapshot().metadata();
    }
}
