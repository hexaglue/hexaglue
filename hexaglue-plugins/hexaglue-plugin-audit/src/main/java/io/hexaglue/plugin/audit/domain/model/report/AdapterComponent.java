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

package io.hexaglue.plugin.audit.domain.model.report;

import java.util.Objects;

/**
 * Details about an adapter component.
 *
 * @param name simple name of the adapter
 * @param packageName fully qualified package name
 * @param implementsPort name of the port this adapter implements
 * @param type adapter type (DRIVING or DRIVEN)
 * @since 5.0.0
 */
public record AdapterComponent(String name, String packageName, String implementsPort, AdapterType type) {

    /**
     * Creates an adapter component with validation.
     */
    public AdapterComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        Objects.requireNonNull(implementsPort, "implementsPort is required");
        Objects.requireNonNull(type, "type is required");
    }

    /**
     * Type of adapter.
     */
    public enum AdapterType {
        DRIVING,
        DRIVEN
    }
}
