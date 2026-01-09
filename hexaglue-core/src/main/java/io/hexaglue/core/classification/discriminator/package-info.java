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
 * Discriminators for deterministic domain type classification.
 *
 * <p>Discriminators are specialized detectors that identify specific domain patterns
 * with high certainty. Each discriminator focuses on one structural pattern:
 * <ul>
 *   <li>{@link io.hexaglue.core.classification.discriminator.IdWrapperDiscriminator} - Detects ID wrapper types</li>
 *   <li>{@link io.hexaglue.core.classification.discriminator.RecordValueObjectDiscriminator} - Detects record-based value objects</li>
 *   <li>{@link io.hexaglue.core.classification.discriminator.RepositoryDiscriminator} - Detects aggregate roots via repositories</li>
 * </ul>
 *
 * <p>Discriminators are used by the {@link io.hexaglue.core.classification.deterministic.DeterministicClassifier}
 * to build deterministic classifications with explicit reasoning and evidence.
 *
 * @since 3.0.0
 */
package io.hexaglue.core.classification.discriminator;
