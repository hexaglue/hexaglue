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

package io.hexaglue.arch.model.audit;

/**
 * The detected architectural style of the codebase.
 *
 * <p>HexaGlue attempts to detect the dominant architectural style based on
 * package structure, naming conventions, and dependency patterns.
 *
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public enum DetectedArchitectureStyle {

    /** Hexagonal Architecture (Ports and Adapters). */
    HEXAGONAL,

    /** Traditional layered architecture. */
    LAYERED,

    /** Clean Architecture. */
    CLEAN,

    /** Onion Architecture. */
    ONION,

    /** Architecture style could not be determined. */
    UNKNOWN
}
