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
 * Policy for handling stale generated files in {@code src/} directories.
 *
 * <p>Stale files are files that were generated in a previous build but not regenerated
 * in the current build. This typically happens when a source type is removed or its
 * classification changes.</p>
 *
 * <p>Files in {@code target/} directories are not affected by this policy since
 * {@code mvn clean} handles their cleanup.</p>
 *
 * @since 5.0.0
 */
public enum StaleFilePolicy {

    /**
     * Log a warning for each stale file but take no action. This is the default.
     */
    WARN,

    /**
     * Delete stale files from the filesystem.
     */
    DELETE,

    /**
     * Fail the build if stale files are detected.
     */
    FAIL
}
