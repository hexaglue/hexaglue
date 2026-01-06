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
 * Generic classification engine for HexaGlue.
 *
 * <p>This package provides a reusable engine for evaluating classification
 * criteria and making decisions. It is used by both domain and port
 * classification subsystems.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.classification.engine.CriteriaEngine} - The main engine</li>
 *   <li>{@link io.hexaglue.core.classification.engine.Contribution} - Result of criteria evaluation</li>
 *   <li>{@link io.hexaglue.core.classification.engine.DecisionPolicy} - Policy for selecting winners</li>
 *   <li>{@link io.hexaglue.core.classification.engine.DefaultDecisionPolicy} - Standard tie-breaking</li>
 *   <li>{@link io.hexaglue.core.classification.engine.CompatibilityPolicy} - Kind compatibility</li>
 *   <li>{@link io.hexaglue.core.classification.engine.CriteriaProfile} - Priority configuration</li>
 * </ul>
 *
 * <h2>Tie-Breaking Algorithm</h2>
 * <p>The {@link io.hexaglue.core.classification.engine.DefaultDecisionPolicy}
 * implements the standard tie-breaking order:
 * <ol>
 *   <li>Priority (descending) - higher priority wins</li>
 *   <li>Confidence weight (descending) - higher confidence wins</li>
 *   <li>Criteria name (ascending) - alphabetical for determinism</li>
 * </ol>
 *
 * @since 2.0.0
 */
package io.hexaglue.core.classification.engine;
