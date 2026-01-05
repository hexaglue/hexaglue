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

import io.hexaglue.core.classification.Evidence;
import io.hexaglue.core.graph.model.NodeId;
import java.util.List;
import java.util.Objects;

/**
 * Result of anchor classification for a single type.
 *
 * @param typeId the node id of the classified type
 * @param kind the anchor classification
 * @param evidence supporting evidence for the classification
 */
public record AnchorResult(NodeId typeId, AnchorKind kind, List<Evidence> evidence) {

    public AnchorResult {
        Objects.requireNonNull(typeId, "typeId cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    /**
     * Creates an INFRA_ANCHOR result.
     */
    public static AnchorResult infraAnchor(NodeId typeId, List<Evidence> evidence) {
        return new AnchorResult(typeId, AnchorKind.INFRA_ANCHOR, evidence);
    }

    /**
     * Creates an INFRA_ANCHOR result with a single evidence.
     */
    public static AnchorResult infraAnchor(NodeId typeId, Evidence evidence) {
        return new AnchorResult(typeId, AnchorKind.INFRA_ANCHOR, List.of(evidence));
    }

    /**
     * Creates a DRIVING_ANCHOR result.
     */
    public static AnchorResult drivingAnchor(NodeId typeId, List<Evidence> evidence) {
        return new AnchorResult(typeId, AnchorKind.DRIVING_ANCHOR, evidence);
    }

    /**
     * Creates a DRIVING_ANCHOR result with a single evidence.
     */
    public static AnchorResult drivingAnchor(NodeId typeId, Evidence evidence) {
        return new AnchorResult(typeId, AnchorKind.DRIVING_ANCHOR, List.of(evidence));
    }

    /**
     * Creates a DOMAIN_ANCHOR result.
     */
    public static AnchorResult domainAnchor(NodeId typeId) {
        return new AnchorResult(typeId, AnchorKind.DOMAIN_ANCHOR, List.of());
    }

    /**
     * Returns true if this is an infrastructure anchor.
     */
    public boolean isInfraAnchor() {
        return kind == AnchorKind.INFRA_ANCHOR;
    }

    /**
     * Returns true if this is a driving anchor.
     */
    public boolean isDrivingAnchor() {
        return kind == AnchorKind.DRIVING_ANCHOR;
    }

    /**
     * Returns true if this is a domain anchor.
     */
    public boolean isDomainAnchor() {
        return kind == AnchorKind.DOMAIN_ANCHOR;
    }
}
