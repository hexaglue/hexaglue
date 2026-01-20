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
 * Sealed interface for application types in the architectural model.
 *
 * <p>Application types orchestrate use cases and coordinate between
 * the domain and infrastructure layers:</p>
 * <ul>
 *   <li>{@link ApplicationService} - Orchestrates use cases using domain and ports</li>
 *   <li>{@link CommandHandler} - Handles commands and modifies state</li>
 *   <li>{@link QueryHandler} - Handles queries without modifying state</li>
 * </ul>
 *
 * <h2>Pattern Matching</h2>
 * <p>Because this is a sealed interface, you can use exhaustive pattern matching:</p>
 * <pre>{@code
 * if (appType instanceof ApplicationService service) {
 *     // handle application service
 * } else if (appType instanceof CommandHandler cmd) {
 *     // handle command handler
 * } else if (appType instanceof QueryHandler query) {
 *     // handle query handler
 * }
 * }</pre>
 *
 * @since 4.1.0
 */
public sealed interface ApplicationType extends ArchType
        permits ApplicationService, CommandHandler, QueryHandler, ApplicationType.Marker {

    /**
     * Temporary marker interface for testing and migration.
     *
     * <p>This allows creating test implementations of ApplicationType.
     * The concrete records (ApplicationService, CommandHandler, QueryHandler) are the primary implementations.</p>
     *
     * @since 4.1.0
     * @deprecated Use concrete record types instead
     */
    @Deprecated(forRemoval = true)
    non-sealed interface Marker extends ApplicationType {}
}
