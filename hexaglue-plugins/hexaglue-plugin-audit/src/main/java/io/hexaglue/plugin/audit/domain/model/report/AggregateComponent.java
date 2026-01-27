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

import java.util.List;
import java.util.Objects;

/**
 * Details about an aggregate root component.
 *
 * @param name simple name of the aggregate
 * @param packageName fully qualified package name
 * @param fields number of fields
 * @param references other aggregates this one references
 * @param usesPorts ports used by this aggregate
 * @since 5.0.0
 */
public record AggregateComponent(
        String name,
        String packageName,
        int fields,
        List<String> references,
        List<String> usesPorts) {

    /**
     * Creates an aggregate component with validation.
     */
    public AggregateComponent {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(packageName, "packageName is required");
        references = references != null ? List.copyOf(references) : List.of();
        usesPorts = usesPorts != null ? List.copyOf(usesPorts) : List.of();
    }
}
