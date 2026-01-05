/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Port interface classification - DRIVING vs DRIVEN, REPOSITORY, USE_CASE, etc.
 *
 * <p>The {@link io.hexaglue.core.classification.port.PortClassifier} evaluates
 * criteria to classify interfaces as ports with direction and kind.
 *
 * <h2>Port Direction</h2>
 * <ul>
 *   <li><b>DRIVING</b> - Inbound ports (implemented by application, called by external)</li>
 *   <li><b>DRIVEN</b> - Outbound ports (consumed by application, implemented by infrastructure)</li>
 * </ul>
 *
 * <h2>Port Kinds</h2>
 * <ul>
 *   <li><b>REPOSITORY</b> - Data access (DRIVEN)</li>
 *   <li><b>GATEWAY</b> - External system access (DRIVEN)</li>
 *   <li><b>USE_CASE</b> - Application use case (DRIVING)</li>
 *   <li><b>COMMAND</b> - CQRS write operation (DRIVING)</li>
 *   <li><b>QUERY</b> - CQRS read operation (DRIVING)</li>
 * </ul>
 *
 * @see io.hexaglue.core.classification.port.PortClassifier Main classifier
 * @see io.hexaglue.core.classification.port.PortDirection Port direction
 * @see io.hexaglue.core.classification.port.PortKind Port kind
 */
package io.hexaglue.core.classification.port;
