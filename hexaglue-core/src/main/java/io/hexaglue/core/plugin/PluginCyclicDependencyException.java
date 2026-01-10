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

package io.hexaglue.core.plugin;

/**
 * Thrown when a cyclic dependency is detected among plugins.
 *
 * <p>This occurs when the plugin dependency graph contains cycles, making it
 * impossible to determine a valid execution order.
 */
public class PluginCyclicDependencyException extends RuntimeException {

    /**
     * Creates a new exception with the specified message.
     *
     * @param message the detail message
     */
    public PluginCyclicDependencyException(String message) {
        super(message);
    }
}
