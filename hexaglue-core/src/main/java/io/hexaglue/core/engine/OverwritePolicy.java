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

package io.hexaglue.core.engine;

/**
 * Policy for overwriting existing generated files.
 *
 * <p>Controls how HexaGlue handles previously generated files during
 * subsequent code generation runs. This is particularly important when
 * writing to {@code src/main/java/} where manual edits should be protected.</p>
 *
 * @since 5.0.0
 */
public enum OverwritePolicy {

    /**
     * Overwrite unconditionally (default). For transient {@code target/} output.
     */
    ALWAYS,

    /**
     * Overwrite only if the existing file matches the last known checksum.
     * Protects manual edits when writing to {@code src/main/java/}.
     */
    IF_UNCHANGED,

    /**
     * Never overwrite existing files. For one-time scaffolding.
     */
    NEVER
}
