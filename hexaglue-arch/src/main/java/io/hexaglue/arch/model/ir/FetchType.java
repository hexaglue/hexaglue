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

package io.hexaglue.arch.model.ir;

/**
 * JPA fetch strategy for relationship mapping.
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.ir
 */
public enum FetchType {

    /**
     * Lazy fetching - data is loaded only when accessed.
     * Recommended for collections and optional associations.
     */
    LAZY,

    /**
     * Eager fetching - data is loaded immediately with the parent entity.
     * Use sparingly to avoid N+1 queries.
     */
    EAGER
}
