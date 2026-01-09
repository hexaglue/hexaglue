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
 * Classification of architectural roles.
 *
 * <p>This enum represents the role a code unit plays in the architecture,
 * based on DDD tactical patterns and hexagonal architecture concepts.
 *
 * @since 3.0.0
 */
public enum RoleClassification {

    /** An aggregate root in DDD. */
    AGGREGATE_ROOT,

    /** An entity within an aggregate. */
    ENTITY,

    /** An immutable value object. */
    VALUE_OBJECT,

    /** A domain service. */
    SERVICE,

    /** A repository interface or implementation. */
    REPOSITORY,

    /** A factory for creating domain objects. */
    FACTORY,

    /** A port (interface) in hexagonal architecture. */
    PORT,

    /** An adapter implementing a port. */
    ADAPTER,

    /** An application service or use case. */
    USE_CASE,

    /** Role could not be determined. */
    UNKNOWN
}
