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
 * Inventory of all architectural elements grouped by bounded context.
 *
 * @param boundedContexts inventory per bounded context
 * @param totals total counts across all contexts
 * @since 5.0.0
 */
public record Inventory(List<BoundedContextInventory> boundedContexts, InventoryTotals totals) {

    /**
     * Creates an inventory with validation.
     */
    public Inventory {
        boundedContexts = boundedContexts != null ? List.copyOf(boundedContexts) : List.of();
        Objects.requireNonNull(totals, "totals is required");
    }
}
