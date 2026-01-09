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
 * @param classification the classification results
 * @param query architecture query interface for graph analysis
 * @param diagnostics diagnostic reporter for errors and warnings
 * @since 3.0.0
 */
public record EnrichmentContext(
        PrimaryClassificationResult classification, ArchitectureQuery query, DiagnosticReporter diagnostics) {}
