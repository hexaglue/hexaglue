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

package io.hexaglue.syntax;

/**
 * Exception thrown when a required capability is not supported by the parser.
 *
 * @since 4.0.0
 */
public class UnsupportedCapabilityException extends RuntimeException {

    private final SyntaxCapabilities.Capability capability;
    private final String context;

    /**
     * Creates a new exception.
     *
     * @param capability the required capability
     * @param context the context requiring this capability
     */
    public UnsupportedCapabilityException(SyntaxCapabilities.Capability capability, String context) {
        super("Parser does not support " + capability + " required for: " + context);
        this.capability = capability;
        this.context = context;
    }

    /**
     * Returns the required capability.
     *
     * @return the capability
     */
    public SyntaxCapabilities.Capability getCapability() {
        return capability;
    }

    /**
     * Returns the context that required this capability.
     *
     * @return the context description
     */
    public String getContext() {
        return context;
    }
}
