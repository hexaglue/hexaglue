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

package io.hexaglue.plugin.livingdoc.content;

import io.hexaglue.arch.model.TypeId;
import io.hexaglue.arch.model.graph.RelationshipGraph;
import io.hexaglue.arch.model.index.CompositionIndex;
import io.hexaglue.plugin.livingdoc.model.ArchitecturalDependency;
import io.hexaglue.plugin.livingdoc.model.ArchitecturalDependency.Direction;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Enriches domain documentation with incoming and outgoing architectural dependencies.
 *
 * <p>Extracts relationships from the {@link RelationshipGraph} via
 * {@link CompositionIndex}. Degrades gracefully when the composition index
 * is not available (returns empty lists).
 *
 * @since 5.0.0
 */
public final class RelationshipEnricher {

    private final Optional<RelationshipGraph> graph;

    /**
     * Creates an enricher from an optional composition index.
     *
     * <p>If the composition index is absent, all query methods return empty lists.
     *
     * @param compositionIndex the optional composition index
     * @since 5.0.0
     */
    public RelationshipEnricher(Optional<CompositionIndex> compositionIndex) {
        Objects.requireNonNull(compositionIndex, "compositionIndex must not be null");
        this.graph = compositionIndex.map(CompositionIndex::graph);
    }

    /**
     * Returns whether relationship data is available.
     *
     * @return {@code true} if a relationship graph is available
     * @since 5.0.0
     */
    public boolean isAvailable() {
        return graph.isPresent();
    }

    /**
     * Returns outgoing dependencies from the given type.
     *
     * @param typeId the source type
     * @return a list of outgoing dependencies, or empty if graph is unavailable
     * @since 5.0.0
     */
    public List<ArchitecturalDependency> outgoingFrom(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        if (graph.isEmpty()) {
            return List.of();
        }
        return graph.get()
                .from(typeId)
                .map(rel -> new ArchitecturalDependency(
                        rel.target().simpleName(), rel.target().qualifiedName(), rel.type(), Direction.OUTGOING))
                .toList();
    }

    /**
     * Returns incoming dependencies to the given type.
     *
     * @param typeId the target type
     * @return a list of incoming dependencies, or empty if graph is unavailable
     * @since 5.0.0
     */
    public List<ArchitecturalDependency> incomingTo(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        if (graph.isEmpty()) {
            return List.of();
        }
        return graph.get()
                .to(typeId)
                .map(rel -> new ArchitecturalDependency(
                        rel.source().simpleName(), rel.source().qualifiedName(), rel.type(), Direction.INCOMING))
                .toList();
    }
}
