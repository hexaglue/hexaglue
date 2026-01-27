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

import java.util.List;

/**
 * Information about a detected aggregate.
 *
 * <p>An aggregate is a cluster of domain objects that are treated as a single unit.
 * The aggregate root is the entry point for all modifications to entities within
 * the aggregate boundary.
 *
 * @param rootType the fully-qualified name of the aggregate root
 * @param entities entities within the aggregate boundary
 * @param valueObjects value objects used by the aggregate
 * @since 3.0.0
 * @since 5.0.0 - Migrated from io.hexaglue.spi.audit
 */
public record AggregateInfo(String rootType, List<String> entities, List<String> valueObjects) {

    /**
     * Returns the total number of types in the aggregate.
     */
    public int size() {
        return 1 + entities.size() + valueObjects.size();
    }

    /**
     * Returns true if the aggregate contains the given type.
     */
    public boolean contains(String qualifiedName) {
        return rootType.equals(qualifiedName)
                || entities.contains(qualifiedName)
                || valueObjects.contains(qualifiedName);
    }
}
