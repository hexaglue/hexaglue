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

package io.hexaglue.arch.model.index;

/**
 * Architectural role of a module in a multi-module project.
 *
 * <p>Each module in a multi-module Maven project has an architectural role
 * that determines how HexaGlue treats it during analysis and code generation.
 * For example, generated JPA entities are routed to the {@link #INFRASTRUCTURE}
 * module while domain types live in the {@link #DOMAIN} module.</p>
 *
 * @since 5.0.0
 */
public enum ModuleRole {

    /**
     * Contains the domain model (aggregates, entities, value objects, ports).
     */
    DOMAIN,

    /**
     * Contains infrastructure adapters (JPA, REST, messaging).
     */
    INFRASTRUCTURE,

    /**
     * Contains application services and use-case orchestration.
     */
    APPLICATION,

    /**
     * Contains API contracts (DTOs, OpenAPI specs).
     */
    API,

    /**
     * Contains the assembly/bootstrap module (Spring Boot main class, configuration).
     */
    ASSEMBLY,

    /**
     * Contains shared utilities or cross-cutting concerns.
     */
    SHARED
}
