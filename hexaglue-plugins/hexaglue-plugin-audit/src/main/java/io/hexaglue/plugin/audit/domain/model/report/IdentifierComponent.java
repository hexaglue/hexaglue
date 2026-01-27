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
import java.util.Optional;

/**
 * Details about an identifier component.
 *
 * @param name simple name of the identifier
 * @param packageName fully qualified package name
 * @param wrappedType the type being wrapped (e.g., java.util.UUID)
 * @since 5.0.0
 */
public record IdentifierComponent(
        String name,
        String packageName,
        String wrappedType) {

    /**
     * Creates an identifier component with validation.
     */
    public IdentifierComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
    }

    /**
     * Returns the wrapped type as optional.
     *
     * @return optional wrapped type
     */
    public Optional<String> wrappedTypeOpt() {
        return Optional.ofNullable(wrappedType);
    }
}
