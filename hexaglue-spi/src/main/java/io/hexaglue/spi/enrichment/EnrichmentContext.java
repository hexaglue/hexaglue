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

package io.hexaglue.spi.enrichment;

import io.hexaglue.spi.audit.ArchitectureQuery;
import io.hexaglue.spi.classification.PrimaryClassificationResult;
import io.hexaglue.spi.plugin.DiagnosticReporter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Context provided to enrichment plugins.
 *
 * <p>The enrichment context provides access to:
 * <ul>
 *   <li>Classification results from the analysis phase</li>
 *   <li>Architecture query API for dependency analysis</li>
 *   <li>Diagnostic reporting for warnings and errors</li>
 * </ul>
 *
 * <p>Note: The graph is intentionally not exposed in the SPI to maintain
 * the abstraction boundary. Enrichment plugins should work with the
 * classification results and architecture query API.
 *
 * @param classifications all primary classification results indexed by type name
 * @param query architecture query interface for graph analysis
 * @param diagnostics diagnostic reporter for errors and warnings
 * @since 3.0.0
 */
public record EnrichmentContext(
        Map<String, PrimaryClassificationResult> classifications,
        ArchitectureQuery query,
        DiagnosticReporter diagnostics) {

    /**
     * Compact constructor with validation.
     *
     * @throws NullPointerException if required parameters are null
     */
    public EnrichmentContext {
        java.util.Objects.requireNonNull(classifications, "classifications cannot be null");
        // query and diagnostics can be null in some contexts
    }

    /**
     * Creates an enrichment context from a list of classifications.
     *
     * @param classificationList list of primary classification results
     * @param query architecture query (may be null)
     * @param diagnostics diagnostic reporter (may be null)
     * @return enrichment context
     */
    public static EnrichmentContext of(
            List<PrimaryClassificationResult> classificationList,
            ArchitectureQuery query,
            DiagnosticReporter diagnostics) {
        Map<String, PrimaryClassificationResult> map =
                classificationList.stream().collect(Collectors.toMap(PrimaryClassificationResult::typeName, c -> c));
        return new EnrichmentContext(map, query, diagnostics);
    }

    /**
     * Gets the classification result for a specific type.
     *
     * @param typeName the fully qualified type name
     * @return the classification result, or null if not found
     */
    public PrimaryClassificationResult getClassification(String typeName) {
        return classifications.get(typeName);
    }

    /**
     * Returns all classifications as a list.
     *
     * @return unmodifiable list of all classifications
     */
    public List<PrimaryClassificationResult> allClassifications() {
        return List.copyOf(classifications.values());
    }
}
