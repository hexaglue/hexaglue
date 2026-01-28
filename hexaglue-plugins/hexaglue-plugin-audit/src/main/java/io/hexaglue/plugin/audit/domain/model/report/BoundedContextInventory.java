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
 * Inventory of elements within a bounded context.
 *
 * @param name name of the bounded context
 * @param aggregates number of aggregate roots
 * @param entities number of entities
 * @param valueObjects number of value objects
 * @param domainEvents number of domain events
 * @since 5.0.0
 */
public record BoundedContextInventory(String name, int aggregates, int entities, int valueObjects, int domainEvents) {

    /**
     * Creates a bounded context inventory with validation.
     */
    public BoundedContextInventory {
        Objects.requireNonNull(name, "name is required");
    }
}
