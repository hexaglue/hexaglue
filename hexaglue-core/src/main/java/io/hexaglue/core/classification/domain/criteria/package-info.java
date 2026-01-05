/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Domain classification criteria implementations.
 *
 * <p>Each criteria evaluates a type against specific conditions and returns
 * a {@link io.hexaglue.core.classification.MatchResult} with confidence level.
 *
 * <h2>Criteria Categories</h2>
 * <ul>
 *   <li><b>Explicit</b> - jMolecules annotations (@AggregateRoot, @Entity, etc.)</li>
 *   <li><b>Interface</b> - jMolecules interface implementations</li>
 *   <li><b>Structural</b> - Identity fields, immutability, relationships</li>
 *   <li><b>Naming</b> - Name patterns (*Event, *Id, *Service)</li>
 * </ul>
 *
 * <h2>Tie-Breaking</h2>
 * When multiple criteria match:
 * <ol>
 *   <li>Priority (descending) - higher wins</li>
 *   <li>Confidence (descending) - higher wins</li>
 *   <li>Name (ascending) - alphabetical for determinism</li>
 * </ol>
 *
 * @see io.hexaglue.core.classification.ClassificationCriteria Base interface
 */
package io.hexaglue.core.classification.domain.criteria;
