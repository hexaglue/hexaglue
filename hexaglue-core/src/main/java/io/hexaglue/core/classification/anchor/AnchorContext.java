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

package io.hexaglue.core.classification.anchor;

import io.hexaglue.core.graph.model.NodeId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Index of anchor classifications for all types in the application graph.
 *
 * <p>Provides fast lookups by type id and by anchor kind.
 */
public final class AnchorContext {

    private final Map<NodeId, AnchorResult> anchors;
    private final Set<NodeId> infraAnchors;
    private final Set<NodeId> drivingAnchors;
    private final Set<NodeId> domainAnchors;

    private AnchorContext(Map<NodeId, AnchorResult> anchors) {
        this.anchors = Map.copyOf(anchors);
        this.infraAnchors = anchors.entrySet().stream()
                .filter(e -> e.getValue().kind() == AnchorKind.INFRA_ANCHOR)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
        this.drivingAnchors = anchors.entrySet().stream()
                .filter(e -> e.getValue().kind() == AnchorKind.DRIVING_ANCHOR)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
        this.domainAnchors = anchors.entrySet().stream()
                .filter(e -> e.getValue().kind() == AnchorKind.DOMAIN_ANCHOR)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates an empty anchor context.
     */
    public static AnchorContext empty() {
        return new AnchorContext(Map.of());
    }

    /**
     * Creates a builder for constructing an anchor context.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the anchor result for the given type, if any.
     */
    public Optional<AnchorResult> get(NodeId typeId) {
        return Optional.ofNullable(anchors.get(typeId));
    }

    /**
     * Returns the anchor kind for the given type.
     *
     * <p>Returns DOMAIN_ANCHOR if the type is not in the context (conservative default).
     */
    public AnchorKind kindOf(NodeId typeId) {
        AnchorResult result = anchors.get(typeId);
        return result != null ? result.kind() : AnchorKind.DOMAIN_ANCHOR;
    }

    /**
     * Returns true if the type is an infrastructure anchor.
     */
    public boolean isInfraAnchor(NodeId typeId) {
        return infraAnchors.contains(typeId);
    }

    /**
     * Returns true if the type is a driving anchor.
     */
    public boolean isDrivingAnchor(NodeId typeId) {
        return drivingAnchors.contains(typeId);
    }

    /**
     * Returns true if the type is a domain anchor.
     */
    public boolean isDomainAnchor(NodeId typeId) {
        return domainAnchors.contains(typeId);
    }

    /**
     * Returns all infrastructure anchor type ids.
     */
    public Set<NodeId> infraAnchors() {
        return infraAnchors;
    }

    /**
     * Returns all driving anchor type ids.
     */
    public Set<NodeId> drivingAnchors() {
        return drivingAnchors;
    }

    /**
     * Returns all domain anchor type ids.
     */
    public Set<NodeId> domainAnchors() {
        return domainAnchors;
    }

    /**
     * Returns all anchor results.
     */
    public Stream<AnchorResult> all() {
        return anchors.values().stream();
    }

    /**
     * Returns the total number of classified types.
     */
    public int size() {
        return anchors.size();
    }

    /**
     * Builder for AnchorContext.
     */
    public static final class Builder {

        private final Map<NodeId, AnchorResult> anchors = new HashMap<>();

        private Builder() {}

        /**
         * Adds an anchor result.
         */
        public Builder put(AnchorResult result) {
            Objects.requireNonNull(result, "result cannot be null");
            anchors.put(result.typeId(), result);
            return this;
        }

        /**
         * Adds all anchor results from another context.
         */
        public Builder putAll(AnchorContext other) {
            Objects.requireNonNull(other, "other cannot be null");
            other.anchors.values().forEach(this::put);
            return this;
        }

        /**
         * Builds the anchor context.
         */
        public AnchorContext build() {
            return new AnchorContext(anchors);
        }
    }
}
