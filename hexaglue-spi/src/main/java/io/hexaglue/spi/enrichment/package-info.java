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
 * Enrichment plugin API for adding semantic labels and properties.
 *
 * <p>The enrichment phase runs between classification and code generation,
 * augmenting the classification results with additional semantic information.
 *
 * <p>Key interfaces:
 * <ul>
 *   <li>{@link io.hexaglue.spi.enrichment.EnrichmentPlugin} - Plugin interface</li>
 *   <li>{@link io.hexaglue.spi.enrichment.EnrichmentContext} - Execution context</li>
 *   <li>{@link io.hexaglue.spi.enrichment.SemanticLabel} - Predefined labels</li>
 *   <li>{@link io.hexaglue.spi.enrichment.EnrichmentContribution} - Plugin output</li>
 * </ul>
 *
 * @since 3.0.0
 */
package io.hexaglue.spi.enrichment;
