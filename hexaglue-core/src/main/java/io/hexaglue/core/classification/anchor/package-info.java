/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Anchor detection - separating infrastructure from domain code.
 *
 * <p>The {@link io.hexaglue.core.classification.anchor.AnchorDetector} classifies
 * types based on their infrastructure dependencies, running BEFORE domain/port
 * classification.
 *
 * <h2>Anchor Kinds</h2>
 * <ul>
 *   <li><b>DRIVING_ANCHOR</b> - Framework entry points (@RestController, @KafkaListener)</li>
 *   <li><b>INFRA_ANCHOR</b> - Infrastructure implementations (@Repository, @Entity, JdbcTemplate)</li>
 *   <li><b>DOMAIN_ANCHOR</b> - Pure domain/application code (no infra dependencies)</li>
 * </ul>
 *
 * <h2>Detection Priority</h2>
 * <ol>
 *   <li>Driving annotations → DRIVING_ANCHOR</li>
 *   <li>Infrastructure annotations → INFRA_ANCHOR</li>
 *   <li>Infrastructure field dependencies → INFRA_ANCHOR</li>
 *   <li>Default → DOMAIN_ANCHOR</li>
 * </ol>
 *
 * @see io.hexaglue.core.classification.anchor.AnchorDetector Detection logic
 * @see io.hexaglue.core.classification.anchor.AnchorKind Classification enum
 * @see io.hexaglue.core.classification.anchor.AnchorContext Detection results
 */
package io.hexaglue.core.classification.anchor;
