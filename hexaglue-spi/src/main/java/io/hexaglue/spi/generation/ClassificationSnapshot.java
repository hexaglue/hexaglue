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
 * <p>This type alias provides semantic clarity when passing the analyzed
 * application model to generator plugins. It emphasizes that the snapshot
 * contains <b>classification results</b> rather than raw intermediate
 * representation.
 *
 * <p>In HexaGlue 3.0, "ClassificationSnapshot" and "IrSnapshot" are
 * interchangeable terms. This alias exists purely for API clarity and
 * may evolve into a distinct type in future versions.
 *
 * <p>Usage in generator plugins:
 * <pre>{@code
 * public void generate(GeneratorContext context) {
 *     ClassificationSnapshot snapshot = context.snapshot();
 *
 *     // Access classified domain types
 *     snapshot.domain().aggregateRoots().forEach(this::generateEntity);
 *
 *     // Access classified ports
 *     snapshot.ports().ports().forEach(this::generateAdapter);
 * }
 * }</pre>
 *
 * @since 3.0.0
 */
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
