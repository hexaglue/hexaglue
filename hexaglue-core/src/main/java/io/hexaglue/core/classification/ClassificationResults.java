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

package io.hexaglue.core.classification;

import io.hexaglue.core.graph.model.NodeId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Holds all classification results from a classification run.
 *
 * <p>This is the output of {@link TwoPassClassifier} and contains
 * both domain type and port classifications.
 *
 * @param allClassifications all classification results by node ID
 */
public record ClassificationResults(Map<NodeId, ClassificationResult> allClassifications) {

    /**
     * Returns the classification for a specific node.
     */
    public Optional<ClassificationResult> get(NodeId nodeId) {
        return Optional.ofNullable(allClassifications.get(nodeId));
    }

    /**
     * Returns all domain type classifications (non-port).
     */
    public List<ClassificationResult> domainClassifications() {
        return allClassifications.values().stream()
                .filter(c -> c.target() == ClassificationTarget.DOMAIN)
                .toList();
    }

    /**
     * Returns all port classifications.
     */
    public List<ClassificationResult> portClassifications() {
        return allClassifications.values().stream()
                .filter(c -> c.target() == ClassificationTarget.PORT)
                .toList();
    }

    /**
     * Returns all classified results (both domain and port).
     */
    public List<ClassificationResult> classifiedResults() {
        return allClassifications.values().stream()
                .filter(ClassificationResult::isClassified)
                .toList();
    }

    /**
     * Returns all conflict results.
     */
    public List<ClassificationResult> conflicts() {
        return allClassifications.values().stream()
                .filter(c -> c.status() == ClassificationStatus.CONFLICT)
                .toList();
    }

    /**
     * Returns all results as a stream.
     */
    public Stream<ClassificationResult> stream() {
        return allClassifications.values().stream();
    }

    /**
     * Returns the total number of classifications.
     */
    public int size() {
        return allClassifications.size();
    }

    /**
     * Converts to a list of all classification results.
     */
    public List<ClassificationResult> toList() {
        return new ArrayList<>(allClassifications.values());
    }
}
