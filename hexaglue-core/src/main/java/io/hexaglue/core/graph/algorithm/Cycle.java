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

package io.hexaglue.core.graph.algorithm;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a cycle detected in a graph.
 *
 * <p>A cycle is a sequence of edges forming a closed path where the target of
 * each edge is the source of the next edge, and the target of the last edge
 * is the source of the first edge.
 *
 * @param <EDGE> the edge type
 * @param edges the edges forming the cycle
 * @since 3.0.0
 */
public record Cycle<EDGE>(List<EDGE> edges) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException     if edges is null
     * @throws IllegalArgumentException if edges is empty
     */
    public Cycle {
        Objects.requireNonNull(edges, "edges required");
        if (edges.isEmpty()) {
            throw new IllegalArgumentException("cycle must have at least one edge");
        }
        edges = Collections.unmodifiableList(List.copyOf(edges));
    }

    /**
     * Returns the number of edges in the cycle.
     *
     * @return the cycle size
     */
    public int size() {
        return edges.size();
    }

    /**
     * Returns true if this is a self-loop (single edge from node to itself).
     *
     * @return true if size is 1
     */
    public boolean isSelfLoop() {
        return edges.size() == 1;
    }

    /**
     * Returns a string representation showing the cycle path.
     *
     * <p>The format depends on the edge type's toString() implementation.
     *
     * @return formatted cycle path
     */
    public String toPathString() {
        if (edges.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < edges.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(edges.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Cycle[%d edges]", edges.size());
    }
}
