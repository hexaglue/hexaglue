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

package io.hexaglue.spi.audit;

/**
 * Classification of architectural layers.
 *
 * <p>This enum represents the architectural layer a code unit belongs to
 * in hexagonal or layered architectures. Layer classification is used to
 * enforce architectural constraints and detect dependency violations.
 *
 * @since 3.0.0
 */
public enum LayerClassification {

    /**
     * Domain layer (core business logic).
     * Contains entities, value objects, domain services, and business rules.
     */
    DOMAIN,

    /**
     * Application layer (use cases and orchestration).
     * Contains application services, command handlers, query handlers.
     */
    APPLICATION,

    /**
     * Infrastructure layer (technical implementations).
     * Contains persistence, messaging, external API adapters.
     */
    INFRASTRUCTURE,

    /**
     * Presentation layer (user interfaces and APIs).
     * Contains REST controllers, GraphQL resolvers, UI components.
     */
    PRESENTATION,

    /**
     * Layer could not be determined.
     */
    UNKNOWN
}
