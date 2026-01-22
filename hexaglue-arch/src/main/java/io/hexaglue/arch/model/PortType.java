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

package io.hexaglue.arch.model;

/**
 * Sealed interface for port types in the architectural model.
 *
 * <p>Ports define the boundaries of the application in hexagonal architecture.
 * They are interfaces that specify how the application interacts with
 * the outside world:</p>
 * <ul>
 *   <li>{@link DrivingPort} (primary/inbound) - Interface exposed to the outside world</li>
 *   <li>{@link DrivenPort} (secondary/outbound) - Interface for external dependencies</li>
 * </ul>
 *
 * <h2>Pattern Matching</h2>
 * <p>Because this is a sealed interface, you can use exhaustive pattern matching:</p>
 * <pre>{@code
 * if (portType instanceof DrivingPort driving) {
 *     // handle driving port
 * } else if (portType instanceof DrivenPort driven) {
 *     // handle driven port
 * }
 * }</pre>
 *
 * @since 4.1.0
 */
public sealed interface PortType extends ArchType permits DrivingPort, DrivenPort {}
