/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Port classification criteria implementations.
 *
 * <p>Each criteria evaluates an interface against specific conditions and returns
 * a {@link io.hexaglue.core.classification.MatchResult} with confidence level,
 * target kind, and direction.
 *
 * <h2>Criteria Categories</h2>
 * <ul>
 *   <li><b>Explicit</b> - jMolecules annotations (@PrimaryPort, @SecondaryPort, @Repository)</li>
 *   <li><b>Semantic</b> - Based on CoreAppClass relationships (implemented/depended)</li>
 *   <li><b>CQRS</b> - Command/Query pattern detection</li>
 *   <li><b>Package</b> - Package patterns (ports.in, ports.out)</li>
 *   <li><b>Naming</b> - Name patterns (*Repository, *UseCase, *Gateway)</li>
 * </ul>
 *
 * @see io.hexaglue.core.classification.port.PortClassificationCriteria Base interface
 */
package io.hexaglue.core.classification.port.criteria;
